package com.example.notes.note.service;

import java.time.Clock;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.notes.audit.AuditService;
import com.example.notes.common.DataAccessDeniedException;
import com.example.notes.common.NoteConflictException;
import com.example.notes.common.NoteNotFoundException;
import com.example.notes.note.Note;
import com.example.notes.note.NoteRepository;
import com.example.notes.note.NoteStatus;
import com.example.notes.note.api.CreateNoteRequest;
import com.example.notes.note.api.NoteEvent;
import com.example.notes.note.api.NoteResponse;
import com.example.notes.note.api.UpdateNoteRequest;
import com.example.notes.note.sorting.NoteSort;
import com.example.notes.note.sorting.NoteSortStrategyRegistry;
import com.example.notes.note.validation.NoteRuleChain;
import com.example.notes.security.RequestActor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class NoteService {
    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final NoteRepository repository;
    private final NoteFactory factory;
    private final NoteRuleChain ruleChain;
    private final NoteSortStrategyRegistry sortStrategies;
    private final Clock clock;
    private final AuditService auditService;
    private final Sinks.Many<NoteEvent> events = Sinks.many().multicast().onBackpressureBuffer();

    public NoteService(
            NoteRepository repository,
            NoteFactory factory,
            NoteRuleChain ruleChain,
            NoteSortStrategyRegistry sortStrategies,
            Clock clock,
            AuditService auditService) {
        this.repository = repository;
        this.factory = factory;
        this.ruleChain = ruleChain;
        this.sortStrategies = sortStrategies;
        this.clock = clock;
        this.auditService = auditService;
    }

    public Flux<NoteResponse> list(
            String teamId,
            String query,
            NoteStatus status,
            NoteSort sort,
            RequestActor actor) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        log.debug(
                "list notes teamId={} actorId={} clearance={} status={} sort={}",
                teamId, actor.userId(), actor.clearance(), status, sort);
        return repository.findAllByTeamId(teamId)
                .filter(note -> actor.mayAccess(note.classification()))
                .filter(note -> status == null || note.status() == status)
                .filter(note -> normalizedQuery.isEmpty()
                        || note.title().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || note.content().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sort(sortStrategies.get(sort).comparator())
                .map(NoteResponse::from);
    }

    public Mono<NoteResponse> get(String teamId, Long id, RequestActor actor) {
        return findAccessible(teamId, id, actor).map(NoteResponse::from);
    }

    @Transactional
    public Mono<NoteResponse> create(String teamId, CreateNoteRequest request, RequestActor actor) {
        requireClearance(actor, request.classification());
        NoteDraft draft = new NoteDraft(
                null,
                teamId,
                actor.userId(),
                request.title(),
                request.content(),
                request.classification());
        return ruleChain.validate(draft)
                .then(Mono.fromSupplier(() -> factory.create(draft)))
                .flatMap(repository::save)
                .flatMap(saved -> auditService.record("NOTE_CREATED", actor, saved).thenReturn(saved))
                .map(saved -> publish("created", saved))
                .doOnNext(saved -> log.info(
                        "note created id={} teamId={} actorId={} classification={}",
                        saved.id(), teamId, actor.userId(), request.classification()));
    }

    @Transactional
    public Mono<NoteResponse> update(
            String teamId,
            Long id,
            UpdateNoteRequest request,
            RequestActor actor) {
        return findAccessible(teamId, id, actor)
                .flatMap(existing -> {
                    if (!existing.version().equals(request.version())) {
                        log.warn(
                                "optimistic lock conflict noteId={} teamId={} actorId={} "
                                        + "suppliedVersion={} currentVersion={}",
                                id, teamId, actor.userId(), request.version(), existing.version());
                        return Mono.error(new NoteConflictException("The note was changed by someone else"));
                    }
                    NoteDraft draft = new NoteDraft(
                            existing.id(),
                            teamId,
                            existing.authorId(),
                            request.title(),
                            request.content(),
                            existing.classification());
                    return ruleChain.validate(draft)
                            .then(repository.save(existing.update(
                                    request.title().trim(),
                                    request.content().trim(),
                                    clock.instant())));
                })
                .onErrorMap(
                        OptimisticLockingFailureException.class,
                        error -> new NoteConflictException("The note was changed by someone else"))
                .flatMap(saved -> auditService.record("NOTE_UPDATED", actor, saved).thenReturn(saved))
                .map(saved -> publish("updated", saved))
                .doOnNext(saved -> log.info(
                        "note updated id={} teamId={} actorId={} newVersion={}",
                        saved.id(), teamId, actor.userId(), saved.version()));
    }

    @Transactional
    public Mono<NoteResponse> setArchived(
            String teamId,
            Long id,
            boolean archived,
            RequestActor actor) {
        NoteStatus target = archived ? NoteStatus.ARCHIVED : NoteStatus.ACTIVE;
        return findAccessible(teamId, id, actor)
                .flatMap(note -> repository.save(note.withStatus(target, clock.instant())))
                .flatMap(saved -> auditService
                        .record(archived ? "NOTE_ARCHIVED" : "NOTE_RESTORED", actor, saved)
                        .thenReturn(saved))
                .map(saved -> publish(archived ? "archived" : "restored", saved))
                .doOnNext(saved -> log.info(
                        "note {} id={} teamId={} actorId={}",
                        archived ? "archived" : "restored", saved.id(), teamId, actor.userId()));
    }

    @Transactional
    public Mono<Void> delete(String teamId, Long id, RequestActor actor) {
        return findAccessible(teamId, id, actor)
                .flatMap(note -> auditService.record("NOTE_DELETED", actor, note)
                        .then(repository.delete(note))
                        .doOnSuccess(ignored -> {
                            events.tryEmitNext(new NoteEvent("deleted", teamId, note.classification(), null));
                            log.info("note deleted id={} teamId={} actorId={}", id, teamId, actor.userId());
                        }));
    }

    public Flux<NoteEvent> events(String teamId, RequestActor actor) {
        return events.asFlux()
                .filter(event -> event.teamId().equals(teamId))
                .filter(event -> actor.mayAccess(event.classification()))
                .doOnSubscribe(subscription -> log.info(
                        "event stream subscribed teamId={} actorId={} clearance={}",
                        teamId, actor.userId(), actor.clearance()))
                .doOnCancel(() -> log.info(
                        "event stream closed teamId={} actorId={}", teamId, actor.userId()));
    }

    private Mono<Note> find(String teamId, Long id) {
        return repository.findByIdAndTeamId(id, teamId)
                .switchIfEmpty(Mono.error(new NoteNotFoundException(id)));
    }

    private Mono<Note> findAccessible(String teamId, Long id, RequestActor actor) {
        return find(teamId, id)
                .filter(note -> actor.mayAccess(note.classification()))
                .switchIfEmpty(Mono.error(new NoteNotFoundException(id)));
    }

    private void requireClearance(RequestActor actor, com.example.notes.security.DataClassification classification) {
        if (!actor.mayAccess(classification)) {
            log.warn(
                    "access denied actorId={} clearance={} requestedClassification={}",
                    actor.userId(), actor.clearance(), classification);
            throw new DataAccessDeniedException("Clearance does not permit this data classification");
        }
    }

    private NoteResponse publish(String type, Note note) {
        NoteResponse response = NoteResponse.from(note);
        events.tryEmitNext(new NoteEvent(type, note.teamId(), note.classification(), response));
        return response;
    }
}

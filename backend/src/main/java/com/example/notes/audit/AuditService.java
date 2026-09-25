package com.example.notes.audit;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.notes.note.Note;
import com.example.notes.security.RequestActor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRecordRepository repository;
    private final Clock clock;

    public AuditService(AuditRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Mono<Void> record(String action, RequestActor actor, Note note) {
        AuditRecord record = new AuditRecord(
                null,
                note.teamId(),
                actor.userId(),
                action,
                note.id(),
                note.classification(),
                clock.instant());
        return repository.save(record)
                .doOnNext(saved -> log.debug(
                        "audit recorded action={} noteId={} teamId={} actorId={} classification={}",
                        action, note.id(), note.teamId(), actor.userId(), note.classification()))
                .then();
    }

    public Flux<AuditRecord> list(String teamId) {
        return repository.findAllByTeamIdOrderByOccurredAtDesc(teamId);
    }
}

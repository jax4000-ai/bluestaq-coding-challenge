package com.example.notes.note.api;

import java.net.URI;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.notes.note.NoteStatus;
import com.example.notes.note.service.NoteService;
import com.example.notes.note.sorting.NoteSort;
import com.example.notes.security.DataClassification;
import com.example.notes.security.RequestActor;
import com.example.notes.security.RequestActorResolver;
import com.example.notes.security.ValidTeamId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/teams/{teamId}/notes")
public class NoteController {
    private static final Logger log = LoggerFactory.getLogger(NoteController.class);

    private final NoteService service;
    private final RequestActorResolver actorResolver;

    public NoteController(NoteService service, RequestActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public Flux<NoteResponse> list(
            @PathVariable @ValidTeamId String teamId,
            @RequestParam(defaultValue = "") @Size(max = 200) String query,
            @RequestParam(required = false) NoteStatus status,
            @RequestParam(defaultValue = "UPDATED_DESC") NoteSort sort,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Clearance") DataClassification clearance) {
        return service.list(teamId, query, status, sort, actor(userId, clearance));
    }

    @GetMapping("/{id}")
    public Mono<NoteResponse> get(
            @PathVariable @ValidTeamId String teamId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Clearance") DataClassification clearance) {
        return service.get(teamId, id, actor(userId, clearance));
    }

    @PostMapping
    public Mono<ResponseEntity<NoteResponse>> create(
            @PathVariable @ValidTeamId String teamId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Clearance") DataClassification clearance,
            @Valid @RequestBody CreateNoteRequest request) {
        log.debug("POST /notes teamId={} actorId={}", teamId, userId);
        return service.create(teamId, request, actor(userId, clearance))
                .map(note -> ResponseEntity
                        .created(URI.create("/api/teams/" + teamId + "/notes/" + note.id()))
                        .body(note));
    }

    @PutMapping("/{id}")
    public Mono<NoteResponse> update(
            @PathVariable @ValidTeamId String teamId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Clearance") DataClassification clearance,
            @Valid @RequestBody UpdateNoteRequest request) {
        log.debug("PUT /notes/{} teamId={} actorId={}", id, teamId, userId);
        return service.update(teamId, id, request, actor(userId, clearance));
    }

    @PatchMapping("/{id}/archive")
    public Mono<NoteResponse> archive(
            @PathVariable @ValidTeamId String teamId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Clearance") DataClassification clearance,
            @RequestParam(defaultValue = "true") boolean archived) {
        log.debug("PATCH /notes/{}/archive archived={} teamId={} actorId={}", id, archived, teamId, userId);
        return service.setArchived(teamId, id, archived, actor(userId, clearance));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(
            @PathVariable @ValidTeamId String teamId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Clearance") DataClassification clearance) {
        log.debug("DELETE /notes/{} teamId={} actorId={}", id, teamId, userId);
        return service.delete(teamId, id, actor(userId, clearance))
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NoteEvent>> events(
            @PathVariable @ValidTeamId String teamId,
            @RequestParam String userId,
            @RequestParam DataClassification clearance) {
        Flux<ServerSentEvent<NoteEvent>> updates = service.events(teamId, actor(userId, clearance))
                .map(event -> ServerSentEvent.builder(event).event(event.type()).build());
        Flux<ServerSentEvent<NoteEvent>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(sequence -> ServerSentEvent.<NoteEvent>builder().comment("keep-alive").build());
        return Flux.merge(updates, heartbeat);
    }

    private RequestActor actor(String userId, DataClassification clearance) {
        return actorResolver.resolve(userId, clearance);
    }
}

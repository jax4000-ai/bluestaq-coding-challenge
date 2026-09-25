package com.example.notes.note;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NoteRepository extends ReactiveCrudRepository<Note, Long> {
    Flux<Note> findAllByTeamId(String teamId);

    Mono<Note> findByIdAndTeamId(Long id, String teamId);

    Mono<Boolean> existsByTeamIdAndTitleIgnoreCase(String teamId, String title);

    Mono<Boolean> existsByTeamIdAndTitleIgnoreCaseAndIdNot(String teamId, String title, Long id);
}

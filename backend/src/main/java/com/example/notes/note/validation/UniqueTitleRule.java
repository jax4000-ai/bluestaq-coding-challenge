package com.example.notes.note.validation;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.notes.common.InvalidNoteException;
import com.example.notes.note.NoteRepository;
import com.example.notes.note.service.NoteDraft;

import reactor.core.publisher.Mono;

@Component
@Order(20)
public class UniqueTitleRule implements NoteRule {
    private final NoteRepository repository;

    public UniqueTitleRule(NoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Void> validate(NoteDraft draft) {
        Mono<Boolean> exists = draft.noteId() == null
                ? repository.existsByTeamIdAndTitleIgnoreCase(draft.teamId(), draft.title().trim())
                : repository.existsByTeamIdAndTitleIgnoreCaseAndIdNot(
                        draft.teamId(), draft.title().trim(), draft.noteId());
        return exists
                .flatMap(found -> found
                        ? Mono.error(new InvalidNoteException("A note with this title already exists in the team"))
                        : Mono.empty());
    }
}

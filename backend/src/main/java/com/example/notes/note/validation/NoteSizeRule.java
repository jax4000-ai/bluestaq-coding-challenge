package com.example.notes.note.validation;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.notes.common.InvalidNoteException;
import com.example.notes.note.service.NoteDraft;

import reactor.core.publisher.Mono;

@Component
@Order(10)
public class NoteSizeRule implements NoteRule {
    @Override
    public Mono<Void> validate(NoteDraft draft) {
        if (draft.title().trim().length() > 120) {
            return Mono.error(new InvalidNoteException("Title must not exceed 120 characters"));
        }
        if (draft.content().trim().length() > 10_000) {
            return Mono.error(new InvalidNoteException("Content must not exceed 10,000 characters"));
        }
        return Mono.empty();
    }
}

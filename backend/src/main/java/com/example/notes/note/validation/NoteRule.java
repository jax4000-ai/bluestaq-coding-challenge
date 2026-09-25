package com.example.notes.note.validation;

import com.example.notes.note.service.NoteDraft;

import reactor.core.publisher.Mono;

public interface NoteRule {
    Mono<Void> validate(NoteDraft draft);
}

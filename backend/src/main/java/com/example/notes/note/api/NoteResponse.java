package com.example.notes.note.api;

import java.time.Instant;

import com.example.notes.note.Note;
import com.example.notes.note.NoteStatus;
import com.example.notes.security.DataClassification;

public record NoteResponse(
        Long id,
        String teamId,
        String authorId,
        String title,
        String content,
        NoteStatus status,
        DataClassification classification,
        Instant createdAt,
        Instant updatedAt,
        Long version) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.id(),
                note.teamId(),
                note.authorId(),
                note.title(),
                note.content(),
                note.status(),
                note.classification(),
                note.createdAt(),
                note.updatedAt(),
                note.version());
    }
}

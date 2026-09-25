package com.example.notes.note.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.example.notes.note.Note;
import com.example.notes.note.NoteStatus;

@Component
public class NoteFactory {
    private final Clock clock;

    public NoteFactory(Clock clock) {
        this.clock = clock;
    }

    public Note create(NoteDraft draft) {
        Instant now = clock.instant();
        return new Note(
                null,
                draft.teamId().trim(),
                draft.authorId().trim(),
                draft.title().trim(),
                draft.content().trim(),
                NoteStatus.ACTIVE,
                draft.classification(),
                now,
                now,
                null);
    }
}

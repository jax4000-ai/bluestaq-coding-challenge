package com.example.notes.note.api;

import com.example.notes.security.DataClassification;

public record NoteEvent(
        String type,
        String teamId,
        DataClassification classification,
        NoteResponse note) {
}

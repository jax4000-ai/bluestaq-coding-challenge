package com.example.notes.note.service;

import com.example.notes.security.DataClassification;

public record NoteDraft(
        Long noteId,
        String teamId,
        String authorId,
        String title,
        String content,
        DataClassification classification) {
}

package com.example.notes.note.api;

import jakarta.validation.constraints.NotBlank;
import com.example.notes.security.DataClassification;

public record CreateNoteRequest(
        @NotBlank @jakarta.validation.constraints.Size(max = 120) String title,
        @NotBlank @jakarta.validation.constraints.Size(max = 10_000) String content,
        @jakarta.validation.constraints.NotNull DataClassification classification) {
}

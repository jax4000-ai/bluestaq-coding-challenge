package com.example.notes.note.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 10_000) String content,
        @NotNull @PositiveOrZero Long version) {
}

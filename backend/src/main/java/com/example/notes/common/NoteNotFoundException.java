package com.example.notes.common;

public class NoteNotFoundException extends RuntimeException {
    public NoteNotFoundException(Long id) {
        super("Note " + id + " was not found");
    }
}

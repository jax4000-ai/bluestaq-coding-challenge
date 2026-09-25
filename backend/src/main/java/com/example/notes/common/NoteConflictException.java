package com.example.notes.common;

public class NoteConflictException extends RuntimeException {
    public NoteConflictException(String message) {
        super(message);
    }
}

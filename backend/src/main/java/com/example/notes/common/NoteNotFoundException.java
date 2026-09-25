package com.example.notes.common;

public class NoteNotFoundException extends ApiException {
    public NoteNotFoundException(Long id) {
        super(ErrorCode.NOTE_NOT_FOUND, "Note " + id + " was not found");
    }
}

package com.example.notes.common;

public class NoteConflictException extends ApiException {
    public NoteConflictException(String message) {
        super(ErrorCode.NOTE_CONFLICT, message);
    }
}

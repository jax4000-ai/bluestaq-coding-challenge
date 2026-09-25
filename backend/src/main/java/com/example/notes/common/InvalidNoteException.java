package com.example.notes.common;

public class InvalidNoteException extends ApiException {
    public InvalidNoteException(String message) {
        super(ErrorCode.NOTE_VALIDATION_FAILED, message);
    }
}

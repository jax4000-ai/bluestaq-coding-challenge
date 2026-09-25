package com.example.notes.common;

public class InvalidNoteException extends RuntimeException {
    public InvalidNoteException(String message) {
        super(message);
    }
}

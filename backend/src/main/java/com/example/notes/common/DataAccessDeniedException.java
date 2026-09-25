package com.example.notes.common;

public class DataAccessDeniedException extends RuntimeException {
    public DataAccessDeniedException(String message) {
        super(message);
    }
}

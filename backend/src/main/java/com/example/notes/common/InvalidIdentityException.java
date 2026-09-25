package com.example.notes.common;

public class InvalidIdentityException extends RuntimeException {
    public InvalidIdentityException(String message) {
        super(message);
    }
}

package com.example.notes.common;

public class InvalidIdentityException extends ApiException {
    public InvalidIdentityException(String message) {
        super(ErrorCode.IDENTITY_INVALID, message);
    }
}

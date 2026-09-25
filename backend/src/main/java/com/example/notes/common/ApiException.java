package com.example.notes.common;

/** Base type for exceptions that map deterministically to a public {@link ErrorCode}. */
public abstract class ApiException extends RuntimeException {
    private final ErrorCode code;

    protected ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}

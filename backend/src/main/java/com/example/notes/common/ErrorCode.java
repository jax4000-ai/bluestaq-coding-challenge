package com.example.notes.common;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable error codes returned in every error response as the
 * {@code errorCode} extension property on {@link org.springframework.http.ProblemDetail}.
 * Codes are part of the API contract: clients should branch on {@code errorCode}, not on
 * the human-readable {@code title}, which may be reworded over time.
 */
public enum ErrorCode {
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Note not found"),
    NOTE_CONFLICT(HttpStatus.CONFLICT, "Edit conflict"),
    NOTE_VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Note rejected"),
    IDENTITY_INVALID(HttpStatus.UNAUTHORIZED, "Identity required"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    REQUEST_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase()),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}

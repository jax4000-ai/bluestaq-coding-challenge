package com.example.notes.common;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NoteNotFoundException.class)
    ProblemDetail notFound(NoteNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Note not found", exception.getMessage());
    }

    @ExceptionHandler(NoteConflictException.class)
    ProblemDetail conflict(NoteConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Edit conflict", exception.getMessage());
    }

    @ExceptionHandler(InvalidNoteException.class)
    ProblemDetail invalidNote(InvalidNoteException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Note rejected", exception.getMessage());
    }

    @ExceptionHandler(InvalidIdentityException.class)
    ProblemDetail invalidIdentity(InvalidIdentityException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Identity required", exception.getMessage());
    }

    @ExceptionHandler(DataAccessDeniedException.class)
    ProblemDetail accessDenied(DataAccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage());
    }

    @ExceptionHandler(ServerWebInputException.class)
    ProblemDetail malformed(ServerWebInputException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                exception.getBody().getDetail() == null ? "The request could not be read" : exception.getBody().getDetail());
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://team-notes.example/problems/" + status.value()));
        return problem;
    }
}

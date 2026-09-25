package com.example.notes.common;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import com.example.notes.observability.RequestMetricsRecorder;

/**
 * Single place where every exception becomes an RFC 9457 Problem Details response. Each
 * response carries a stable {@code errorCode} (see {@link ErrorCode}) and the correlating
 * {@code X-Request-Id}, and every rejection is logged and counted (see {@link RequestMetricsRecorder})
 * so operators can trace a client-visible error back to the request that caused it without
 * exposing internals to the caller.
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RequestMetricsRecorder metricsRecorder;

    public ApiExceptionHandler(RequestMetricsRecorder metricsRecorder) {
        this.metricsRecorder = metricsRecorder;
    }

    @ExceptionHandler(ApiException.class)
    ProblemDetail apiException(ApiException exception, ServerWebExchange exchange) {
        ErrorCode code = exception.code();
        log.warn(
                "requestId={} errorCode={} status={} path={} reason=\"{}\"",
                requestId(exchange),
                code,
                code.status().value(),
                exchange.getRequest().getPath(),
                exception.getMessage());
        return problem(exchange, code.status(), code.title(), exception.getMessage(), code);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    ProblemDetail bodyValidationFailed(WebExchangeBindException exception, ServerWebExchange exchange) {
        String detail = exception.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((first, second) -> first + "; " + second)
                .orElse("The request body failed validation");
        log.warn(
                "requestId={} errorCode={} status=400 path={} reason=\"{}\"",
                requestId(exchange),
                ErrorCode.REQUEST_VALIDATION_FAILED,
                exchange.getRequest().getPath(),
                detail);
        return problem(
                exchange, HttpStatus.BAD_REQUEST, ErrorCode.REQUEST_VALIDATION_FAILED.title(), detail,
                ErrorCode.REQUEST_VALIDATION_FAILED);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail parameterValidationFailed(HandlerMethodValidationException exception, ServerWebExchange exchange) {
        String detail = exception.getReason() == null
                ? "One or more request parameters failed validation"
                : exception.getReason();
        log.warn(
                "requestId={} errorCode={} status=400 path={} reason=\"{}\"",
                requestId(exchange),
                ErrorCode.REQUEST_VALIDATION_FAILED,
                exchange.getRequest().getPath(),
                detail);
        return problem(
                exchange, HttpStatus.BAD_REQUEST, ErrorCode.REQUEST_VALIDATION_FAILED.title(), detail,
                ErrorCode.REQUEST_VALIDATION_FAILED);
    }

    @ExceptionHandler(ServerWebInputException.class)
    ProblemDetail malformed(ServerWebInputException exception, ServerWebExchange exchange) {
        String detail = exception.getBody().getDetail() == null
                ? "The request could not be read"
                : exception.getBody().getDetail();
        log.warn(
                "requestId={} errorCode={} status=400 path={} reason=\"{}\"",
                requestId(exchange),
                ErrorCode.MALFORMED_REQUEST,
                exchange.getRequest().getPath(),
                detail);
        return problem(
                exchange, HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST.title(), detail,
                ErrorCode.MALFORMED_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, ServerWebExchange exchange) {
        log.error(
                "requestId={} errorCode={} status=500 path={} unhandled exception",
                requestId(exchange),
                ErrorCode.INTERNAL_ERROR,
                exchange.getRequest().getPath(),
                exception);
        return problem(
                exchange,
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR.title(),
                "An unexpected error occurred",
                ErrorCode.INTERNAL_ERROR);
    }

    private ProblemDetail problem(
            ServerWebExchange exchange, HttpStatus status, String title, String detail, ErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://team-notes.example/problems/" + status.value()));
        problem.setProperty("errorCode", code.name());
        problem.setProperty("requestId", requestId(exchange));
        metricsRecorder.recordErrorCode(code.name());
        return problem;
    }

    private String requestId(ServerWebExchange exchange) {
        String requestId = exchange.getResponse().getHeaders().getFirst(REQUEST_ID_HEADER);
        return requestId == null ? "unknown" : requestId;
    }
}

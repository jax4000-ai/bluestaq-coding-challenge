package com.example.notes.security;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class ResponseSecurityFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(ResponseSecurityFilter.class);
    private static final String REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String suppliedRequestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID);
        String requestId = suppliedRequestId != null && suppliedRequestId.matches("[A-Za-z0-9._-]{8,64}")
                ? suppliedRequestId
                : UUID.randomUUID().toString();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set(REQUEST_ID, requestId);
        headers.setCacheControl("no-store");
        headers.set("Pragma", "no-cache");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set(
                "Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; "
                        + "connect-src 'self'; img-src 'self' data:; frame-ancestors 'none'");

        Instant start = Instant.now();
        // doFinally runs for completion, error, and cancellation without blocking the
        // reactive pipeline, so this doubles as a lightweight structured access log.
        return chain.filter(exchange)
                .doFinally(signalType -> log.info(
                        "requestId={} method={} path={} status={} durationMs={} signal={}",
                        requestId,
                        exchange.getRequest().getMethod(),
                        exchange.getRequest().getPath(),
                        exchange.getResponse().getStatusCode(),
                        Duration.between(start, Instant.now()).toMillis(),
                        signalType));
    }
}

package com.example.notes.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.notes.common.InvalidIdentityException;

@Component
public class RequestActorResolver {
    private static final Logger log = LoggerFactory.getLogger(RequestActorResolver.class);

    public RequestActor resolve(String userId, DataClassification clearance) {
        if (userId == null || !userId.matches("[A-Za-z0-9._@-]{2,64}")) {
            // The raw value is intentionally not logged: it failed a safe-character check,
            // so echoing it back into logs would risk log-injection/forging.
            log.warn("rejected request: X-User-Id missing or not 2-64 safe identity characters");
            throw new InvalidIdentityException("X-User-Id must be 2-64 safe identity characters");
        }
        if (clearance == null) {
            log.warn("rejected request: X-User-Clearance header missing actorId={}", userId);
            throw new InvalidIdentityException("X-User-Clearance is required");
        }
        return new RequestActor(userId, clearance);
    }
}

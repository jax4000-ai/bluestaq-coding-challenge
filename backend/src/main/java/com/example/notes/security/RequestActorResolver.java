package com.example.notes.security;

import org.springframework.stereotype.Component;

import com.example.notes.common.InvalidIdentityException;

@Component
public class RequestActorResolver {
    public RequestActor resolve(String userId, DataClassification clearance) {
        if (userId == null || !userId.matches("[A-Za-z0-9._@-]{2,64}")) {
            throw new InvalidIdentityException("X-User-Id must be 2-64 safe identity characters");
        }
        if (clearance == null) {
            throw new InvalidIdentityException("X-User-Clearance is required");
        }
        return new RequestActor(userId, clearance);
    }
}

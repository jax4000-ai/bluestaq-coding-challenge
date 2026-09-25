package com.example.notes.security;

public record RequestActor(String userId, DataClassification clearance) {
    public boolean mayAccess(DataClassification classification) {
        return clearance.allows(classification);
    }
}

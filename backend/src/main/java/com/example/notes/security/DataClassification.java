package com.example.notes.security;

public enum DataClassification {
    PUBLIC,
    INTERNAL,
    CUI;

    public boolean allows(DataClassification dataClassification) {
        return ordinal() >= dataClassification.ordinal();
    }
}

package com.example.notes.common;

public class DataAccessDeniedException extends ApiException {
    public DataAccessDeniedException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }
}

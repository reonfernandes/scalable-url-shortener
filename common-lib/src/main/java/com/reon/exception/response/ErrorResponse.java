package com.reon.exception.response;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timeStamp,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(HttpStatus httpStatus, String message) {
        return new ErrorResponse(
                httpStatus.value(),
                httpStatus.name(),
                message,
                LocalDateTime.now(),
                null
        );
    }

    public static ErrorResponse ofValidation(Map<String, String> fieldErrors) {
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation_Failed",
                "Request validation failed",
                LocalDateTime.now(),
                fieldErrors
        );
    }
}

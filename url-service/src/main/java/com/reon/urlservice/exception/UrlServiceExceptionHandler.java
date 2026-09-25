package com.reon.urlservice.exception;

import com.reon.exception.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errors only url-service can raise. Runs before the shared GlobalExceptionHandler,
 * whose catch-all would otherwise turn these into a 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UrlServiceExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(UrlServiceExceptionHandler.class);

    /**
     * Two requests can pass the "is this alias free?" check at the same time; the unique
     * short_code column then rejects the second one.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateShortCode(DataIntegrityViolationException exception) {
        log.warn("Short code already exists: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, "Custom alias not available."));
    }
}

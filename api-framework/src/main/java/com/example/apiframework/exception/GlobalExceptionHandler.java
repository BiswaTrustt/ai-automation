package com.example.apiframework.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Centralized exception handler that translates framework exceptions
 * into RFC 7807 {@link ProblemDetail} responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiNotFoundException.class)
    public ProblemDetail handleApiNotFound(ApiNotFoundException ex) {
        log.error("API not found: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("API Not Found");
        detail.setType(URI.create("https://api-framework/errors/api-not-found"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(ApiExecutionException.class)
    public ProblemDetail handleExecutionError(ApiExecutionException ex) {
        log.error("API execution error: {}", ex.getMessage(), ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        detail.setTitle("API Execution Error");
        detail.setType(URI.create("https://api-framework/errors/execution-error"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericError(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        detail.setTitle("Internal Server Error");
        detail.setType(URI.create("https://api-framework/errors/internal-error"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}

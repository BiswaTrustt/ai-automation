package com.example.apiframework.exception;

/**
 * Thrown when an unrecoverable error occurs during API execution
 * (e.g. network failure, SSL error, serialisation error).
 *
 * <p>Distinct from a validation failure, which is represented by
 * {@link com.example.apiframework.enums.ExecutionStatus#FAILURE}
 * in the result object rather than an exception.</p>
 */
public class ApiExecutionException extends RuntimeException {

    /**
     * @param message human-readable description of the failure
     */
    public ApiExecutionException(String message) {
        super(message);
    }

    /**
     * @param message human-readable description of the failure
     * @param cause   the underlying exception
     */
    public ApiExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.example.apiframework.exception;

/**
 * Thrown when an API definition cannot be found in the database,
 * or when the found definition is inactive.
 */
public class ApiNotFoundException extends RuntimeException {

    /**
     * @param apiName the logical name that was looked up
     */
    public ApiNotFoundException(String apiName) {
        super("No active API found with name: " + apiName);
    }

    /**
     * @param apiName the logical name that was looked up
     * @param cause   the underlying cause
     */
    public ApiNotFoundException(String apiName, Throwable cause) {
        super("No active API found with name: " + apiName, cause);
    }
}

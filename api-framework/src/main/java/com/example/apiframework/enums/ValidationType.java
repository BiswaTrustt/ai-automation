package com.example.apiframework.enums;

/**
 * Strategies used to validate API responses.
 *
 * <p>Maps to the {@code validation_type} column in {@code api_validations}.</p>
 */
public enum ValidationType {

    /** Assert that the HTTP response status code equals {@code expected_value}. */
    STATUS_CODE,

    /** Evaluate a JsonPath expression and compare the result to {@code expected_value}. */
    JSON_PATH,

    /** Assert that the JsonPath result (or whole body) is not null/empty. */
    NOT_NULL,

    /** Assert that the response body contains the {@code expected_value} string. */
    CONTAINS,

    /** Assert that the JsonPath result matches the {@code expected_value} regex. */
    REGEX
}

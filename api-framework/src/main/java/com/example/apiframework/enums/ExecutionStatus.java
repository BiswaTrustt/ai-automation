package com.example.apiframework.enums;

/**
 * Possible outcomes of an API execution attempt.
 *
 * <p>Maps to the {@code execution_status} column in {@code api_execution_history}.</p>
 */
public enum ExecutionStatus {

    /** HTTP call completed and all mandatory validations passed. */
    SUCCESS,

    /** HTTP call completed but one or more mandatory validations failed. */
    FAILURE,

    /** An unexpected exception occurred during execution or validation. */
    ERROR,

    /** The HTTP call exceeded the configured timeout threshold. */
    TIMEOUT
}

package com.example.apiframework.dto;

import com.example.apiframework.enums.ExecutionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable result object returned after a complete API execution cycle
 * (HTTP call + response validation + history persistence).
 */
@Value
@Builder
public class ExecutionResultDto {

    /** Database identifier of the persisted {@code api_execution_history} record. */
    Long executionId;

    /** Logical name of the API that was executed. */
    String apiName;

    /** HTTP response status code. {@code -1} if a network/timeout error occurred. */
    int statusCode;

    /** Raw response body. {@code null} on network error. */
    String responseBody;

    /** Final outcome of the execution + validation cycle. */
    ExecutionStatus executionStatus;

    /** Wall-clock duration of the HTTP round-trip in milliseconds. */
    long executionTimeMs;

    /** Individual validation outcomes for each configured rule. */
    List<ValidationResultDto> validationResults;

    /** Timestamp when the execution record was created in the database. */
    LocalDateTime executedAt;

    /** Human-readable summary message. */
    String message;

    /**
     * Convenience predicate.
     *
     * @return {@code true} when {@code executionStatus} is {@link ExecutionStatus#SUCCESS}
     */
    public boolean isSuccess() {
        return ExecutionStatus.SUCCESS.equals(executionStatus);
    }
}

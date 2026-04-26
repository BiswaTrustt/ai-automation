package com.example.apiframework.dto;

import com.example.apiframework.enums.ValidationType;
import lombok.Builder;
import lombok.Value;

/**
 * Outcome of evaluating a single {@link ValidationRuleDto} against an API response.
 */
@Value
@Builder
public class ValidationResultDto {

    /** The rule that was evaluated. */
    Long validationId;

    /** The validation strategy that was applied. */
    ValidationType validationType;

    /** The JsonPath or context description for this assertion. */
    String jsonPath;

    /** The value that was expected. */
    String expectedValue;

    /** The value actually extracted from the response. */
    String actualValue;

    /** {@code true} if the assertion passed. */
    boolean passed;

    /** {@code true} if a failure would mark the execution as {@code FAILURE}. */
    boolean mandatory;

    /** Human-readable reason for failure, or {@code null} on pass. */
    String failureReason;
}

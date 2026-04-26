package com.example.apiframework.dto;

import com.example.apiframework.enums.ValidationType;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable representation of a single response validation rule.
 */
@Value
@Builder
public class ValidationRuleDto {

    /** Database identifier of this rule. */
    Long validationId;

    /**
     * JsonPath expression used to extract the value to test.
     * {@code null} for {@code STATUS_CODE} rules.
     */
    String jsonPath;

    /**
     * The value the extracted result must equal / contain / match.
     * {@code null} for {@code NOT_NULL} rules.
     */
    String expectedValue;

    /** The type of assertion to perform. */
    ValidationType validationType;

    /**
     * When {@code true}, a failure marks the entire execution as {@code FAILURE}.
     * When {@code false}, failures are logged as warnings only.
     */
    boolean mandatory;
}

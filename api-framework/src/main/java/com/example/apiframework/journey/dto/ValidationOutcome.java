package com.example.apiframework.journey.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ValidationOutcome {
    String validationKey;
    String expectedValue;
    String actualValue;
    boolean passed;
    String source;
}

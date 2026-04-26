package com.example.apiframework.journey.dto;

import com.example.apiframework.journey.enums.JourneyStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ApiStepResult {
    String apiName;
    int executionOrder;
    JourneyStatus status;
    Integer httpStatusCode;
    long executionTimeMs;
    String requestPayload;
    String responsePayload;
    List<ValidationOutcome> dbValidations;
    List<ValidationOutcome> csvValidations;
    String errorMessage;
}

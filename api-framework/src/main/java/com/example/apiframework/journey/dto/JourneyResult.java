package com.example.apiframework.journey.dto;

import com.example.apiframework.journey.enums.JourneyStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class JourneyResult {
    String journeyRunId;
    String moduleCode;
    String scenarioCode;
    JourneyStatus status;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
    long totalDurationMs;
    List<ApiStepResult> steps;
}

package com.example.apiframework.journey.executor;

import com.example.apiframework.journey.dto.JourneyResult;
import com.example.apiframework.journey.service.JourneyExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Public entry point for the journey framework.
 *
 * <pre>{@code
 *   journeyExecutor.execute("QDE", "VALID_CUSTOMER");
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class JourneyExecutor {

    private final JourneyExecutionService service;

    public JourneyResult execute(String moduleCode, String scenarioCode) {
        return service.execute(moduleCode, scenarioCode);
    }
}

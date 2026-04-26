package com.example.apiframework.journey.executor;

import com.example.apiframework.journey.dto.JourneyResult;
import com.example.apiframework.journey.service.JourneyExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Public entry point for the journey framework.
 *
 * <pre>{@code
 *   // legacy 2-arg call (uses scenario rows where product_id IS NULL)
 *   journeyExecutor.execute("QDE", "VALID_CUSTOMER");
 *
 *   // product-aware 3-arg call (uses scenario rows tagged with product_id)
 *   journeyExecutor.execute("JLG", "QDE", "VALID_CUSTOMER");
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class JourneyExecutor {

    private final JourneyExecutionService service;

    public JourneyResult execute(String moduleCode, String scenarioCode) {
        return service.execute(null, moduleCode, scenarioCode);
    }

    public JourneyResult execute(String loanProductCode, String moduleCode, String scenarioCode) {
        return service.execute(loanProductCode, moduleCode, scenarioCode);
    }
}

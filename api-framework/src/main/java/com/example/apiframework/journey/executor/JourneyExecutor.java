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
 *
 *   // env + members aware (full form)
 *   journeyExecutor.execute("qa2", "SHG", "LOGIN_SO", 5, "SHG_E2E_FLOW");
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class JourneyExecutor {

    private final JourneyExecutionService service;

    public JourneyResult execute(String moduleCode, String scenarioCode) {
        return service.execute(null, null, moduleCode, null, scenarioCode);
    }

    public JourneyResult execute(String loanProductCode, String moduleCode, String scenarioCode) {
        return service.execute(null, loanProductCode, moduleCode, null, scenarioCode);
    }

    public JourneyResult execute(String envCode,
                                 String loanProductCode,
                                 String moduleCode,
                                 Integer memberCount,
                                 String scenarioCode) {
        return service.execute(envCode, loanProductCode, moduleCode, memberCount, scenarioCode);
    }
}

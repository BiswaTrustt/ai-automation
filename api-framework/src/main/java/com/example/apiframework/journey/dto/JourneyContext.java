package com.example.apiframework.journey.dto;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-run mutable context: holds responses keyed by API name (for ${RESPONSE:...}
 * resolution), captured journey-level variables, and identification info.
 */
@Getter
public class JourneyContext {

    private final String journeyRunId = UUID.randomUUID().toString();
    private final String loanProductCode;
    private final String moduleCode;
    private final String scenarioCode;
    private final Map<String, String> responsesByApi = new LinkedHashMap<>();
    private final Map<String, String> capturedValues = new LinkedHashMap<>();

    public JourneyContext(String moduleCode, String scenarioCode) {
        this(null, moduleCode, scenarioCode);
    }

    public JourneyContext(String loanProductCode, String moduleCode, String scenarioCode) {
        this.loanProductCode = loanProductCode;
        this.moduleCode = moduleCode;
        this.scenarioCode = scenarioCode;
    }

    public void recordResponse(String apiName, String responseBody) {
        if (apiName != null && responseBody != null) {
            responsesByApi.put(apiName, responseBody);
        }
    }

    public void capture(String key, String value) {
        capturedValues.put(key, value);
    }
}

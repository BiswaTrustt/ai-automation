package com.example.apiframework.journey.service;

import com.example.apiframework.journey.dto.JourneyContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseMappingService {

    private final ObjectMapper objectMapper;

    /** Always store the raw response keyed by API name (for ${RESPONSE:...} resolution). */
    public void capture(JourneyContext ctx, String apiName, String responseBody) {
        ctx.recordResponse(apiName, responseBody);
    }

    /**
     * Apply the per-step extraction map. Each (varName -> jsonPath) pulls a value
     * from the response and stores it into the context's capturedValues, where it
     * is later resolved by ${CTX:varName} in downstream requests.
     */
    public void applyExtractions(JourneyContext ctx, String responseBody, String extractionMappingsJson) {
        if (responseBody == null || responseBody.isBlank()) return;
        if (extractionMappingsJson == null || extractionMappingsJson.isBlank()) return;
        Map<String, String> mappings;
        try {
            mappings = objectMapper.readValue(extractionMappingsJson, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Invalid extraction_mappings JSON [{}]: {}", extractionMappingsJson, ex.getMessage());
            return;
        }
        for (Map.Entry<String, String> e : mappings.entrySet()) {
            String varName  = e.getKey();
            String jsonPath = e.getValue();
            try {
                Object value = JsonPath.read(responseBody, jsonPath);
                ctx.capture(varName, value == null ? "" : value.toString());
            } catch (PathNotFoundException pnf) {
                log.debug("Extraction '{}' path '{}' not found in response", varName, jsonPath);
                ctx.capture(varName, "");
            } catch (Exception ex) {
                log.warn("Extraction '{}' path '{}' failed: {}", varName, jsonPath, ex.getMessage());
            }
        }
    }
}

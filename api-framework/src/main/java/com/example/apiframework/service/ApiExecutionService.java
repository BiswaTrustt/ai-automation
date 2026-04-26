package com.example.apiframework.service;

import com.example.apiframework.dto.ApiMetadataDto;
import com.example.apiframework.dto.ExecutionResultDto;
import com.example.apiframework.entity.ApiExecutionHistory;
import com.example.apiframework.enums.ExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Top-level orchestration service for the API execution pipeline.
 *
 * <h2>Execution flow</h2>
 * <ol>
 *   <li>Load API metadata from PostgreSQL ({@link ApiMetadataService})</li>
 *   <li>Resolve headers and request body placeholders</li>
 *   <li>Execute HTTP call via REST Assured ({@link GenericApiExecutor})</li>
 *   <li>Validate response ({@link ResponseValidationEngine} – called internally)</li>
 *   <li>Persist execution history ({@link ExecutionHistoryService})</li>
 *   <li>Return a fully-populated {@link ExecutionResultDto}</li>
 * </ol>
 *
 * <p>This service is thread-safe; each invocation is fully independent.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiExecutionService {

    private final ApiMetadataService apiMetadataService;
    private final GenericApiExecutor genericApiExecutor;
    private final ExecutionHistoryService executionHistoryService;

    /**
     * Executes the named API with the supplied dynamic placeholder values,
     * using no additional authentication credentials.
     *
     * @param apiName       logical name registered in {@code api_master}
     * @param dynamicValues placeholder key-value pairs (e.g. {@code CUSTOMER_ID=501147})
     * @return the execution + validation result
     */
    public ExecutionResultDto execute(String apiName, Map<String, String> dynamicValues) {
        return execute(apiName, dynamicValues, Collections.emptyMap());
    }

    /**
     * Executes the named API with the supplied dynamic values and authentication credentials.
     *
     * @param apiName       logical name registered in {@code api_master}
     * @param dynamicValues placeholder key-value pairs (e.g. {@code CUSTOMER_ID=501147})
     * @param credentials   authentication values (e.g. {@code BEARER_TOKEN=abc123})
     * @return the execution + validation result
     */
    public ExecutionResultDto execute(String apiName,
                                      Map<String, String> dynamicValues,
                                      Map<String, String> credentials) {
        log.info("===== BEGIN API EXECUTION: '{}' =====", apiName);

        // 1. Load metadata
        ApiMetadataDto metadata = apiMetadataService.loadMetadata(apiName, dynamicValues);

        // 2. Execute + Validate
        GenericApiExecutor.ExecutionOutcome outcome =
                genericApiExecutor.execute(metadata, credentials);

        // 3. Persist history
        ApiExecutionHistory history = executionHistoryService.save(
                metadata.getApiId(),
                metadata.getResolvedRequestBody(),
                outcome.getResponseBody(),
                outcome.getStatusCode(),
                outcome.getExecutionStatus(),
                outcome.getExecutionTimeMs(),
                outcome.getErrorMessage()
        );

        // 4. Build result DTO
        String message = buildMessage(apiName, outcome);
        ExecutionResultDto result = ExecutionResultDto.builder()
                .executionId(history.getExecutionId())
                .apiName(apiName)
                .statusCode(outcome.getStatusCode())
                .responseBody(outcome.getResponseBody())
                .executionStatus(outcome.getExecutionStatus())
                .executionTimeMs(outcome.getExecutionTimeMs())
                .validationResults(outcome.getValidationResults())
                .executedAt(LocalDateTime.now())
                .message(message)
                .build();

        log.info("===== END API EXECUTION: '{}' | status={} | time={}ms | executionId={} =====",
                apiName, result.getExecutionStatus(), result.getExecutionTimeMs(),
                result.getExecutionId());

        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildMessage(String apiName, GenericApiExecutor.ExecutionOutcome outcome) {
        return switch (outcome.getExecutionStatus()) {
            case SUCCESS -> String.format(
                    "API '%s' executed successfully in %dms with HTTP %d",
                    apiName, outcome.getExecutionTimeMs(), outcome.getStatusCode());
            case FAILURE -> String.format(
                    "API '%s' executed but %d validation(s) failed",
                    apiName, outcome.getValidationResults().stream()
                            .filter(v -> !v.isPassed() && v.isMandatory()).count());
            case ERROR -> String.format(
                    "API '%s' encountered an error: %s",
                    apiName, outcome.getErrorMessage());
            case TIMEOUT -> String.format(
                    "API '%s' timed out after %dms",
                    apiName, outcome.getExecutionTimeMs());
        };
    }
}

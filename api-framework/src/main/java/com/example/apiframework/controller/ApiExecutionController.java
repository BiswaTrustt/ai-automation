package com.example.apiframework.controller;

import com.example.apiframework.dto.ExecutionResultDto;
import com.example.apiframework.service.ApiExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing the API execution pipeline over HTTP.
 *
 * <p>This endpoint allows external tools (CI systems, dashboards) to trigger
 * API executions and retrieve structured results without starting a JUnit test run.</p>
 *
 * <p>Base path: {@code /api/v1/execute}</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
public class ApiExecutionController {

    private final ApiExecutionService apiExecutionService;

    /**
     * Executes a registered API by name.
     *
     * <p>Example request body:
     * <pre>{@code
     * {
     *   "dynamicValues": { "CUSTOMER_ID": "501147", "MOBILE_NUMBER": "9816923672" },
     *   "credentials":   {}
     * }
     * }</pre>
     *
     * @param apiName      path variable identifying the API to run
     * @param requestBody  JSON body with optional {@code dynamicValues} and {@code credentials}
     * @return the {@link ExecutionResultDto} with full execution details
     */
    @PostMapping("/{apiName}")
    public ResponseEntity<ExecutionResultDto> execute(
            @PathVariable String apiName,
            @RequestBody(required = false) Map<String, Map<String, String>> requestBody) {

        log.info("Received execution request for API '{}'", apiName);

        Map<String, String> dynamicValues = requestBody != null
                ? requestBody.getOrDefault("dynamicValues", Map.of())
                : Map.of();

        Map<String, String> credentials = requestBody != null
                ? requestBody.getOrDefault("credentials", Map.of())
                : Map.of();

        ExecutionResultDto result = apiExecutionService.execute(apiName, dynamicValues, credentials);
        return ResponseEntity.ok(result);
    }
}

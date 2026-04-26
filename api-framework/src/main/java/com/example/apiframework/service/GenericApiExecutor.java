package com.example.apiframework.service;

import com.example.apiframework.dto.ApiMetadataDto;
import com.example.apiframework.dto.ValidationResultDto;
import com.example.apiframework.enums.ExecutionStatus;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds and executes an HTTP request from a fully-resolved {@link ApiMetadataDto}
 * using REST Assured, then validates the response.
 *
 * <p>This class is stateless and thread-safe. Each call to {@link #execute}
 * produces an independent {@link ExecutionOutcome} without sharing mutable state.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenericApiExecutor {

    private final RestAssuredConfig restAssuredConfig;
    private final AuthenticationHandler authenticationHandler;
    private final ResponseValidationEngine validationEngine;

    /**
     * Executes the API described by {@code metadata} and validates the response.
     *
     * @param metadata    fully-resolved API metadata (headers, body, rules all set)
     * @param credentials optional authentication credentials keyed by well-known names
     * @return an {@link ExecutionOutcome} containing the response and validation results
     */
    public ExecutionOutcome execute(ApiMetadataDto metadata, Map<String, String> credentials) {
        String fullUrl = metadata.getBaseUrl() + metadata.getEndpoint();
        log.info("Executing API '{}' [{}] {}",
                metadata.getApiName(), metadata.getHttpMethod(), fullUrl);

        long startTime = System.currentTimeMillis();

        try {
            // --- Build request specification ---
            RequestSpecification spec = RestAssured
                    .given()
                    .config(restAssuredConfig)
                    .headers(metadata.getResolvedHeaders())
                    .contentType(metadata.getContentType());

            // Apply authentication
            authenticationHandler.apply(spec, metadata.getAuthType(),
                    credentials != null ? credentials : Collections.emptyMap());

            // Attach body for non-GET methods
            if (metadata.getResolvedRequestBody() != null
                    && !metadata.getResolvedRequestBody().isBlank()) {
                spec.body(metadata.getResolvedRequestBody());
                log.debug("Request body: {}", metadata.getResolvedRequestBody());
            }

            // --- Dispatch ---
            Response response = dispatch(spec, metadata.getHttpMethod(), fullUrl);
            long elapsed = System.currentTimeMillis() - startTime;

            log.info("API '{}' responded in {}ms with HTTP {}",
                    metadata.getApiName(), elapsed, response.getStatusCode());

            // --- Validate ---
            List<ValidationResultDto> validations =
                    validationEngine.validate(response, metadata.getValidationRules());

            boolean anyMandatoryFailed = validations.stream()
                    .anyMatch(v -> !v.isPassed() && v.isMandatory());

            ExecutionStatus status = anyMandatoryFailed
                    ? ExecutionStatus.FAILURE
                    : ExecutionStatus.SUCCESS;

            return ExecutionOutcome.builder()
                    .response(response)
                    .statusCode(response.getStatusCode())
                    .responseBody(response.getBody().asString())
                    .executionStatus(status)
                    .executionTimeMs(elapsed)
                    .validationResults(validations)
                    .errorMessage(null)
                    .build();

        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("API execution failed for '{}' after {}ms: {}",
                    metadata.getApiName(), elapsed, ex.getMessage(), ex);

            return ExecutionOutcome.builder()
                    .response(null)
                    .statusCode(-1)
                    .responseBody(null)
                    .executionStatus(ExecutionStatus.ERROR)
                    .executionTimeMs(elapsed)
                    .validationResults(Collections.emptyList())
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // HTTP dispatch
    // -------------------------------------------------------------------------

    /**
     * Dispatches the request using the appropriate HTTP method.
     *
     * @param spec       the configured REST Assured spec
     * @param httpMethod the HTTP verb (case-insensitive)
     * @param url        the fully-qualified request URL
     * @return the REST Assured {@link Response}
     * @throws IllegalArgumentException if the HTTP method is not supported
     */
    private Response dispatch(RequestSpecification spec, String httpMethod, String url) {
        return switch (httpMethod.toUpperCase()) {
            case "GET"    -> spec.when().get(url);
            case "POST"   -> spec.when().post(url);
            case "PUT"    -> spec.when().put(url);
            case "PATCH"  -> spec.when().patch(url);
            case "DELETE" -> spec.when().delete(url);
            case "HEAD"   -> spec.when().head(url);
            case "OPTIONS"-> spec.when().options(url);
            default -> throw new IllegalArgumentException(
                    "Unsupported HTTP method: " + httpMethod);
        };
    }

    // -------------------------------------------------------------------------
    // Result container
    // -------------------------------------------------------------------------

    /**
     * Immutable container holding the outcome of one HTTP execution + validation cycle.
     */
    @lombok.Value
    @lombok.Builder
    public static class ExecutionOutcome {
        Response response;
        int statusCode;
        String responseBody;
        ExecutionStatus executionStatus;
        long executionTimeMs;
        List<ValidationResultDto> validationResults;
        String errorMessage;
    }
}

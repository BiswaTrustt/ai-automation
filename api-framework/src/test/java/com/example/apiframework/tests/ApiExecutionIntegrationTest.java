package com.example.apiframework.tests;

import com.example.apiframework.base.BaseIntegrationTest;
import com.example.apiframework.dto.ExecutionResultDto;
import com.example.apiframework.dto.ValidationResultDto;
import com.example.apiframework.enums.ExecutionStatus;
import com.example.apiframework.service.ApiExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the full API execution pipeline.
 *
 * <p>Tests execute against real endpoints configured in the database.
 * Ensure the target environment (qa2-mfi.novopay.in) is reachable before running.</p>
 *
 * <p>Each test method documents its intent in the {@code @DisplayName} and
 * performs focused assertions on the {@link ExecutionResultDto}.</p>
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiExecutionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApiExecutionService apiExecutionService;

    // -------------------------------------------------------------------------
    // getBorrowerLoanApplicationDetailsByCustomerId
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Execute getBorrowerLoanApplicationDetailsByCustomerId - happy path")
    void testGetBorrowerLoanApplicationDetails_HappyPath() {
        // Arrange
        Map<String, String> dynamicValues = Map.of(
                "CUSTOMER_ID",   "501147",
                "MOBILE_NUMBER", "9816923672"
        );

        // Act
        ExecutionResultDto result = apiExecutionService.execute(
                "getBorrowerLoanApplicationDetailsByCustomerId",
                dynamicValues
        );

        // Assert – execution metadata
        assertNotNull(result,                 "Result must not be null");
        assertNotNull(result.getExecutionId(), "Execution ID must be persisted");
        assertNotNull(result.getApiName(),     "API name must be set");
        assertNotNull(result.getExecutedAt(),  "Execution timestamp must be set");
        assertTrue(result.getExecutionTimeMs() >= 0, "Execution time must be non-negative");

        // Assert – HTTP outcome
        assertEquals(200, result.getStatusCode(),
                "Expected HTTP 200 but got: " + result.getStatusCode()
                        + "\nResponse: " + result.getResponseBody());

        // Assert – validation results
        assertNotNull(result.getValidationResults(), "Validation results must not be null");

        result.getValidationResults().forEach(v -> {
            if (v.isMandatory()) {
                assertTrue(v.isPassed(),
                        String.format("Mandatory validation failed: type=%s jsonPath=%s expected=%s actual=%s reason=%s",
                                v.getValidationType(), v.getJsonPath(),
                                v.getExpectedValue(), v.getActualValue(),
                                v.getFailureReason()));
            }
        });

        // Assert – overall status
        assertEquals(ExecutionStatus.SUCCESS, result.getExecutionStatus(),
                "Expected SUCCESS but got " + result.getExecutionStatus()
                        + "\nMessage: " + result.getMessage());

        log.info("Test passed. ExecutionId={} time={}ms",
                result.getExecutionId(), result.getExecutionTimeMs());
    }

    @Test
    @Order(2)
    @DisplayName("Execute getBorrowerLoanApplicationDetailsByCustomerId - result is persisted")
    void testGetBorrowerLoanApplicationDetails_ResultPersisted() {
        Map<String, String> dynamicValues = Map.of(
                "CUSTOMER_ID",   "501147",
                "MOBILE_NUMBER", "9816923672"
        );

        ExecutionResultDto result = apiExecutionService.execute(
                "getBorrowerLoanApplicationDetailsByCustomerId",
                dynamicValues
        );

        // The execution ID is only non-null if persistence succeeded
        assertNotNull(result.getExecutionId(),
                "Execution history must be persisted and return a generated ID");
        assertTrue(result.getExecutionId() > 0,
                "Execution ID must be a positive long: " + result.getExecutionId());
    }

    @Test
    @Order(3)
    @DisplayName("Execute getBorrowerLoanApplicationDetailsByCustomerId - validation rules evaluated")
    void testGetBorrowerLoanApplicationDetails_ValidationRulesEvaluated() {
        Map<String, String> dynamicValues = Map.of(
                "CUSTOMER_ID",   "501147",
                "MOBILE_NUMBER", "9816923672"
        );

        ExecutionResultDto result = apiExecutionService.execute(
                "getBorrowerLoanApplicationDetailsByCustomerId",
                dynamicValues
        );

        assertFalse(result.getValidationResults().isEmpty(),
                "At least one validation rule should have been evaluated");

        for (ValidationResultDto vr : result.getValidationResults()) {
            log.info("Validation: type={} jsonPath='{}' expected='{}' actual='{}' passed={}",
                    vr.getValidationType(), vr.getJsonPath(),
                    vr.getExpectedValue(), vr.getActualValue(), vr.isPassed());
        }
    }

    // -------------------------------------------------------------------------
    // Error handling
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("Execute non-existent API - should throw ApiNotFoundException")
    void testExecute_NonExistentApi_ThrowsException() {
        assertThrows(
                com.example.apiframework.exception.ApiNotFoundException.class,
                () -> apiExecutionService.execute("nonExistentApiName_xyz", Map.of()),
                "Should throw ApiNotFoundException for unknown API name"
        );
    }

    @Test
    @Order(11)
    @DisplayName("Execute API with missing dynamic value - placeholders remain unresolved")
    void testExecute_MissingDynamicValue_UnresolvedPlaceholder() {
        // Provide only one of the two required placeholders
        Map<String, String> dynamicValues = Map.of("CUSTOMER_ID", "501147");
        // MOBILE_NUMBER is missing – ${MOBILE_NUMBER} will remain in the header

        // Execution should proceed (not throw); the status depends on the API's tolerance
        ExecutionResultDto result = apiExecutionService.execute(
                "getBorrowerLoanApplicationDetailsByCustomerId",
                dynamicValues
        );

        assertNotNull(result, "Result must not be null even with unresolved placeholders");
        log.info("Result with missing placeholder: status={} httpCode={}",
                result.getExecutionStatus(), result.getStatusCode());
    }
}

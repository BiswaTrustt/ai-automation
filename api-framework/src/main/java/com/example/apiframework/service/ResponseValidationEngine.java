package com.example.apiframework.service;

import com.example.apiframework.dto.ValidationResultDto;
import com.example.apiframework.dto.ValidationRuleDto;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Evaluates a list of {@link ValidationRuleDto} rules against a REST Assured
 * {@link Response} and returns a {@link ValidationResultDto} for each rule.
 *
 * <p>The engine is stateless and thread-safe.</p>
 */
@Slf4j
@Component
public class ResponseValidationEngine {

    /**
     * Evaluates all supplied rules against {@code response}.
     *
     * @param response        the REST Assured response to validate
     * @param validationRules the ordered list of rules to apply
     * @return one {@link ValidationResultDto} per input rule
     */
    public List<ValidationResultDto> validate(Response response,
                                              List<ValidationRuleDto> validationRules) {
        List<ValidationResultDto> results = new ArrayList<>();

        for (ValidationRuleDto rule : validationRules) {
            ValidationResultDto result = evaluateRule(response, rule);
            results.add(result);

            if (result.isPassed()) {
                log.info("PASS [{}] jsonPath='{}' expected='{}' actual='{}'",
                        rule.getValidationType(), rule.getJsonPath(),
                        rule.getExpectedValue(), result.getActualValue());
            } else {
                if (rule.isMandatory()) {
                    log.error("FAIL (mandatory) [{}] jsonPath='{}' expected='{}' actual='{}' reason='{}'",
                            rule.getValidationType(), rule.getJsonPath(),
                            rule.getExpectedValue(), result.getActualValue(),
                            result.getFailureReason());
                } else {
                    log.warn("FAIL (optional) [{}] jsonPath='{}' expected='{}' actual='{}'",
                            rule.getValidationType(), rule.getJsonPath(),
                            rule.getExpectedValue(), result.getActualValue());
                }
            }
        }

        return results;
    }

    // -------------------------------------------------------------------------
    // Internal dispatch
    // -------------------------------------------------------------------------

    private ValidationResultDto evaluateRule(Response response, ValidationRuleDto rule) {
        try {
            return switch (rule.getValidationType()) {
                case STATUS_CODE -> validateStatusCode(response, rule);
                case JSON_PATH   -> validateJsonPath(response, rule);
                case NOT_NULL    -> validateNotNull(response, rule);
                case CONTAINS    -> validateContains(response, rule);
                case REGEX       -> validateRegex(response, rule);
            };
        } catch (Exception ex) {
            log.error("Exception evaluating rule id={}: {}", rule.getValidationId(), ex.getMessage(), ex);
            return failResult(rule, null, "Evaluation exception: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Rule implementations
    // -------------------------------------------------------------------------

    private ValidationResultDto validateStatusCode(Response response, ValidationRuleDto rule) {
        String actual = String.valueOf(response.getStatusCode());
        boolean passed = actual.equals(rule.getExpectedValue());
        return resultOf(rule, actual, passed,
                passed ? null : "Expected status " + rule.getExpectedValue() + " but got " + actual);
    }

    private ValidationResultDto validateJsonPath(Response response, ValidationRuleDto rule) {
        String actual = extractJsonPath(response, rule.getJsonPath());
        boolean passed = rule.getExpectedValue() != null
                && rule.getExpectedValue().equals(actual);
        return resultOf(rule, actual, passed,
                passed ? null : "JsonPath '" + rule.getJsonPath()
                        + "' expected '" + rule.getExpectedValue()
                        + "' but got '" + actual + "'");
    }

    private ValidationResultDto validateNotNull(Response response, ValidationRuleDto rule) {
        String actual;
        if (rule.getJsonPath() != null && !rule.getJsonPath().isBlank()) {
            actual = extractJsonPath(response, rule.getJsonPath());
        } else {
            actual = response.getBody().asString();
        }
        boolean passed = actual != null && !actual.isBlank() && !"null".equalsIgnoreCase(actual);
        return resultOf(rule, actual, passed,
                passed ? null : "Value at '" + rule.getJsonPath() + "' was null or empty");
    }

    private ValidationResultDto validateContains(Response response, ValidationRuleDto rule) {
        String body = response.getBody().asString();
        boolean passed = body != null
                && rule.getExpectedValue() != null
                && body.contains(rule.getExpectedValue());
        return resultOf(rule, body == null ? null : "(response body)",
                passed, passed ? null : "Response body does not contain '" + rule.getExpectedValue() + "'");
    }

    private ValidationResultDto validateRegex(Response response, ValidationRuleDto rule) {
        String actual = extractJsonPath(response, rule.getJsonPath());
        boolean passed = actual != null
                && rule.getExpectedValue() != null
                && Pattern.matches(rule.getExpectedValue(), actual);
        return resultOf(rule, actual, passed,
                passed ? null : "'" + actual + "' does not match regex '" + rule.getExpectedValue() + "'");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String extractJsonPath(Response response, String jsonPath) {
        try {
            Object value = response.jsonPath().get(jsonPath);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            log.debug("Failed to extract JsonPath '{}': {}", jsonPath, ex.getMessage());
            return null;
        }
    }

    private ValidationResultDto resultOf(ValidationRuleDto rule, String actual,
                                         boolean passed, String failureReason) {
        return ValidationResultDto.builder()
                .validationId(rule.getValidationId())
                .validationType(rule.getValidationType())
                .jsonPath(rule.getJsonPath())
                .expectedValue(rule.getExpectedValue())
                .actualValue(actual)
                .passed(passed)
                .mandatory(rule.isMandatory())
                .failureReason(failureReason)
                .build();
    }

    private ValidationResultDto failResult(ValidationRuleDto rule, String actual, String reason) {
        return resultOf(rule, actual, false, reason);
    }
}

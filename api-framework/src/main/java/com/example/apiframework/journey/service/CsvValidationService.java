package com.example.apiframework.journey.service;

import com.example.apiframework.journey.csv.CsvExpectedResultReader;
import com.example.apiframework.journey.dto.ValidationOutcome;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Compares CSV-defined expected values against the actual API response and
 * any captured DB values. Looks for {@code expected/<scenario_code>.csv}.
 */
@Service
@RequiredArgsConstructor
public class CsvValidationService {

    private final CsvExpectedResultReader csvReader;

    public List<ValidationOutcome> validate(String scenarioCode,
                                            String apiName,
                                            String responseBody,
                                            Map<String, String> dbValues) {
        Map<String, String> expected = csvReader.expectedFor(
                scenarioCode + ".csv", scenarioCode, apiName);

        List<ValidationOutcome> out = new ArrayList<>();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            String key = e.getKey();
            String expectedVal = e.getValue();
            String actual = lookupActual(key, responseBody, dbValues);
            out.add(ValidationOutcome.builder()
                    .validationKey(key)
                    .expectedValue(expectedVal)
                    .actualValue(actual)
                    .passed(Objects.equals(expectedVal, actual))
                    .source("CSV")
                    .build());
        }
        return out;
    }

    /**
     * Resolution order for CSV validation_key:
     *   1. JSONPath ($.foo.bar) → response body
     *   2. Match key in dbValues map
     *   3. null
     */
    private String lookupActual(String key, String responseBody, Map<String, String> dbValues) {
        if (key.startsWith("$") && responseBody != null) {
            try {
                Object v = JsonPath.read(responseBody, key);
                return v == null ? null : v.toString();
            } catch (PathNotFoundException ignored) {
                return null;
            }
        }
        return dbValues == null ? null : dbValues.get(key);
    }
}

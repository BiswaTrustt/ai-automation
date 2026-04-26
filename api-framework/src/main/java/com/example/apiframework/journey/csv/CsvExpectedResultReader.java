package com.example.apiframework.journey.csv;

import com.opencsv.CSVReaderHeaderAware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads expected-result CSV files from {@code classpath:/expected/} and from
 * absolute filesystem paths. Files are cached in memory after first read.
 *
 * Expected schema:
 *   scenario_code,api_name,validation_key,expected_value
 */
@Slf4j
@Component
public class CsvExpectedResultReader {

    private final Map<String, List<Map<String, String>>> cache = new ConcurrentHashMap<>();

    /**
     * Returns expected values for a given scenario+api as a key→value map.
     */
    public Map<String, String> expectedFor(String fileName, String scenarioCode, String apiName) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map<String, String> row : load(fileName)) {
            if (scenarioCode.equals(row.get("scenario_code")) && apiName.equals(row.get("api_name"))) {
                out.put(row.get("validation_key"), row.get("expected_value"));
            }
        }
        return out;
    }

    /**
     * Returns the first row matching the given scenarioCode (used by
     * ${CSV:file:col} placeholder substitution).
     */
    public Map<String, String> firstRow(String fileName, String scenarioCode) {
        for (Map<String, String> row : load(fileName)) {
            if (scenarioCode == null || scenarioCode.equals(row.get("scenario_code"))) {
                return row;
            }
        }
        return Collections.emptyMap();
    }

    private List<Map<String, String>> load(String fileName) {
        return cache.computeIfAbsent(fileName, this::readFile);
    }

    private List<Map<String, String>> readFile(String fileName) {
        Resource res = new ClassPathResource("expected/" + fileName);
        if (!res.exists()) {
            log.warn("Expected-results CSV not found on classpath: expected/{}", fileName);
            return Collections.emptyList();
        }
        try (Reader r = new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8);
             CSVReaderHeaderAware csv = new CSVReaderHeaderAware(r)) {
            List<Map<String, String>> rows = new ArrayList<>();
            Map<String, String> next;
            while ((next = csv.readMap()) != null) rows.add(next);
            return rows;
        } catch (Exception ex) {
            log.error("Failed to read CSV {}: {}", fileName, ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }
}

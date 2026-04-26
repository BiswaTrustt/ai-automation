package com.example.apiframework.journey.csv;

import com.example.apiframework.journey.dto.ApiStepResult;
import com.example.apiframework.journey.dto.JourneyResult;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CsvResultWriter {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Path REPORTS_DIR = Paths.get("reports");

    public Path write(JourneyResult result) {
        try {
            Files.createDirectories(REPORTS_DIR);
            String name = "results-" + result.getModuleCode() + "-" + result.getScenarioCode()
                    + "-" + result.getStartedAt().format(TS) + ".csv";
            Path path = REPORTS_DIR.resolve(name);
            try (CSVWriter w = new CSVWriter(new FileWriter(path.toFile()))) {
                w.writeNext(new String[]{"order","api_name","status","http_status","duration_ms","error_message"});
                for (ApiStepResult s : result.getSteps()) {
                    w.writeNext(new String[]{
                            String.valueOf(s.getExecutionOrder()),
                            s.getApiName(),
                            s.getStatus().name(),
                            String.valueOf(s.getHttpStatusCode()),
                            String.valueOf(s.getExecutionTimeMs()),
                            s.getErrorMessage() == null ? "" : s.getErrorMessage()
                    });
                }
            }
            log.info("CSV result written: {}", path.toAbsolutePath());
            return path;
        } catch (Exception ex) {
            log.error("Failed to write CSV result: {}", ex.getMessage(), ex);
            return null;
        }
    }
}

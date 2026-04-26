package com.example.apiframework.journey.service;

import com.example.apiframework.dto.ApiMetadataDto;
import com.example.apiframework.entity.ApiMaster;
import com.example.apiframework.journey.csv.CsvResultWriter;
import com.example.apiframework.journey.dto.ApiStepResult;
import com.example.apiframework.journey.dto.JourneyContext;
import com.example.apiframework.journey.dto.JourneyResult;
import com.example.apiframework.journey.dto.ValidationOutcome;
import com.example.apiframework.journey.entity.JourneyExecutionHistory;
import com.example.apiframework.journey.entity.TestScenarioMaster;
import com.example.apiframework.journey.enums.JourneyStatus;
import com.example.apiframework.journey.repository.JourneyExecutionHistoryRepository;
import com.example.apiframework.service.GenericApiExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JourneyExecutionService {

    private final ScenarioService scenarioService;
    private final ModuleService moduleService;
    private final PreSqlExecutionService preSqlService;
    private final RequestBuilderService requestBuilder;
    private final GenericApiExecutor apiExecutor;
    private final ResponseMappingService responseMapping;
    private final PostValidationService postValidation;
    private final CsvValidationService csvValidation;
    private final JourneyExecutionHistoryRepository historyRepo;
    private final CsvResultWriter csvWriter;

    public JourneyResult execute(String moduleCode, String scenarioCode) {
        log.info("=== Journey start | module={} scenario={} ===", moduleCode, scenarioCode);
        LocalDateTime startedAt = LocalDateTime.now();

        TestScenarioMaster scenario = scenarioService.require(scenarioCode);
        JourneyContext ctx = new JourneyContext(moduleCode, scenarioCode);

        List<ModuleService.OrderedApi> apis = moduleService.orderedApisFor(moduleCode, scenario.getId());
        if (apis.isEmpty()) {
            log.warn("No APIs mapped for module={} scenario={}", moduleCode, scenarioCode);
        }

        List<ApiStepResult> steps = new ArrayList<>();
        boolean anyFail = false;

        for (ModuleService.OrderedApi entry : apis) {
            ApiStepResult step = runStep(scenario.getId(), entry.api(), entry.executionOrder(), ctx);
            steps.add(step);
            persistHistory(ctx, moduleCode, scenarioCode, step);
            if (step.getStatus() != JourneyStatus.PASS) anyFail = true;
        }

        LocalDateTime finishedAt = LocalDateTime.now();
        JourneyResult result = JourneyResult.builder()
                .journeyRunId(ctx.getJourneyRunId())
                .moduleCode(moduleCode)
                .scenarioCode(scenarioCode)
                .status(anyFail ? JourneyStatus.FAIL : JourneyStatus.PASS)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .totalDurationMs(Duration.between(startedAt, finishedAt).toMillis())
                .steps(steps)
                .build();

        csvWriter.write(result);
        log.info("=== Journey done | runId={} status={} duration={}ms ===",
                result.getJourneyRunId(), result.getStatus(), result.getTotalDurationMs());
        return result;
    }

    private ApiStepResult runStep(Long scenarioId, ApiMaster api, int order, JourneyContext ctx) {
        long t0 = System.currentTimeMillis();
        try {
            preSqlService.runPreSql(scenarioId, api.getApiId(), ctx);

            ApiMetadataDto metadata = requestBuilder.build(api, ctx);
            GenericApiExecutor.ExecutionOutcome outcome = apiExecutor.execute(metadata, Map.of());

            responseMapping.capture(ctx, api.getApiName(), outcome.getResponseBody());

            List<ValidationOutcome> dbOutcomes = postValidation.validate(scenarioId, api.getApiId(), ctx);
            Map<String, String> dbValuesMap = new LinkedHashMap<>();
            dbOutcomes.forEach(v -> dbValuesMap.put(v.getValidationKey(), v.getActualValue()));

            List<ValidationOutcome> csvOutcomes = csvValidation.validate(
                    ctx.getScenarioCode(), api.getApiName(), outcome.getResponseBody(), dbValuesMap);

            boolean httpOk = outcome.getStatusCode() >= 200 && outcome.getStatusCode() < 400;
            boolean dbOk  = dbOutcomes.stream().allMatch(ValidationOutcome::isPassed);
            boolean csvOk = csvOutcomes.stream().allMatch(ValidationOutcome::isPassed);
            JourneyStatus status = (httpOk && dbOk && csvOk) ? JourneyStatus.PASS : JourneyStatus.FAIL;

            return ApiStepResult.builder()
                    .apiName(api.getApiName())
                    .executionOrder(order)
                    .status(status)
                    .httpStatusCode(outcome.getStatusCode())
                    .executionTimeMs(System.currentTimeMillis() - t0)
                    .requestPayload(metadata.getResolvedRequestBody())
                    .responsePayload(outcome.getResponseBody())
                    .dbValidations(dbOutcomes)
                    .csvValidations(csvOutcomes)
                    .errorMessage(outcome.getErrorMessage())
                    .build();

        } catch (Exception ex) {
            log.error("Step '{}' errored: {}", api.getApiName(), ex.getMessage(), ex);
            return ApiStepResult.builder()
                    .apiName(api.getApiName())
                    .executionOrder(order)
                    .status(JourneyStatus.ERROR)
                    .executionTimeMs(System.currentTimeMillis() - t0)
                    .dbValidations(List.of())
                    .csvValidations(List.of())
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    private void persistHistory(JourneyContext ctx, String moduleCode, String scenarioCode, ApiStepResult s) {
        historyRepo.save(JourneyExecutionHistory.builder()
                .journeyRunId(ctx.getJourneyRunId())
                .moduleCode(moduleCode)
                .scenarioCode(scenarioCode)
                .apiName(s.getApiName())
                .executionOrder(s.getExecutionOrder())
                .status(s.getStatus().name())
                .httpStatusCode(s.getHttpStatusCode())
                .requestPayload(s.getRequestPayload())
                .responsePayload(s.getResponsePayload())
                .dbValidationResult(stringify(s.getDbValidations()))
                .csvValidationResult(stringify(s.getCsvValidations()))
                .executionTimeMs(s.getExecutionTimeMs())
                .errorMessage(s.getErrorMessage())
                .build());
    }

    private String stringify(List<ValidationOutcome> outcomes) {
        if (outcomes == null || outcomes.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (ValidationOutcome v : outcomes) {
            sb.append(v.getValidationKey()).append('=').append(v.getActualValue())
              .append(" (expected=").append(v.getExpectedValue())
              .append(", ").append(v.isPassed() ? "PASS" : "FAIL").append("); ");
        }
        return sb.toString();
    }
}

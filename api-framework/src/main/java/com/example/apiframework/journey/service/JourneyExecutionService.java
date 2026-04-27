package com.example.apiframework.journey.service;

import com.example.apiframework.dto.ApiMetadataDto;
import com.example.apiframework.entity.ApiMaster;
import com.example.apiframework.journey.csv.CsvResultWriter;
import com.example.apiframework.journey.dto.ApiStepResult;
import com.example.apiframework.journey.dto.JourneyContext;
import com.example.apiframework.journey.dto.JourneyResult;
import com.example.apiframework.journey.dto.ValidationOutcome;
import com.example.apiframework.journey.entity.JourneyExecutionHistory;
import com.example.apiframework.journey.entity.LoanProductMaster;
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

    private final ProductService productService;
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

    /** Backward-compatible 2-arg entry point. */
    public JourneyResult execute(String moduleCode, String scenarioCode) {
        return execute(null, null, moduleCode, null, scenarioCode);
    }

    /** Product-aware 3-arg entry point. */
    public JourneyResult execute(String productCode, String moduleCode, String scenarioCode) {
        return execute(null, productCode, moduleCode, null, scenarioCode);
    }

    /**
     * Full entry point — env (selects environment_master.base_url at runtime),
     * product (filters api_scenario_mapping rows), members (placeholder for
     * {@code ${MEMBERS}} in templates).
     */
    public JourneyResult execute(String envCode,
                                 String productCode,
                                 String moduleCode,
                                 Integer memberCount,
                                 String scenarioCode) {
        log.info("=== Journey start | env={} product={} module={} members={} scenario={} ===",
                envCode, productCode, moduleCode, memberCount, scenarioCode);
        LocalDateTime startedAt = LocalDateTime.now();

        Long productId = null;
        if (productCode != null && !productCode.isBlank()) {
            LoanProductMaster product = productService.require(productCode);
            productId = product.getId();
            productService.requireProductModule(productId, productCode, moduleCode);
        }

        TestScenarioMaster scenario = scenarioService.require(scenarioCode);
        JourneyContext ctx = new JourneyContext(envCode, productCode, moduleCode, memberCount, scenarioCode);

        List<ModuleService.OrderedApi> apis =
                moduleService.orderedApisFor(moduleCode, scenario.getId(), productId);

        if (apis.isEmpty()) {
            log.warn("No APIs mapped for product={} module={} scenario={}",
                    productCode, moduleCode, scenarioCode);
        }

        List<ApiStepResult> steps = new ArrayList<>();
        boolean anyFail = false;
        for (ModuleService.OrderedApi entry : apis) {
            int loops = Math.max(1, entry.loopCount());
            for (int i = 1; i <= loops; i++) {
                ctx.setCurrentIteration(i);
                if (entry.delayMs() > 0) {
                    try { Thread.sleep(entry.delayMs()); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
                ApiStepResult step = runStep(scenario.getId(), entry, i, loops, ctx);
                steps.add(step);
                persistHistory(ctx, step);
                if (step.getStatus() != JourneyStatus.PASS) anyFail = true;
            }
        }

        LocalDateTime finishedAt = LocalDateTime.now();
        JourneyResult result = JourneyResult.builder()
                .journeyRunId(ctx.getJourneyRunId())
                .loanProductCode(productCode)
                .moduleCode(moduleCode)
                .scenarioCode(scenarioCode)
                .status(anyFail ? JourneyStatus.FAIL : JourneyStatus.PASS)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .totalDurationMs(Duration.between(startedAt, finishedAt).toMillis())
                .steps(steps)
                .build();

        csvWriter.write(result);
        log.info("=== Journey done | runId={} status={} duration={}ms steps={} ===",
                result.getJourneyRunId(), result.getStatus(),
                result.getTotalDurationMs(), steps.size());
        return result;
    }

    private ApiStepResult runStep(Long scenarioId,
                                  ModuleService.OrderedApi entry,
                                  int iteration,
                                  int totalLoops,
                                  JourneyContext ctx) {
        ApiMaster api = entry.api();
        long t0 = System.currentTimeMillis();
        try {
            preSqlService.runPreSql(scenarioId, api.getApiId(), ctx);

            ApiMetadataDto metadata = requestBuilder.build(api, ctx);
            GenericApiExecutor.ExecutionOutcome outcome = apiExecutor.execute(metadata, Map.of());

            responseMapping.capture(ctx, api.getApiName(), outcome.getResponseBody());
            responseMapping.applyExtractions(ctx, outcome.getResponseBody(), entry.extractionMappings());

            List<ValidationOutcome> dbOutcomes = postValidation.validate(scenarioId, api.getApiId(), ctx);
            Map<String, String> dbValuesMap = new LinkedHashMap<>();
            dbOutcomes.forEach(v -> dbValuesMap.put(v.getValidationKey(), v.getActualValue()));

            List<ValidationOutcome> csvOutcomes = csvValidation.validate(
                    ctx.getScenarioCode(), api.getApiName(), outcome.getResponseBody(), dbValuesMap);

            boolean httpOk = outcome.getStatusCode() >= 200 && outcome.getStatusCode() < 400;
            boolean dbOk  = dbOutcomes.stream().allMatch(ValidationOutcome::isPassed);
            boolean csvOk = csvOutcomes.stream().allMatch(ValidationOutcome::isPassed);
            JourneyStatus status = (httpOk && dbOk && csvOk) ? JourneyStatus.PASS : JourneyStatus.FAIL;

            String label = totalLoops > 1
                    ? api.getApiName() + " [" + iteration + "/" + totalLoops + "]"
                    : api.getApiName();

            return ApiStepResult.builder()
                    .apiName(label)
                    .executionOrder(entry.executionOrder())
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
            log.error("Step '{}' iteration {} errored: {}", api.getApiName(), iteration, ex.getMessage(), ex);
            return ApiStepResult.builder()
                    .apiName(api.getApiName())
                    .executionOrder(entry.executionOrder())
                    .status(JourneyStatus.ERROR)
                    .executionTimeMs(System.currentTimeMillis() - t0)
                    .dbValidations(List.of())
                    .csvValidations(List.of())
                    .errorMessage(ex.getMessage())
                    .build();
        }
    }

    private void persistHistory(JourneyContext ctx, ApiStepResult s) {
        historyRepo.save(JourneyExecutionHistory.builder()
                .journeyRunId(ctx.getJourneyRunId())
                .loanProductCode(ctx.getLoanProductCode())
                .moduleCode(ctx.getModuleCode())
                .scenarioCode(ctx.getScenarioCode())
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

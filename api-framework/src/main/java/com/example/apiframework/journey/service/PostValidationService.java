package com.example.apiframework.journey.service;

import com.example.apiframework.journey.dto.JourneyContext;
import com.example.apiframework.journey.dto.ValidationOutcome;
import com.example.apiframework.journey.entity.ApiExpectedResult;
import com.example.apiframework.journey.entity.ApiPostValidationSql;
import com.example.apiframework.journey.repository.ApiExpectedResultRepository;
import com.example.apiframework.journey.repository.ApiPostValidationSqlRepository;
import com.example.apiframework.journey.util.DynamicValueResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Runs post-validation SQL queries, fetches the configured expected_column,
 * and compares against the corresponding api_expected_results entry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostValidationService {

    private final ApiPostValidationSqlRepository postRepo;
    private final ApiExpectedResultRepository expectedRepo;
    private final JdbcTemplate jdbc;
    private final DynamicValueResolver resolver;

    @Transactional(readOnly = true)
    public List<ValidationOutcome> validate(Long scenarioId, Long apiId, JourneyContext ctx) {
        List<ValidationOutcome> outcomes = new ArrayList<>();
        for (ApiPostValidationSql q : postRepo.findByScenarioIdAndApiIdAndActiveTrue(scenarioId, apiId)) {
            String sql = resolver.resolve(q.getSqlQuery(), ctx);
            log.debug("[post-SQL] {}", sql);

            String actual = fetchSingleColumn(sql, q.getExpectedColumn());
            Optional<ApiExpectedResult> expected = expectedRepo
                    .findByScenarioIdAndApiIdAndValidationKey(scenarioId, apiId, q.getValidationName());

            String expectedValue = expected.map(ApiExpectedResult::getExpectedValue).orElse(null);
            boolean passed = expectedValue == null
                    ? actual != null
                    : Objects.equals(expectedValue, actual);

            outcomes.add(ValidationOutcome.builder()
                    .validationKey(q.getValidationName())
                    .expectedValue(expectedValue)
                    .actualValue(actual)
                    .passed(passed)
                    .source("DB")
                    .build());
        }
        return outcomes;
    }

    private String fetchSingleColumn(String sql, String column) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            if (rows.isEmpty()) return null;
            Object v = rows.get(0).get(column);
            return v == null ? null : v.toString();
        } catch (Exception ex) {
            log.warn("Post-validation SQL failed: {}", ex.getMessage());
            return null;
        }
    }
}

package com.example.apiframework.journey.service;

import com.example.apiframework.journey.dto.JourneyContext;
import com.example.apiframework.journey.entity.ApiPreSql;
import com.example.apiframework.journey.repository.ApiPreSqlRepository;
import com.example.apiframework.journey.util.DynamicValueResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreSqlExecutionService {

    private final ApiPreSqlRepository repo;
    private final JdbcTemplate jdbc;
    private final DynamicValueResolver resolver;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runPreSql(Long scenarioId, Long apiId, JourneyContext ctx) {
        for (ApiPreSql row : repo.findByScenarioIdAndApiIdAndActiveTrueOrderBySqlOrderAsc(scenarioId, apiId)) {
            String sql = resolver.resolve(row.getSqlQuery(), ctx);
            log.debug("[pre-SQL #{}] {}", row.getSqlOrder(), sql);
            jdbc.execute(sql);
        }
    }
}

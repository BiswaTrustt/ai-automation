package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.ApiPreSql;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiPreSqlRepository extends JpaRepository<ApiPreSql, Long> {
    List<ApiPreSql> findByScenarioIdAndApiIdAndActiveTrueOrderBySqlOrderAsc(Long scenarioId, Long apiId);
}

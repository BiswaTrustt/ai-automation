package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.ApiPostValidationSql;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiPostValidationSqlRepository extends JpaRepository<ApiPostValidationSql, Long> {
    List<ApiPostValidationSql> findByScenarioIdAndApiIdAndActiveTrue(Long scenarioId, Long apiId);
}

package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.ApiExpectedResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiExpectedResultRepository extends JpaRepository<ApiExpectedResult, Long> {
    List<ApiExpectedResult> findByScenarioIdAndApiId(Long scenarioId, Long apiId);

    Optional<ApiExpectedResult> findByScenarioIdAndApiIdAndValidationKey(Long scenarioId, Long apiId, String validationKey);
}

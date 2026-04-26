package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.ApiScenarioMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiScenarioMappingRepository extends JpaRepository<ApiScenarioMapping, Long> {
    List<ApiScenarioMapping> findByScenarioIdAndActiveTrueOrderByExecutionOrderAsc(Long scenarioId);
}

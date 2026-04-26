package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.ApiScenarioMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApiScenarioMappingRepository extends JpaRepository<ApiScenarioMapping, Long> {

    /**
     * Legacy lookup: rows that have no product binding (product_id IS NULL).
     * Drives the 2-arg journeyExecutor.execute(module, scenario) flow.
     */
    @Query("""
            SELECT m FROM ApiScenarioMapping m
            WHERE m.scenarioId = :scenarioId
              AND m.active = true
              AND m.productId IS NULL
            ORDER BY m.executionOrder ASC
            """)
    List<ApiScenarioMapping> findActiveForScenarioNoProduct(@Param("scenarioId") Long scenarioId);

    /**
     * Product-aware lookup: rows whose product_id matches the supplied id.
     * Drives the 3-arg journeyExecutor.execute(product, module, scenario) flow.
     */
    @Query("""
            SELECT m FROM ApiScenarioMapping m
            WHERE m.scenarioId = :scenarioId
              AND m.productId  = :productId
              AND m.active = true
            ORDER BY m.executionOrder ASC
            """)
    List<ApiScenarioMapping> findActiveForScenarioAndProduct(@Param("scenarioId") Long scenarioId,
                                                             @Param("productId")  Long productId);

    // Kept for backward compatibility with any callers from before this change.
    List<ApiScenarioMapping> findByScenarioIdAndActiveTrueOrderByExecutionOrderAsc(Long scenarioId);
}

package com.example.apiframework.journey.repository;

import com.example.apiframework.journey.entity.TestScenarioMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestScenarioMasterRepository extends JpaRepository<TestScenarioMaster, Long> {
    Optional<TestScenarioMaster> findByScenarioCodeAndActiveTrue(String scenarioCode);

    List<TestScenarioMaster> findAllByActiveTrueOrderByScenarioCodeAsc();

    List<TestScenarioMaster> findAllByModuleCodeAndActiveTrueOrderByScenarioCodeAsc(String moduleCode);
}

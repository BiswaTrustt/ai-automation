package com.example.apiframework.journey.service;

import com.example.apiframework.journey.entity.TestScenarioMaster;
import com.example.apiframework.journey.repository.TestScenarioMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final TestScenarioMasterRepository repo;

    public TestScenarioMaster require(String scenarioCode) {
        return repo.findByScenarioCodeAndActiveTrue(scenarioCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active scenario not found: " + scenarioCode));
    }
}

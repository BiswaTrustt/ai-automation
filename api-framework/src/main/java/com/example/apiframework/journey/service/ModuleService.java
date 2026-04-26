package com.example.apiframework.journey.service;

import com.example.apiframework.entity.ApiMaster;
import com.example.apiframework.repository.ApiMasterRepository;
import com.example.apiframework.journey.entity.ApiScenarioMapping;
import com.example.apiframework.journey.repository.ApiScenarioMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ApiScenarioMappingRepository mappingRepo;
    private final ApiMasterRepository apiRepo;

    /**
     * Returns the ordered list of (mapping, api_master) pairs for a given
     * module + scenario, restricted to APIs whose module_name == moduleCode.
     * The module filter guards against the same scenario_code being reused
     * across modules.
     */
    public List<OrderedApi> orderedApisFor(String moduleCode, Long scenarioId) {
        List<ApiScenarioMapping> mappings = mappingRepo
                .findByScenarioIdAndActiveTrueOrderByExecutionOrderAsc(scenarioId);

        if (mappings.isEmpty()) return List.of();

        Map<Long, ApiMaster> apisById = new HashMap<>();
        apiRepo.findAllById(mappings.stream().map(ApiScenarioMapping::getApiId).toList())
                .forEach(a -> apisById.put(a.getApiId(), a));

        List<OrderedApi> out = new ArrayList<>();
        for (ApiScenarioMapping m : mappings) {
            ApiMaster api = apisById.get(m.getApiId());
            if (api == null || !Boolean.TRUE.equals(api.getActive())) continue;
            if (moduleCode != null && !moduleCode.equalsIgnoreCase(api.getModuleName())) continue;
            out.add(new OrderedApi(api, m.getExecutionOrder()));
        }
        return out;
    }

    public record OrderedApi(ApiMaster api, int executionOrder) {}
}

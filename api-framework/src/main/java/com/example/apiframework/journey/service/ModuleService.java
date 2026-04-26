package com.example.apiframework.journey.service;

import com.example.apiframework.entity.ApiMaster;
import com.example.apiframework.repository.ApiMasterRepository;
import com.example.apiframework.journey.entity.ApiScenarioMapping;
import com.example.apiframework.journey.entity.TestScenarioMaster;
import com.example.apiframework.journey.repository.ApiScenarioMappingRepository;
import com.example.apiframework.journey.repository.TestScenarioMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ApiScenarioMappingRepository mappingRepo;
    private final ApiMasterRepository apiRepo;
    private final TestScenarioMasterRepository scenarioRepo;

    /**
     * Returns the ordered list of (mapping, api_master) pairs.
     *
     * <ul>
     *   <li>If {@code productId} is null, picks legacy rows where product_id IS NULL.</li>
     *   <li>Otherwise picks rows whose product_id matches.</li>
     *   <li>The module filter is skipped when the scenario is flagged
     *       {@code cross_module} (used by JMX-style end-to-end flows).</li>
     * </ul>
     */
    public List<OrderedApi> orderedApisFor(String moduleCode, Long scenarioId, Long productId) {
        List<ApiScenarioMapping> mappings = (productId == null)
                ? mappingRepo.findActiveForScenarioNoProduct(scenarioId)
                : mappingRepo.findActiveForScenarioAndProduct(scenarioId, productId);

        if (mappings.isEmpty()) return List.of();

        boolean crossModule = scenarioRepo.findById(scenarioId)
                .map(TestScenarioMaster::getCrossModule)
                .orElse(Boolean.FALSE);

        Map<Long, ApiMaster> apisById = new HashMap<>();
        apiRepo.findAllById(mappings.stream().map(ApiScenarioMapping::getApiId).toList())
                .forEach(a -> apisById.put(a.getApiId(), a));

        List<OrderedApi> out = new ArrayList<>();
        for (ApiScenarioMapping m : mappings) {
            ApiMaster api = apisById.get(m.getApiId());
            if (api == null || !Boolean.TRUE.equals(api.getActive())) continue;
            if (!crossModule && moduleCode != null
                    && !moduleCode.equalsIgnoreCase(api.getModuleName())) continue;
            out.add(new OrderedApi(api, m));
        }
        return out;
    }

    /** Carries both the API definition and its mapping (for loop_count / extraction_mappings / delay_ms). */
    public record OrderedApi(ApiMaster api, ApiScenarioMapping mapping) {
        public int executionOrder() { return mapping.getExecutionOrder(); }
        public int loopCount()      { return mapping.getLoopCount() == null ? 1 : mapping.getLoopCount(); }
        public int delayMs()        { return mapping.getDelayMs()   == null ? 0 : mapping.getDelayMs();   }
        public String extractionMappings() { return mapping.getExtractionMappings(); }
    }
}

package com.example.apiframework.journey.controller;

import com.example.apiframework.journey.entity.LoanProductMaster;
import com.example.apiframework.journey.entity.ProductModuleMapping;
import com.example.apiframework.journey.entity.TestScenarioMaster;
import com.example.apiframework.journey.repository.LoanProductMasterRepository;
import com.example.apiframework.journey.repository.ProductModuleMappingRepository;
import com.example.apiframework.journey.repository.TestScenarioMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Read-only lookup endpoints used by the static HTML UI to populate
 * cascading dropdowns: products → modules → scenarios.
 */
@RestController
@RequestMapping("/journey/lookup")
@RequiredArgsConstructor
public class JourneyLookupController {

    private final LoanProductMasterRepository productRepo;
    private final ProductModuleMappingRepository moduleRepo;
    private final TestScenarioMasterRepository scenarioRepo;

    @GetMapping("/products")
    public List<Option> products() {
        return productRepo.findAllByActiveTrueOrderByProductCodeAsc().stream()
                .map(p -> new Option(p.getProductCode(), p.getProductName()))
                .toList();
    }

    /**
     * Modules available for a product. When productCode is omitted, returns
     * the union of every distinct module across all products (legacy mode).
     */
    @GetMapping("/modules")
    public List<Option> modules(@RequestParam(required = false) String productCode) {
        List<ProductModuleMapping> rows;
        if (productCode == null || productCode.isBlank()) {
            rows = moduleRepo.findAllByActiveTrueOrderBySequenceNoAsc();
        } else {
            LoanProductMaster product = productRepo
                    .findByProductCodeAndActiveTrue(productCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown product: " + productCode));
            rows = moduleRepo.findAllByProductIdAndActiveTrueOrderBySequenceNoAsc(product.getId());
        }
        // dedupe by moduleCode preserving order
        Map<String, Option> dedup = new LinkedHashMap<>();
        for (ProductModuleMapping m : rows) {
            dedup.putIfAbsent(m.getModuleCode(),
                    new Option(m.getModuleCode(),
                            m.getModuleName() == null ? m.getModuleCode() : m.getModuleName()));
        }
        return new ArrayList<>(dedup.values());
    }

    /**
     * Scenarios for a module (or all scenarios if moduleCode omitted).
     */
    @GetMapping("/scenarios")
    public List<Option> scenarios(@RequestParam(required = false) String moduleCode) {
        List<TestScenarioMaster> rows = (moduleCode == null || moduleCode.isBlank())
                ? scenarioRepo.findAllByActiveTrueOrderByScenarioCodeAsc()
                : scenarioRepo.findAllByModuleCodeAndActiveTrueOrderByScenarioCodeAsc(moduleCode);
        return rows.stream()
                .map(s -> new Option(s.getScenarioCode(), s.getScenarioName()))
                .toList();
    }

    public record Option(String code, String name) {}
}

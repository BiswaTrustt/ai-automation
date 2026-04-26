package com.example.apiframework.journey.controller;

import com.example.apiframework.journey.dto.JourneyResult;
import com.example.apiframework.journey.executor.JourneyExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/journey")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyExecutor executor;

    /**
     * Trigger a journey.
     * <ul>
     *   <li>{@code product} is optional — when omitted, runs the legacy 2-arg flow.</li>
     *   <li>When supplied, runs the product-aware flow filtered by loan product.</li>
     * </ul>
     */
    @PostMapping("/execute")
    public JourneyResult execute(@RequestParam(required = false) String product,
                                 @RequestParam String module,
                                 @RequestParam String scenario) {
        return (product == null || product.isBlank())
                ? executor.execute(module, scenario)
                : executor.execute(product, module, scenario);
    }
}

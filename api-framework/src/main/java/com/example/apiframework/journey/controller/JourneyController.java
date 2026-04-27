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
     *   <li>{@code env} (optional) — environment code from environment_master (e.g. qa2). When set,
     *       the run resolves base URLs from environment_master instead of api_master.base_url.</li>
     *   <li>{@code product} (optional) — when omitted, runs the legacy flow (scenario rows where
     *       product_id IS NULL).</li>
     *   <li>{@code members} (optional) — exposed to templates as {@code ${MEMBERS}}; does not yet
     *       multiply per-customer step loops.</li>
     * </ul>
     */
    @PostMapping("/execute")
    public JourneyResult execute(@RequestParam(required = false) String env,
                                 @RequestParam(required = false) String product,
                                 @RequestParam String module,
                                 @RequestParam(required = false) Integer members,
                                 @RequestParam String scenario) {
        return executor.execute(env, product, module, members, scenario);
    }
}

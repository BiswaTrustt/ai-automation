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

    @PostMapping("/execute")
    public JourneyResult execute(@RequestParam String module, @RequestParam String scenario) {
        return executor.execute(module, scenario);
    }
}

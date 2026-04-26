package com.example.apiframework.journey.service;

import com.example.apiframework.journey.dto.JourneyContext;
import org.springframework.stereotype.Service;

@Service
public class ResponseMappingService {

    public void capture(JourneyContext ctx, String apiName, String responseBody) {
        ctx.recordResponse(apiName, responseBody);
    }
}

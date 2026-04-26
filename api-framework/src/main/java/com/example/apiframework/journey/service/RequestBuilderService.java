package com.example.apiframework.journey.service;

import com.example.apiframework.dto.ApiMetadataDto;
import com.example.apiframework.entity.ApiHeader;
import com.example.apiframework.entity.ApiMaster;
import com.example.apiframework.entity.ApiRequestTemplate;
import com.example.apiframework.repository.ApiMasterRepository;
import com.example.apiframework.journey.dto.JourneyContext;
import com.example.apiframework.journey.util.DynamicValueResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RequestBuilderService {

    private final ApiMasterRepository apiRepo;
    private final DynamicValueResolver resolver;

    /**
     * Build an ApiMetadataDto with placeholders fully resolved against the
     * current JourneyContext.
     */
    @Transactional(readOnly = true)
    public ApiMetadataDto build(ApiMaster bareApi, JourneyContext ctx) {
        // Re-load with collections inside this tx so lazy fields are populated.
        ApiMaster api = apiRepo.findById(bareApi.getApiId()).orElseThrow();

        Map<String, String> headers = new LinkedHashMap<>();
        api.getHeaders().stream()
                .sorted((a, b) -> Integer.compare(
                        a.getSequenceNo() == null ? 0 : a.getSequenceNo(),
                        b.getSequenceNo() == null ? 0 : b.getSequenceNo()))
                .forEach((ApiHeader h) -> headers.put(
                        h.getHeaderKey(),
                        resolver.resolve(h.getHeaderValue(), ctx)));

        String body = api.getRequestTemplates().stream()
                .findFirst()
                .map(ApiRequestTemplate::getRequestTemplate)
                .map(t -> resolver.resolve(t, ctx))
                .orElse(null);

        return ApiMetadataDto.builder()
                .apiId(api.getApiId())
                .apiName(api.getApiName())
                .moduleName(api.getModuleName())
                .baseUrl(api.getBaseUrl())
                .endpoint(api.getEndpoint())
                .httpMethod(api.getHttpMethod())
                .authType(api.getAuthType())
                .contentType(api.getContentType())
                .resolvedHeaders(headers)
                .resolvedRequestBody(body)
                .validationRules(Collections.emptyList())
                .build();
    }
}

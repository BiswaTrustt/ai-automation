package com.example.apiframework.journey.service;

import com.example.apiframework.dto.ApiMetadataDto;
import com.example.apiframework.entity.ApiHeader;
import com.example.apiframework.entity.ApiMaster;
import com.example.apiframework.entity.ApiRequestTemplate;
import com.example.apiframework.repository.ApiMasterRepository;
import com.example.apiframework.journey.dto.JourneyContext;
import com.example.apiframework.journey.entity.EnvironmentMaster;
import com.example.apiframework.journey.repository.EnvironmentMasterRepository;
import com.example.apiframework.journey.util.DynamicValueResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestBuilderService {

    private final ApiMasterRepository apiRepo;
    private final EnvironmentMasterRepository envRepo;
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

        String baseUrl = resolveBaseUrl(api.getBaseUrl(), ctx);

        return ApiMetadataDto.builder()
                .apiId(api.getApiId())
                .apiName(api.getApiName())
                .moduleName(api.getModuleName())
                .baseUrl(baseUrl)
                .endpoint(api.getEndpoint())
                .httpMethod(api.getHttpMethod())
                .authType(api.getAuthType())
                .contentType(api.getContentType())
                .resolvedHeaders(headers)
                .resolvedRequestBody(body)
                .validationRules(Collections.emptyList())
                .build();
    }

    /**
     * If the run specifies an environment code, use that environment's base_url.
     * Otherwise fall back to the api_master row's base_url (legacy behaviour).
     * Unknown env codes are logged and the row's base_url is used so a typo
     * doesn't silently route nowhere.
     */
    private String resolveBaseUrl(String fallback, JourneyContext ctx) {
        String envCode = ctx == null ? null : ctx.getEnvCode();
        if (envCode == null || envCode.isBlank()) return fallback;
        return envRepo.findByEnvCodeAndActiveTrue(envCode)
                .map(EnvironmentMaster::getBaseUrl)
                .orElseGet(() -> {
                    log.warn("Unknown environment code '{}' — falling back to api_master.base_url '{}'",
                            envCode, fallback);
                    return fallback;
                });
    }
}

package com.example.apiframework.service;

import com.example.apiframework.dto.ApiMetadataDto;
import com.example.apiframework.dto.ValidationRuleDto;
import com.example.apiframework.entity.ApiMaster;
import com.example.apiframework.entity.ApiRequestTemplate;
import com.example.apiframework.exception.ApiNotFoundException;
import com.example.apiframework.repository.ApiMasterRepository;
import com.example.apiframework.util.HeaderBuilder;
import com.example.apiframework.util.PlaceholderResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for assembling a complete {@link ApiMetadataDto}
 * from the PostgreSQL metadata tables.
 *
 * <p>All database reads are performed within a single read-only transaction
 * to guarantee a consistent view of the metadata, even if concurrent
 * onboarding operations are in progress.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiMetadataService {

    private final ApiMasterRepository apiMasterRepository;
    private final HeaderBuilder headerBuilder;
    private final PlaceholderResolver placeholderResolver;

    /**
     * Loads and assembles all metadata required to execute the named API.
     *
     * <p>Three separate queries are issued (headers, templates, validations)
     * so that each collection is fully initialised before the transaction ends.</p>
     *
     * @param apiName       the logical name identifying the API in {@code api_master}
     * @param dynamicValues caller-supplied placeholder values (e.g. CUSTOMER_ID)
     * @return a fully resolved {@link ApiMetadataDto}
     * @throws ApiNotFoundException if no active API is registered with the given name
     */
    public ApiMetadataDto loadMetadata(String apiName, Map<String, String> dynamicValues) {
        log.info("Loading metadata for API: '{}'", apiName);

        // Load the master record + each child collection (separate fetch joins)
        ApiMaster master = apiMasterRepository
                .findByApiNameAndActiveTrue(apiName)
                .orElseThrow(() -> new ApiNotFoundException(apiName));

        // Load headers (triggers JOIN FETCH)
        ApiMaster withHeaders = apiMasterRepository
                .findWithHeadersByApiName(apiName)
                .orElseThrow(() -> new ApiNotFoundException(apiName));

        // Load templates (triggers JOIN FETCH)
        ApiMaster withTemplates = apiMasterRepository
                .findWithTemplatesByApiName(apiName)
                .orElseThrow(() -> new ApiNotFoundException(apiName));

        // Load validations (triggers JOIN FETCH)
        ApiMaster withValidations = apiMasterRepository
                .findWithValidationsByApiName(apiName)
                .orElseThrow(() -> new ApiNotFoundException(apiName));

        // Resolve headers
        Map<String, String> resolvedHeaders = headerBuilder.build(
                withHeaders.getHeaders(), dynamicValues);

        // Resolve request body (use first template if available)
        String resolvedBody = resolveRequestBody(withTemplates, dynamicValues);

        // Map validation entities to DTOs
        List<ValidationRuleDto> validationRules = withValidations.getValidations().stream()
                .map(v -> ValidationRuleDto.builder()
                        .validationId(v.getValidationId())
                        .jsonPath(v.getJsonPath())
                        .expectedValue(v.getExpectedValue())
                        .validationType(v.getValidationType())
                        .mandatory(Boolean.TRUE.equals(v.getMandatory()))
                        .build())
                .collect(Collectors.toList());

        ApiMetadataDto metadata = ApiMetadataDto.builder()
                .apiId(master.getApiId())
                .apiName(master.getApiName())
                .moduleName(master.getModuleName())
                .baseUrl(master.getBaseUrl())
                .endpoint(master.getEndpoint())
                .httpMethod(master.getHttpMethod())
                .authType(master.getAuthType())
                .contentType(master.getContentType())
                .resolvedHeaders(resolvedHeaders)
                .resolvedRequestBody(resolvedBody)
                .validationRules(validationRules)
                .build();

        log.info("Metadata loaded for '{}': method={}, validations={}",
                apiName, master.getHttpMethod(), validationRules.size());
        return metadata;
    }

    /**
     * Resolves placeholders in the first available request template.
     *
     * @param master        the API entity with templates collection initialised
     * @param dynamicValues caller-supplied placeholder overrides
     * @return the resolved JSON body string, or {@code null} if no template exists
     */
    private String resolveRequestBody(ApiMaster master, Map<String, String> dynamicValues) {
        List<ApiRequestTemplate> templates = master.getRequestTemplates();
        if (templates == null || templates.isEmpty()) {
            log.debug("No request template found for API '{}'", master.getApiName());
            return null;
        }
        String raw = templates.get(0).getRequestTemplate();
        String resolved = placeholderResolver.resolve(raw, dynamicValues);
        log.debug("Request body resolved for '{}': {}", master.getApiName(), resolved);
        return resolved;
    }
}

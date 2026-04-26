package com.example.apiframework.dto;

import com.example.apiframework.enums.AuthType;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Immutable data transfer object carrying the complete metadata needed
 * to execute one API call.
 *
 * <p>Assembled by {@link com.example.apiframework.service.ApiMetadataService}
 * and consumed by the executor and validation engine.</p>
 */
@Value
@Builder
public class ApiMetadataDto {

    /** Unique database identifier of the API. */
    Long apiId;

    /** Logical name (matches {@code api_master.api_name}). */
    String apiName;

    /** Business module the API belongs to. */
    String moduleName;

    /** Full base URL including scheme and host. */
    String baseUrl;

    /** URL path relative to {@code baseUrl}. */
    String endpoint;

    /** HTTP verb (GET, POST, PUT, PATCH, DELETE …). */
    String httpMethod;

    /** Authentication strategy to apply. */
    AuthType authType;

    /** MIME type for the Content-Type request header. */
    String contentType;

    /**
     * Ordered map of header key-value pairs with placeholders already resolved.
     * Uses {@link java.util.LinkedHashMap} to preserve insertion order.
     */
    Map<String, String> resolvedHeaders;

    /**
     * Resolved JSON request body string (placeholders substituted).
     * May be {@code null} for GET / HEAD / DELETE requests without a body.
     */
    String resolvedRequestBody;

    /** Validation rules to run after the response is received. */
    List<ValidationRuleDto> validationRules;
}

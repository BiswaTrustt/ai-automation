package com.example.apiframework.enums;

/**
 * Supported authentication strategies for API execution.
 *
 * <p>Maps directly to the {@code auth_type} column in {@code api_master}.</p>
 */
public enum AuthType {

    /** No authentication required. */
    NONE,

    /** HTTP Basic Authentication (username:password Base64-encoded). */
    BASIC,

    /** Bearer token authentication (Authorization: Bearer &lt;token&gt;). */
    BEARER,

    /** API Key authentication (sent as a header or query param). */
    API_KEY,

    /** OAuth 2.0 client-credentials or token-based flow. */
    OAUTH2
}

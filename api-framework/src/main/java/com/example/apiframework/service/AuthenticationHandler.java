package com.example.apiframework.service;

import com.example.apiframework.enums.AuthType;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Applies the correct authentication strategy to a REST Assured
 * {@link RequestSpecification} before the request is dispatched.
 *
 * <p>The {@code credentials} map carries authentication-specific values keyed
 * by the following well-known names:
 * <ul>
 *   <li>{@code BASIC_USERNAME} / {@code BASIC_PASSWORD} – for BASIC auth</li>
 *   <li>{@code BEARER_TOKEN} – for Bearer token auth</li>
 *   <li>{@code API_KEY_HEADER} / {@code API_KEY_VALUE} – for API key auth</li>
 *   <li>{@code OAUTH2_TOKEN} – for OAuth 2.0 Bearer token</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class AuthenticationHandler {

    /**
     * Mutates {@code spec} to include the appropriate authentication credentials.
     *
     * @param spec        the REST Assured request specification to configure
     * @param authType    the authentication strategy declared in the API metadata
     * @param credentials map of authentication values (may be empty for NONE)
     */
    public void apply(RequestSpecification spec,
                      AuthType authType,
                      Map<String, String> credentials) {
        switch (authType) {
            case NONE -> log.debug("No authentication applied");

            case BASIC -> {
                String username = credentials.getOrDefault("BASIC_USERNAME", "");
                String password = credentials.getOrDefault("BASIC_PASSWORD", "");
                spec.auth().preemptive().basic(username, password);
                log.debug("BASIC auth applied for user '{}'", username);
            }

            case BEARER -> {
                String token = credentials.getOrDefault("BEARER_TOKEN", "");
                spec.header("Authorization", "Bearer " + token);
                log.debug("BEARER token auth applied");
            }

            case API_KEY -> {
                String headerName = credentials.getOrDefault("API_KEY_HEADER", "X-Api-Key");
                String apiKey     = credentials.getOrDefault("API_KEY_VALUE", "");
                spec.header(headerName, apiKey);
                log.debug("API_KEY auth applied via header '{}'", headerName);
            }

            case OAUTH2 -> {
                String token = credentials.getOrDefault("OAUTH2_TOKEN", "");
                spec.auth().oauth2(token);
                log.debug("OAUTH2 token auth applied");
            }

            default -> log.warn("Unknown auth type '{}'; skipping authentication", authType);
        }
    }
}

package com.example.apiframework.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson {@link ObjectMapper} configuration.
 *
 * <p>Registers the Java 8 time module and disables features that commonly
 * cause issues in test automation contexts (e.g. failing on unknown properties).</p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates the primary {@link ObjectMapper} used throughout the framework.
     *
     * @return a configured {@link ObjectMapper} instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }
}

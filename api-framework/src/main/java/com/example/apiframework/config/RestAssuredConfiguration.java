package com.example.apiframework.config;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.PrintStream;

/**
 * Global REST Assured configuration.
 *
 * <p>Applied once at application startup. Configures connection and socket
 * timeouts, SSL relaxation for test environments, and request/response
 * logging filters that route output through SLF4J (via Logback).</p>
 */
@Slf4j
@Configuration
public class RestAssuredConfiguration {

    @Value("${api.framework.default-timeout:30000}")
    private int defaultTimeoutMs;

    /**
     * Provides the shared {@link RestAssuredConfig} used by every executor call.
     *
     * @return a fully configured {@link RestAssuredConfig} instance
     */
    @Bean
    public RestAssuredConfig restAssuredConfig() {
        return RestAssuredConfig.config()
                .httpClient(
                        HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", defaultTimeoutMs)
                                .setParam("http.socket.timeout", defaultTimeoutMs)
                                .setParam("http.connection-manager.timeout", (long) defaultTimeoutMs)
                )
                .sslConfig(
                        SSLConfig.sslConfig().relaxedHTTPSValidation()
                );
    }

    /**
     * Initialises global REST Assured defaults after the Spring context starts.
     * Sets up request/response logging filters that write to the SLF4J stream.
     */
    @PostConstruct
    public void initializeRestAssured() {
        // Route REST Assured logs through SLF4J / Logback
        PrintStream logStream = new LogbackPrintStream(log);
        RestAssured.filters(
                new RequestLoggingFilter(logStream),
                new ResponseLoggingFilter(logStream)
        );
        log.info("REST Assured initialised with timeout={}ms", defaultTimeoutMs);
    }

    /**
     * Minimal {@link PrintStream} adapter that routes REST Assured text output
     * to SLF4J at DEBUG level.
     */
    private static class LogbackPrintStream extends PrintStream {

        private final org.slf4j.Logger logger;

        LogbackPrintStream(org.slf4j.Logger logger) {
            super(System.out, true);
            this.logger = logger;
        }

        @Override
        public void println(String x) {
            if (x != null && !x.isBlank()) {
                logger.debug(x);
            }
        }

        @Override
        public void print(String s) {
            if (s != null && !s.isBlank()) {
                logger.debug(s);
            }
        }
    }
}

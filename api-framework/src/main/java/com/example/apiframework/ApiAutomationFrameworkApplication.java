package com.example.apiframework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the API Automation Framework.
 *
 * <p>This application provides a metadata-driven engine for executing REST API tests
 * without writing API-specific Java code. All API definitions are stored in PostgreSQL
 * and resolved dynamically at runtime using REST Assured.</p>
 *
 * @author API Automation Team
 * @version 1.0.0
 */
@SpringBootApplication
public class ApiAutomationFrameworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiAutomationFrameworkApplication.class, args);
    }
}

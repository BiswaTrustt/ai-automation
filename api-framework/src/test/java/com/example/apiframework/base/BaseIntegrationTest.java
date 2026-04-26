package com.example.apiframework.base;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Base class for all integration tests.
 *
 * <p>Loads the full Spring Boot context with the {@code test} profile,
 * which reads {@code application-test.yml} to point at the test database.
 * Subclasses inherit the Spring context and can {@code @Autowired} any bean.</p>
 *
 * <p><strong>Prerequisites:</strong> A PostgreSQL instance must be running and
 * the {@code api_automation_db_test} database must exist before tests run.
 * Flyway will create the schema and seed data automatically on startup.</p>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    // Shared context – extend to add common fixtures, helpers, or @BeforeAll hooks
}

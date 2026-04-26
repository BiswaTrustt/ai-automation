package com.example.apiframework.tests;

import com.example.apiframework.util.PlaceholderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlaceholderResolver}.
 * No Spring context needed – the component is tested in isolation.
 */
class PlaceholderResolverTest {

    private PlaceholderResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PlaceholderResolver();
    }

    @Test
    @DisplayName("Resolve caller-supplied placeholder")
    void testResolve_CallerSupplied() {
        String result = resolver.resolve(
                "{\"customer_id\": \"${CUSTOMER_ID}\"}",
                Map.of("CUSTOMER_ID", "501147")
        );
        assertEquals("{\"customer_id\": \"501147\"}", result);
    }

    @Test
    @DisplayName("Resolve built-in UUID placeholder")
    void testResolve_BuiltInUUID() {
        String result = resolver.resolve("${UUID}", Map.of());
        assertNotNull(result);
        assertFalse(result.contains("${UUID}"), "UUID should be replaced");
        assertEquals(32, result.length(), "UUID without hyphens is 32 chars");
    }

    @Test
    @DisplayName("Resolve built-in CURRENT_TIMESTAMP placeholder")
    void testResolve_CurrentTimestamp() {
        String result = resolver.resolve("${CURRENT_TIMESTAMP}", Map.of());
        assertNotNull(result);
        assertFalse(result.contains("${"), "Timestamp should be fully resolved");
    }

    @Test
    @DisplayName("Resolve built-in TODAY placeholder")
    void testResolve_Today() {
        String result = resolver.resolve("date=${TODAY}", Map.of());
        assertTrue(result.matches("date=\\d{4}-\\d{2}-\\d{2}"),
                "TODAY should produce yyyy-MM-dd format");
    }

    @Test
    @DisplayName("Resolve built-in RANDOM_NUMBER placeholder")
    void testResolve_RandomNumber() {
        String result = resolver.resolve("${RANDOM_NUMBER}", Map.of());
        assertTrue(result.matches("\\d{9}"),
                "RANDOM_NUMBER should produce a 9-digit integer");
    }

    @Test
    @DisplayName("Leave unknown placeholder unchanged")
    void testResolve_UnknownPlaceholder() {
        String result = resolver.resolve("${UNKNOWN_KEY}", Map.of());
        assertEquals("${UNKNOWN_KEY}", result,
                "Unknown placeholders should remain as-is");
    }

    @Test
    @DisplayName("Resolve multiple placeholders in one string")
    void testResolve_MultiplePlaceholders() {
        String input = "id=${CUSTOMER_ID}&mobile=${MOBILE_NUMBER}";
        String result = resolver.resolve(input,
                Map.of("CUSTOMER_ID", "12345", "MOBILE_NUMBER", "9999999999"));
        assertEquals("id=12345&mobile=9999999999", result);
    }

    @Test
    @DisplayName("Null input returns null")
    void testResolve_NullInput() {
        assertNull(resolver.resolve(null, Map.of()));
    }
}

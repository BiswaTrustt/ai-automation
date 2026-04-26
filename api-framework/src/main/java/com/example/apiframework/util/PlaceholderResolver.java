package com.example.apiframework.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thread-safe utility that resolves {@code ${PLACEHOLDER}} tokens in strings.
 *
 * <h2>Built-in placeholders</h2>
 * <ul>
 *   <li>{@code ${CURRENT_TIMESTAMP}} – current date-time as {@code yyyyMMddHHmmssSSS}</li>
 *   <li>{@code ${UUID}} – a random UUID without hyphens</li>
 *   <li>{@code ${RANDOM_NUMBER}} – a random 9-digit integer</li>
 *   <li>{@code ${TODAY}} – today's date as {@code yyyy-MM-dd}</li>
 * </ul>
 *
 * <p>Any unrecognised placeholder that is also absent from the caller-supplied
 * {@code dynamicValues} map is left unchanged so that upstream code can detect
 * unresolved tokens.</p>
 */
@Slf4j
@Component
public class PlaceholderResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Resolves all {@code ${…}} tokens in the input string.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Caller-supplied {@code dynamicValues} map (case-insensitive key lookup)</li>
     *   <li>Built-in system placeholders</li>
     * </ol>
     *
     * @param input         the raw string that may contain placeholder tokens
     * @param dynamicValues caller-supplied key-value pairs; may be {@code null}
     * @return the fully-resolved string; unresolvable tokens are left as-is
     */
    public String resolve(String input, Map<String, String> dynamicValues) {
        if (input == null || input.isBlank()) {
            return input;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1); // e.g. "CUSTOMER_ID"
            String resolved = resolvePlaceholder(placeholder, dynamicValues);

            if (resolved != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
                log.debug("Resolved placeholder '{}' -> '{}'", placeholder, resolved);
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                log.warn("Could not resolve placeholder '${{}}'", placeholder);
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Attempts to resolve a single placeholder token (without the {@code ${…}} wrapper).
     *
     * @param placeholder  the token name, e.g. {@code CUSTOMER_ID}
     * @param dynamicValues caller-supplied overrides
     * @return the resolved value, or {@code null} if the token is unknown
     */
    private String resolvePlaceholder(String placeholder, Map<String, String> dynamicValues) {
        // 1. Check caller-supplied map first
        if (dynamicValues != null) {
            String callerValue = dynamicValues.get(placeholder);
            if (callerValue != null) {
                return callerValue;
            }
            // Case-insensitive fallback
            for (Map.Entry<String, String> entry : dynamicValues.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(placeholder)) {
                    return entry.getValue();
                }
            }
        }

        // 2. Built-in system placeholders
        return switch (placeholder.toUpperCase()) {
            case "CURRENT_TIMESTAMP" -> LocalDateTime.now().format(TIMESTAMP_FMT);
            case "UUID"              -> UUID.randomUUID().toString().replace("-", "");
            case "RANDOM_NUMBER"     -> String.valueOf(ThreadLocalRandom.current().nextInt(100_000_000, 999_999_999));
            case "TODAY"             -> LocalDate.now().format(DATE_FMT);
            default                  -> null;
        };
    }
}

package com.example.apiframework.journey.util;

import com.example.apiframework.journey.csv.CsvExpectedResultReader;
import com.example.apiframework.journey.dto.JourneyContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves placeholder tokens in headers, request bodies, and SQL.
 *
 * Supported sources:
 *   ${RANDOM:N}           — N-digit random integer (default N=10)
 *   ${RANDOM:UUID}        — random UUID
 *   ${TIMESTAMP}          — current epoch ms
 *   ${TIMESTAMP:pattern}  — current time formatted with pattern
 *   ${ENV:key}            — Spring Environment property
 *   ${DB:select ... }     — first column of first row from JdbcTemplate query
 *   ${CSV:file:col}       — first row's column from /expected/<file> on classpath
 *   ${RESPONSE:api.$.path} — JsonPath into a previously captured response body
 *   anything else          — left as-is
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicValueResolver {

    private static final Pattern TOKEN = Pattern.compile("\\$\\{([^}]+)\\}");

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;
    private final CsvExpectedResultReader csvReader;

    public String resolve(String input, JourneyContext ctx) {
        if (input == null || input.isEmpty()) return input;
        Matcher m = TOKEN.matcher(input);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String token = m.group(1);
            String value = resolveToken(token, ctx);
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    private String resolveToken(String token, JourneyContext ctx) {
        try {
            int colon = token.indexOf(':');
            String type = colon < 0 ? token : token.substring(0, colon);
            String arg  = colon < 0 ? ""    : token.substring(colon + 1);

            return switch (type.toUpperCase()) {
                case "RANDOM"      -> resolveRandom(arg);
                case "TIMESTAMP"   -> resolveTimestamp(arg);
                case "ENV"         -> environment.getProperty(arg, "");
                case "DB"          -> resolveDb(arg);
                case "CSV"         -> resolveCsv(arg, ctx);
                case "RESPONSE"    -> resolveResponse(arg, ctx);
                case "CTX"         -> ctx.getCapturedValues().getOrDefault(arg, "");
                default            -> "${" + token + "}";
            };
        } catch (Exception ex) {
            log.warn("Failed to resolve placeholder ${{}}: {}", token, ex.getMessage());
            return "${" + token + "}";
        }
    }

    private String resolveRandom(String arg) {
        if (arg.isEmpty()) arg = "10";
        if ("UUID".equalsIgnoreCase(arg)) return UUID.randomUUID().toString();
        int digits = Integer.parseInt(arg);
        StringBuilder sb = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) sb.append(ThreadLocalRandom.current().nextInt(10));
        return sb.toString();
    }

    private String resolveTimestamp(String pattern) {
        if (pattern.isEmpty()) return Long.toString(System.currentTimeMillis());
        return DateTimeFormatter.ofPattern(pattern).format(LocalDateTime.now());
    }

    private String resolveDb(String sql) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        if (rows.isEmpty()) return "";
        Object v = rows.get(0).values().iterator().next();
        return v == null ? "" : v.toString();
    }

    private String resolveCsv(String arg, JourneyContext ctx) {
        // arg = file:column, file resolved from /expected/<file> on classpath
        String[] parts = arg.split(":", 2);
        if (parts.length < 2) return "";
        Map<String, String> firstRow = csvReader.firstRow(parts[0], ctx.getScenarioCode());
        return firstRow.getOrDefault(parts[1], "");
    }

    private String resolveResponse(String arg, JourneyContext ctx) {
        // arg = apiName.<jsonpath>   — split on first '.'
        int dot = arg.indexOf('.');
        if (dot < 0) return "";
        String apiName = arg.substring(0, dot);
        String jsonPath = arg.substring(dot + 1);
        String body = ctx.getResponsesByApi().get(apiName);
        if (body == null) return "";
        try {
            Object value = JsonPath.read(body, jsonPath);
            return value == null ? "" : value.toString();
        } catch (PathNotFoundException ex) {
            return "";
        }
    }
}

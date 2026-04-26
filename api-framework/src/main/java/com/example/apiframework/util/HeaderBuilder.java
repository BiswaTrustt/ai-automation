package com.example.apiframework.util;

import com.example.apiframework.entity.ApiHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the resolved HTTP header map for a REST Assured request.
 *
 * <p>Processes each {@link ApiHeader} in sequence-number order, resolving
 * placeholders in dynamic headers via {@link PlaceholderResolver}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeaderBuilder {

    private final PlaceholderResolver placeholderResolver;

    /**
     * Produces an ordered map of {@code headerKey -> resolvedValue} pairs
     * ready to be passed to REST Assured.
     *
     * @param headers       the list of header definitions (pre-sorted by sequence)
     * @param dynamicValues caller-supplied placeholder overrides
     * @return an ordered header map (insertion order preserved)
     */
    public Map<String, String> build(List<ApiHeader> headers, Map<String, String> dynamicValues) {
        Map<String, String> resolved = new LinkedHashMap<>();

        for (ApiHeader header : headers) {
            String value = header.getIsDynamic()
                    ? placeholderResolver.resolve(header.getHeaderValue(), dynamicValues)
                    : header.getHeaderValue();

            resolved.put(header.getHeaderKey(), value);
            log.debug("Header built: {} = {}", header.getHeaderKey(), value);
        }

        log.info("Built {} headers for API request", resolved.size());
        return resolved;
    }
}

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.model.WebRequest;
import tools.jackson.core.JacksonException;

/** Request handling and execution metadata shared by the HTTP entries. */
public final class HttpSupport {
    private HttpSupport() {
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Map<?, ?> value) {
        return (Map<String, Object>) value;
    }

    public static Map<String, Object> parameters(Object value) {
        if (value instanceof String text) {
            try {
                value = JsonUtils.readValue(text, Object.class);
            } catch (JacksonException e) {
                throw new DatawayException(400, "Invalid request: " + e.getMessage(), e);
            }
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new DatawayException(400, "requestBody must be a JSON object");
        }
        return HttpSupport.objectMap(map);
    }

    public static String requiredText(Map<String, Object> input, String name) {
        if (!(input.get(name) instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    /** Reads a metadata object supplied as a map or stored as JSON. */
    public static Map<String, Object> document(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (value instanceof String text) {
            value = JsonUtils.readValue(text.isBlank() ? "{}" : text, Object.class);
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Metadata must be a JSON object");
        }
        return new LinkedHashMap<>(HttpSupport.objectMap(map));
    }

    public static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new DatawayException(400, "Invalid request: " + e.getMessage(), e);
        }
    }

    public static Map<String, ?> metadata(WebRequest request, Map<String, Object> parameters, Map<String, Object> body) {
        Map<String, String> visibleHeaders = new LinkedHashMap<>(request.getHeaders());
        visibleHeaders.remove("cookie");
        visibleHeaders.remove("authorization"); // Authentication credentials belong to the host, not scripts.

        Map<String, List<String>> headerValues = new LinkedHashMap<>(request.getHeaderValues());
        headerValues.remove("authorization");
        headerValues.remove("cookie");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("method", request.getMethod());
        metadata.put("path", request.getPath());
        metadata.put("headers", visibleHeaders);
        metadata.put("headerValues", headerValues);
        metadata.put("cookies", request.getCookies());
        metadata.put("parameters", parameters);
        metadata.put("body", body);
        return metadata;
    }

    public static int apiStatus(ApiState state) {
        if (!state.isPublished()) {
            return 0;
        }
        if (!state.isEnabled()) {
            return 3;
        }
        return state.isHasDraft() ? 2 : 1;
    }
}

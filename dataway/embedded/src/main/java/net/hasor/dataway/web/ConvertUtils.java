/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.*;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.service.DatawayException;
import tools.jackson.core.JacksonException;
import static net.hasor.dataway.service.HttpSupport.invalid;
import static net.hasor.dataway.service.HttpSupport.objectMap;

/** Converts management request and response data to and from API models. */
public final class ConvertUtils {

    public static ApiDefinition definition(String id, Map<String, Object> input) {
        try {
            Map<String, Object> sample = ConvertUtils.document(input.get("sample"));
            sample.put("requestBody", ConvertUtils.parameters(input.getOrDefault("requestBody", Map.of())));
            Object headers = input.getOrDefault("headerData", List.of());
            if (!(headers instanceof List<?> rows)) {
                throw new IllegalArgumentException("headerData must be an array");
            }
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> header) || !(header.get("checked") instanceof Boolean) || !(header.get("name") instanceof String) || !(header.get("value") instanceof String)) {
                    throw new IllegalArgumentException("Each header requires checked, name and value");
                }
            }

            sample.put("requestHeader", JsonUtils.writeValueAsString(headers));
            sample.remove("headerData");
            String schema = input.containsKey("schema") ? JsonUtils.writeValueAsString(ConvertUtils.document(input.get("schema"))) : null;
            String options = JsonUtils.writeValueAsString(ConvertUtils.document(input.get("optionInfo")));
            ApiDefinition definition = new ApiDefinition();
            definition.setId(id);
            definition.setMethod(ConvertUtils.text(input, "select"));
            definition.setPath(ConvertUtils.text(input, "apiPath"));
            definition.setType(ApiScriptType.valueOf(ConvertUtils.text(input, "codeType").toUpperCase(Locale.ROOT)));
            definition.setScript(ConvertUtils.text(input, "codeValue"));
            definition.setDescription(Objects.toString(input.get("comment"), ""));
            definition.setSchema(schema);
            definition.setSample(JsonUtils.writeValueAsString(sample));
            definition.setOptions(options);
            return definition;
        } catch (IllegalArgumentException | JacksonException e) {
            throw invalid(e);
        }
    }

    private static Map<String, Object> document(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (value instanceof String text) {
            value = JsonUtils.readValue(text.isBlank() ? "{}" : text, Object.class);
        }
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Metadata must be a JSON object");
        }
        return new LinkedHashMap<>(objectMap(value));
    }

    private static String text(Map<String, Object> input, String name) {
        if (!(input.get(name) instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    public static Map<String, Object> parameters(Object value) {
        if (value instanceof String text) {
            try {
                value = JsonUtils.readValue(text, Object.class);
            } catch (JacksonException e) {
                throw invalid(e);
            }
        }
        if (!(value instanceof Map<?, ?>)) {
            throw new DatawayException(400, "requestBody must be a JSON object");
        }
        return objectMap(value);
    }

    public static Map<String, Object> summary(ApiState state) {
        ApiDefinition definition = state.getDraft();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", definition.getId());
        result.put("version", state.getRevision());
        result.put("checked", false);
        result.put("select", definition.getMethod());
        result.put("path", definition.getPath());
        result.put("status", ConvertUtils.status(state));
        result.put("comment", definition.getDescription());
        return result;
    }

    public static int status(ApiState state) {
        return switch (state.getStatus()) {
            case "DRAFT" -> 0;
            case "PUBLISHED" -> 1;
            case "MODIFIED" -> 2;
            case "DISABLED" -> 3;
            default -> {
                throw new IllegalArgumentException("Unknown API status");
            }
        };
    }

    public static Map<String, Object> detail(ApiDefinition definition, long version, int status) {
        Map<String, Object> sample = ConvertUtils.document(definition.getSample());
        Object requestBody = sample.getOrDefault("requestBody", Map.of());
        Object headers = sample.getOrDefault("headerData", sample.getOrDefault("requestHeader", List.of()));
        if (headers instanceof String text) {
            headers = JsonUtils.readValue(text, Object.class);
        }

        Map<String, Object> code = new LinkedHashMap<>();
        code.put("codeValue", definition.getScript());
        code.put("requestBody", requestBody instanceof String ? requestBody : JsonUtils.writeValueAsString(requestBody));
        code.put("headerData", headers);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", definition.getId());
        result.put("version", version);
        result.put("select", definition.getMethod());
        result.put("path", definition.getPath());
        result.put("status", status);
        result.put("apiComment", definition.getDescription());
        result.put("codeType", definition.getType() == ApiScriptType.SQL ? "SQL" : "DataQL");
        result.put("codeInfo", code);
        result.put("requestBody", code.get("requestBody"));
        result.put("headerData", headers);
        result.put("optionData", ConvertUtils.document(definition.getOptions()));
        result.put("sample", sample);
        result.put("schema", ConvertUtils.document(definition.getSchema()));
        return result;
    }
}

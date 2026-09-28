/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.*;
import net.hasor.dataway.model.vo.ApiCodeVO;
import net.hasor.dataway.model.vo.ApiDetailVO;
import net.hasor.dataway.model.vo.ApiHistoryVO;
import net.hasor.dataway.model.vo.ApiSummaryVO;
import tools.jackson.core.JacksonException;

/** Converts API definitions and management views. */
public final class ConvertUtils {
    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public static ApiDefinition convertToApiDefinition(String id, Map<String, Object> input) {
        try {
            Map<String, Object> sample = new LinkedHashMap<>(ConvertUtils.convertToApiParameters(input.get("sample")));
            sample.put("requestBody", ConvertUtils.convertToApiParameters(input.get("requestBody")));
            Object headers = input.getOrDefault("headerData", List.of());
            if (!(headers instanceof List<?> rows)) {
                throw new IllegalArgumentException("headerData must be an array");
            }

            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> header)) {
                    throw new IllegalArgumentException("Each header requires checked, name and value");
                }

                boolean validChecked = header.get("checked") instanceof Boolean;
                boolean validName = header.get("name") instanceof String;
                boolean validValue = header.get("value") instanceof String;
                if (!validChecked || !validName || !validValue) {
                    throw new IllegalArgumentException("Each header requires checked, name and value");
                }
            }

            sample.put("requestHeader", JsonUtils.writeValueAsString(headers));
            sample.remove("headerData");
            String schema = input.containsKey("schema") ? JsonUtils.writeValueAsString(ConvertUtils.convertToApiParameters(input.get("schema"))) : null;
            String options = JsonUtils.writeValueAsString(ConvertUtils.convertToApiParameters(input.get("optionInfo")));

            ApiDefinition definition = new ApiDefinition();
            definition.setId(id);
            definition.setMethod(ConvertUtils.requiredText(input, "select"));
            definition.setPath(ConvertUtils.requiredText(input, "apiPath"));
            definition.setType(ApiScriptType.fromName(ConvertUtils.requiredText(input, "codeType")));
            definition.setScript(ConvertUtils.requiredText(input, "codeValue"));
            definition.setDescription(Objects.toString(input.get("comment"), ""));
            definition.setSchema(schema);
            definition.setSample(JsonUtils.writeValueAsString(sample));
            definition.setOptions(options);
            return definition;
        } catch (IllegalArgumentException | JacksonException e) {
            throw new DatawayException(400, "Invalid request: " + e.getMessage(), e);
        }
    }

    private static String requiredText(Map<String, Object> input, String name) {
        if (!(input.get(name) instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    /** Converts API parameters and metadata to a document; empty input yields a mutable empty map. */
    public static Map<String, Object> convertToApiParameters(Object value) {
        if (value instanceof String text) {
            try {
                value = text.isBlank() ? null : JsonUtils.readValue(text, Object.class);
            } catch (JacksonException e) {
                throw new DatawayException(400, "Invalid API document: " + e.getMessage(), e);
            }
        }

        if (value == null) {
            return new LinkedHashMap<>();
        }

        if (!(value instanceof Map<?, ?> map)) {
            throw new DatawayException(400, "API document must be a JSON object");
        }

        return (Map<String, Object>) map;
    }

    /** Converts the request and business inputs into the script's HTTP context. */
    public static Map<String, ?> convertToWebContext(WebRequest request, Map<String, Object> parameters, Map<String, Object> body) {
        Map<String, String> visibleHeaders = new LinkedHashMap<>(request.getHeaders());
        visibleHeaders.remove("cookie");
        visibleHeaders.remove("authorization"); // Authentication credentials belong to the host, not scripts.

        Map<String, List<String>> headerValues = new LinkedHashMap<>(request.getHeaderValues());
        headerValues.remove("authorization");
        headerValues.remove("cookie");

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("method", request.getMethod());
        context.put("path", request.getPath());
        context.put("headers", visibleHeaders);
        context.put("headerValues", headerValues);
        context.put("cookies", request.getCookies());
        context.put("parameters", parameters);
        context.put("body", body);
        return context;
    }

    public static ApiSummaryVO convertToApiSummaryVO(ApiDefinition definition, ApiState state) {
        ApiSummaryVO result = new ApiSummaryVO();
        result.setId(definition.getId());
        result.setVersion(state.getRevision());
        result.setChecked(false);
        result.setSelect(definition.getMethod());
        result.setPath(definition.getPath());
        result.setStatus(apiStatus(state));
        result.setComment(definition.getDescription());
        return result;
    }

    public static ApiDetailVO convertToApiDetailVO(ApiDefinition definition, ApiState state) {
        Map<String, Object> sample = ConvertUtils.convertToApiParameters(definition.getSample());
        ApiCodeVO code = ConvertUtils.convertToApiCodeVO(definition, sample);

        ApiDetailVO result = new ApiDetailVO();
        result.setId(definition.getId());
        result.setVersion(state.getRevision());
        result.setSelect(definition.getMethod());
        result.setPath(definition.getPath());
        result.setStatus(apiStatus(state));
        result.setApiComment(definition.getDescription());
        result.setCodeType(definition.getType().getDisplayName());
        result.setCodeInfo(code);
        result.setRequestBody(code.getRequestBody());
        result.setHeaderData(code.getHeaderData());
        result.setOptionData(ConvertUtils.convertToApiParameters(definition.getOptions()));
        result.setSample(sample);
        result.setSchema(ConvertUtils.convertToApiParameters(definition.getSchema()));
        return result;
    }

    private static int apiStatus(ApiState state) {
        if (!state.isPublished()) {
            return 0;
        }
        if (!state.isEnabled()) {
            return 3;
        }
        return state.isHasDraft() ? 2 : 1;
    }

    public static ApiCodeVO convertToApiCodeVO(ApiDefinition definition, Map<String, Object> sample) {
        Object requestBody = sample.getOrDefault("requestBody", Map.of());
        Object headers = sample.getOrDefault("headerData", sample.getOrDefault("requestHeader", List.of()));
        if (headers instanceof String text) {
            headers = JsonUtils.readValue(text, Object.class);
        }

        ApiCodeVO result = new ApiCodeVO();
        result.setCodeValue(definition.getScript());
        result.setRequestBody(requestBody instanceof String text ? text : JsonUtils.writeValueAsString(requestBody));
        result.setHeaderData(headers);
        return result;
    }

    public static ApiHistoryVO convertToApiHistoryVO(ApiRelease release, int status) {
        ApiHistoryVO result = new ApiHistoryVO();
        result.setHistoryId(release.getId());
        result.setTime(HISTORY_TIME.format(release.getPublishedAt()));
        result.setStatus(status);
        return result;
    }
}

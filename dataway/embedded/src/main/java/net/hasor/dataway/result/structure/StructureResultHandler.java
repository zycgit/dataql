/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.result.structure;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.domain.BinaryModel;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.result.AbstractResultHandler;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;

/** Applies the response template to execution outcomes, preserving explicit and binary responses. */
public class StructureResultHandler extends AbstractResultHandler {
    private static final String DEFAULT_RESPONSE_FORMAT = """
            {
                "success"      : "@resultStatus",
                "message"      : "@resultMessage",
                "location"     : "@blockLocation",
                "code"         : "@resultCode",
                "lifeCycleTime": "@timeLifeCycle",
                "executionTime": "@timeExecution",
                "value"        : "@resultData"
            }
            """;

    public StructureResultHandler() {
        this(Map.of());
    }

    public StructureResultHandler(Map<String, ?> defaults) {
        super(defaults);
    }

    @Override
    public Map<String, Object> prepareOptions(Map<String, ?> options) {
        Map<String, Object> resolved = super.prepareOptions(options);
        if (!resolved.containsKey("responseFormat")) {
            resolved.put("responseFormat", DEFAULT_RESPONSE_FORMAT);
        }

        this.readResponseFormat(resolved);
        return resolved;
    }

    private Map<?, ?> readResponseFormat(Map<String, ?> options) {
        Object value = DEFAULT_RESPONSE_FORMAT;
        if (options != null && options.containsKey("responseFormat")) {
            value = options.get("responseFormat");
        }
        if (!(value instanceof String text)) {
            throw new DatawayException(400, "responseFormat must be a JSON object string");
        }

        try {
            Object template = JsonUtils.readValue(text, Object.class);
            if (template instanceof Map<?, ?> format) {
                return format;
            }
        } catch (RuntimeException error) {
            throw new DatawayException(400, "responseFormat must be a JSON object string", error);
        }

        throw new DatawayException(400, "responseFormat must be a JSON object string");
    }

    @Override
    public ResultInfo handle(ResultContext context) {
        Object value = context.getValue();
        if (context.isSuccess() && (value instanceof ResultInfo || value instanceof BinaryModel || value instanceof byte[] || value instanceof InputStream)) {
            return ResultInfoUtils.convertToResultInfo(value);
        }

        Map<String, Object> formatted = new LinkedHashMap<>();
        this.readResponseFormat(context.getOptions()).forEach((key, placeholder) -> {
            Object field = switch (String.valueOf(placeholder)) {
                case "@resultStatus" -> context.isSuccess();
                case "@resultMessage" -> context.getMessage();
                case "@resultCode" -> context.getCode();
                case "@blockLocation", "@codeLocation" -> context.getLocation();
                case "@timeLifeCycle" -> context.getLifeCycleTime();
                case "@timeExecution" -> context.getExecutionTime();
                case "@resultData" -> value;
                default -> this.copyTemplateValue(placeholder);
            };
            formatted.put(key.toString(), field);
        });
        return ResultInfoUtils.json(200, formatted);
    }

    /** Keeps literal objects and arrays independent between executions. */
    private Object copyTemplateValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(key.toString(), this.copyTemplateValue(item)));
            return copy;
        }

        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object item : list) {
                copy.add(this.copyTemplateValue(item));
            }
            return copy;
        }
        return value;
    }
}

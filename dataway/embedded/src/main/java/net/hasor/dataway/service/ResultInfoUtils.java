/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.hasor.dataql.domain.BinaryModel;
import net.hasor.dataway.function.WebFile;
import net.hasor.dataway.model.ResultInfo;

/** Constructs management envelopes and converts JSON, binary and streaming responses. */
public final class ResultInfoUtils {
    private ResultInfoUtils() {
    }

    public static ResultInfo buildSuccess(Object value) {
        return ResultInfoUtils.json(200, ResultInfoUtils.successData(value));
    }

    public static ResultInfo buildSuccess(Object value, long version) {
        Map<String, Object> data = ResultInfoUtils.successData(value);
        data.put("version", version);
        return ResultInfoUtils.json(200, data);
    }

    private static Map<String, Object> successData(Object value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("code", 200);
        data.put("message", "OK");
        data.put("result", value);
        return data;
    }

    public static ResultInfo json(int status, Object value) {
        ResultInfo result = new ResultInfo();
        result.setData(value);
        result.setStatus(status);
        result.getHeaders().put("Content-Type", "application/json; charset=utf-8");
        result.getHeaders().put("Cache-Control", "no-store");
        result.getHeaders().put("X-Content-Type-Options", "nosniff");
        return result;
    }

    /** Preserves explicit responses and binary results without a management envelope. */
    public static ResultInfo convertToResultInfo(Object value) {
        if (value instanceof ResultInfo serialized) {
            return serialized;
        }
        if (value instanceof BinaryModel binary) {
            ResultInfo result = new ResultInfo();
            result.setJson(false);
            result.setData(binary);
            result.getHeaders().put("Content-Type", "application/octet-stream");
            if (binary.getSize() >= 0) {
                result.getHeaders().put("Content-Length", Long.toString(binary.getSize()));
            }
            if (binary instanceof WebFile file) {
                String contentType = file.getContentType();
                if (contentType != null && !contentType.isBlank() && !contentType.contains("\r") && !contentType.contains("\n")) {
                    result.getHeaders().put("Content-Type", contentType);
                }
                String name = file.getName();
                if (name != null) {
                    name = name.replace('\\', '/');
                    name = name.substring(name.lastIndexOf('/') + 1);
                    String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
                    result.getHeaders().put("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
                }
            }
            return result;
        }
        if (value instanceof byte[] bytes) {
            return ResultInfoUtils.convertToResultInfo("application/octet-stream", bytes);
        }
        if (value instanceof InputStream stream) {
            return ResultInfoUtils.convertToResultInfo("application/octet-stream", stream);
        }

        return json(200, value);
    }

    public static ResultInfo convertToResultInfo(String contentType, byte[] value) {
        ResultInfo result = new ResultInfo();
        result.setData(value);
        result.setJson(false);
        result.getHeaders().put("Content-Type", contentType);
        result.getHeaders().put("Content-Length", String.valueOf(value.length));
        return result;
    }

    /** The response source is closed after output, including HEAD and failed writes. */
    public static ResultInfo convertToResultInfo(String contentType, InputStream value) {
        ResultInfo result = new ResultInfo();
        result.setData(Objects.requireNonNull(value));
        result.setJson(false);
        result.getHeaders().put("Content-Type", contentType);
        return result;
    }
}

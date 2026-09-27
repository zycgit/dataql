/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Constructs response data for JSON, binary and streaming results. */
public final class ResultInfoUtils {
    private ResultInfoUtils() {
    }

    public static ResultInfo buildSuccess(Object value) {
        return json(200, successData(value));
    }

    public static ResultInfo buildSuccess(Object value, long version) {
        Map<String, Object> data = successData(value);
        data.put("version", version);
        return json(200, data);
    }

    private static Map<String, Object> successData(Object value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("code", 200);
        data.put("message", "OK");
        data.put("result", value);
        return data;
    }

    public static ResultInfo buildError(int status, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", false);
        data.put("code", status);
        data.put("message", message);
        data.put("error", message);
        return json(status, data);
    }

    public static ResultInfo json(int status, Object value) {
        ResultInfo result = ofObject(value);
        result.setStatus(status);
        result.getHeaders().put("Cache-Control", "no-store");
        result.getHeaders().put("X-Content-Type-Options", "nosniff");
        return result;
    }

    public static ResultInfo ofObject(Object value) {
        ResultInfo result = new ResultInfo();
        result.setData(value);
        result.getHeaders().put("Content-Type", "application/json; charset=utf-8");
        return result;
    }

    public static ResultInfo ofBytes(String contentType, byte[] value) {
        ResultInfo result = new ResultInfo();
        result.setData(value);
        result.setJson(false);
        result.getHeaders().put("Content-Type", contentType);
        result.getHeaders().put("Content-Length", String.valueOf(value.length));
        return result;
    }

    public static ResultInfo ofString(String contentType, String value) {
        return ofBytes(contentType, value.getBytes(StandardCharsets.UTF_8));
    }

    /** The response source is closed after output, including HEAD and failed writes. */
    public static ResultInfo ofStream(String contentType, InputStream value) {
        ResultInfo result = new ResultInfo();
        result.setData(Objects.requireNonNull(value));
        result.setJson(false);
        result.getHeaders().put("Content-Type", contentType);
        return result;
    }

    /** Preserves explicit responses and binary results without a management envelope. */
    public static ResultInfo result(Object value) {
        if (value instanceof ResultInfo serialized) {
            return serialized;
        }
        if (value instanceof byte[] bytes) {
            return ofBytes("application/octet-stream", bytes);
        }
        if (value instanceof InputStream stream) {
            return ofStream("application/octet-stream", stream);
        }

        return json(200, value);
    }
}

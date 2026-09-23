/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import net.hasor.dataway.service.*;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spi.DatawayException;

/** Shared request validation and response conventions for the console APIs. */
public abstract class AbstractApiController {
    protected final DatawayService service;
    protected final ApiDocument    documents = new ApiDocument();
    private final   String         method;
    private final   boolean        idRequired;

    protected AbstractApiController(DatawayService service, String method) {
        this(service, method, true);
    }

    protected AbstractApiController(DatawayService service, String method, boolean idRequired) {
        this.service = Objects.requireNonNull(service);
        this.method = Objects.requireNonNull(method);
        this.idRequired = idRequired;
    }

    /** Decodes the host body once and invokes the selected API without committing its response. */
    public final SerializationInfo handle(WebRequest request, WebResponse response) throws Exception {
        Map<String, Object> body = HttpSupport.body(request);
        Map<String, String> query = this.query(request);
        String id = query.getOrDefault("id", Objects.toString(body.get("id"), null));
        if (body.containsKey("id") && !Objects.equals(id, body.get("id"))) {
            throw new DatawayException(400, "Conflicting API ids");
        }
        if (!request.getMethod().equals(this.method)) {
            throw new DatawayException(405, "Method not allowed");
        }
        if (this.idRequired && (id == null || id.isBlank())) {
            throw new DatawayException(400, "id is required");
        }

        CallContext context = HttpSupport.context(request, response, body, body);
        return this.execute(id, query, body, context);
    }

    private Map<String, String> query(WebRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            for (String pair : request.getQuery().split("&")) {
                String[] parts = pair.split("=", 2);
                String key = HttpSupport.decode(parts[0]);
                String value = parts.length == 2 ? HttpSupport.decode(parts[1]) : "";
                if (result.putIfAbsent(key, value) != null) {
                    throw new DatawayException(400, "Duplicate query parameter: " + key);
                }
            }
        }
        return result;
    }

    protected abstract SerializationInfo execute(String id, Map<String, String> query, Map<String, Object> body, CallContext context) throws Exception;

    protected SerializationInfo result(Object value) {
        return HttpSupport.json(200, Map.of("success", true, "code", 200, "message", "OK", "result", value));
    }

    protected SerializationInfo result(Object value, long version) {
        return HttpSupport.json(200, Map.of("success", true, "code", 200, "message", "OK", "result", value, "version", version));
    }

    protected long version(Map<String, Object> body) {
        Object value = body.get("version");
        if (!(value instanceof Number number) || number.longValue() < 0 || number.doubleValue() != number.longValue()) {
            throw new DatawayException(400, "A non-negative integer version is required");
        }
        return number.longValue();
    }

    /** Scripts see the simulated business body, never the management command or editor contents. */
    protected CallContext executionContext(CallContext context, Map<String, Object> parameters) {
        Map<String, Object> metadata = new LinkedHashMap<>(context.request());
        Map<String, Object> input = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        metadata.put("parameters", input);
        metadata.put("body", input);
        return new CallContext(context.source(), context.identity(), metadata, context.response());
    }

    protected SerializationInfo executeScript(Callable<?> action) throws Exception {
        try {
            return HttpSupport.result(action.call());
        } catch (DatawayException e) {
            throw e;
        } catch (Exception e) {
            throw new DatawayException(422, "Script execution failed: " + e.getMessage(), e);
        }
    }
}

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.admin.AdminService;

/** Shared parameter handling and service invocation for the console APIs. */
public abstract class AbstractApiController {
    protected final AdminService adminService;
    private final   Operation    operation;

    protected AbstractApiController(AdminService adminService, Operation operation) {
        this.adminService = adminService;
        this.operation = operation;
    }

    public final Operation getOperation() {
        return this.operation;
    }

    /** Prepares the operation inputs after authorization and before management interception. */
    public final Map<String, Object> getParameters(WebRequest request) throws IOException {
        Map<String, String> query = this.query(request);
        Map<String, Object> body = request.readBody();
        Map<String, Object> parameters = new LinkedHashMap<>(query);
        parameters.putAll(body);
        if (this.operation != Operation.LIST) {
            parameters.put("id", this.id(query, body));
        }
        return parameters;
    }

    /** Resolves the stored target before an interceptor can approve or reject the operation. */
    public ApiDefinition getTargetDefinition(WebRequest request, Map<String, Object> parameters) {
        if (this.operation == Operation.LIST) {
            return null;
        } else {
            return this.adminService.getDraftByApi(this.id(Map.of(), parameters));
        }
    }

    public String getTargetReleaseId(WebRequest request) {
        return null;
    }

    /** Reads request parameters and invokes the selected console operation. */
    public final ResultInfo handle(WebRequest request, WebResponse response) throws Exception {
        UserIdentity identity = request.getIdentity();
        Map<String, Object> body = request.readBody();
        return this.execute(request, response, identity, body);
    }

    protected abstract ResultInfo execute(WebRequest request, WebResponse response, UserIdentity identity, Map<String, Object> body) throws Exception;

    protected Map<String, String> query(WebRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            for (String pair : request.getQuery().split("&")) {
                String[] parts = pair.split("=", 2);
                String key = decode(parts[0]);
                String value = parts.length == 2 ? decode(parts[1]) : "";
                if (result.putIfAbsent(key, value) != null) {
                    throw new DatawayException(400, "Duplicate query parameter: " + key);
                }
            }
        }

        return result;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new DatawayException(400, "Invalid request: " + e.getMessage(), e);
        }
    }

    /** Executes the selected service callback; failures belong to the host exception handlers. */
    protected final ResultInfo executeService(Callable<ResultInfo> action) throws Exception {
        return action.call();
    }

    /** Checks that independently loaded response parts still belong to the same API revision. */
    protected void checkVersion(String apiID, long version) {
        if (this.adminService.getVersionById(apiID) != version) {
            throw new DatawayException(409, "API changed; reload and retry");
        }
    }

    protected String id(Map<String, String> query, Map<String, Object> body) {
        String id = query.getOrDefault("id", Objects.toString(body.get("id"), null));
        if (body.containsKey("id") && !Objects.equals(id, body.get("id"))) {
            throw new DatawayException(400, "Conflicting API ids");
        }

        if (id == null || id.isBlank()) {
            throw new DatawayException(400, "id is required");
        }
        return id;
    }

    protected long version(Map<String, Object> body) {
        Object value = body.get("version");
        if (!(value instanceof Number number) || number.longValue() < 0 || number.doubleValue() != number.longValue()) {
            throw new DatawayException(400, "A non-negative integer version is required");
        }

        return number.longValue();
    }
}

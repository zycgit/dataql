/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.HttpSupport;
import net.hasor.dataway.service.admin.AdminService;
s
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

    /** Reads request parameters and invokes the selected console operation. */
    public final ResultInfo handle(WebRequest request, WebResponse response) throws Exception {
        UserIdentity identity = request.getIdentity();
        Map<String, Object> body = HttpSupport.body(request);
        Map<String, String> query = this.query(request);
        Map<String, ?> metadata = HttpSupport.metadata(request, body, body);
        return this.execute(query, body, identity, metadata, response);
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

    protected abstract ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception;

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

    /** Scripts see the simulated business body, never the management command or editor contents. */
    protected Map<String, ?> executionRequest(Map<String, ?> request, Map<String, Object> parameters) {
        Map<String, Object> metadata = new LinkedHashMap<>(request);
        Map<String, Object> input = new LinkedHashMap<>(parameters);

        metadata.put("parameters", input);
        metadata.put("body", input);
        return metadata;
    }
}

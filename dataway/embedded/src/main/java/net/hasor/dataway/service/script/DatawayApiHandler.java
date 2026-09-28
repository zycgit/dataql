/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.AuthorizationCheck;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.model.*;
import net.hasor.dataway.service.*;
import tools.jackson.databind.JsonNode;
import static net.hasor.dataway.dal.FieldDef.*;

/** Invokes published APIs; it exposes neither management operations nor UI assets. */
public final class DatawayApiHandler extends WebHandler {
    private final AuthorizationCheck authorizationCheck;
    private final ApiDataAccessLayer access;
    private final DatawayEngine      engine;

    public DatawayApiHandler(BeanContainer beans) {
        super(beans);
        this.authorizationCheck = beans.getBean(AuthorizationCheck.class);
        this.access = beans.getBean(ApiDataAccessLayer.class);
        this.engine = beans.getBean(DatawayEngine.class);
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) throws Exception {
        UserIdentity identity = request.getIdentity();
        if (!this.authorizationCheck.check(identity, Operation.INVOKE)) {
            throw new DatawayException(401, "Unauthorized");
        }

        Map<String, Object> body = request.readBody();
        Map<String, Object> parameters = this.parameters(request, body);
        Map<String, ?> metadata = ConvertUtils.convertToWebContext(request, parameters, body);
        String path = request.getPathInfo();
        String apiPath = path.isEmpty() ? "/" : path;
        ApiDefinition definition = this.findApi(request.getMethod(), apiPath);
        if (definition == null) {
            throw new DatawayException(404, "Published API not found");
        }

        Map<String, Object> options = this.options(definition);
        List<String> parameterNames = this.parameterNames(definition);
        DatawayQuery query = this.engine.newQuery(definition, parameterNames, options);
        Object result = query.execute(Operation.INVOKE, identity, parameters, metadata, response);
        return ResultInfoUtils.convertToResultInfo(result);
    }

    private Map<String, Object> parameters(WebRequest request, Map<String, Object> body) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            for (String pair : request.getQuery().split("&")) {
                String[] parts = pair.split("=", 2);
                String key = decode(parts[0]);
                String value = parts.length > 1 ? decode(parts[1]) : "";
                Object previous = values.get(key);
                if (previous == null) {
                    values.put(key, value);
                } else if (previous instanceof List<?> list) {
                    List<Object> repeated = new ArrayList<>(list);
                    repeated.add(value);
                    values.put(key, repeated);
                } else {
                    values.put(key, List.of(previous, value));
                }
            }
        }
        values.putAll(body);
        return values;
    }

    private ApiDefinition findApi(String method, String path) {
        Map<FieldDef, String> conditions = Map.of(METHOD, method.toUpperCase(Locale.ROOT), PATH, path, STATUS, "1");
        List<Map<FieldDef, String>> releases = this.access.listObjects(EntityType.RELEASE, conditions);
        Comparator<Map<FieldDef, String>> order = Comparator.comparingLong(row -> Long.parseLong(row.get(RELEASE_TIME)));
        Map<FieldDef, String> release = releases.stream().max(order).orElse(null);
        if (release == null) {
            return null;
        }

        ApiDefinition definition = new ApiDefinition();
        definition.setId(release.get(API_ID));
        definition.setMethod(release.get(METHOD));
        definition.setPath(release.get(PATH));
        definition.setType(ApiScriptType.fromName(release.get(TYPE)));
        definition.setScript(release.get(SCRIPT));
        definition.setDescription(release.get(COMMENT));
        definition.setSchema(release.get(SCHEMA));
        definition.setSample(release.get(SAMPLE));
        definition.setOptions(release.get(OPTION));
        return definition;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> options(ApiDefinition definition) {
        String document = definition.getOptions();
        try {
            String json = document == null || document.isBlank() ? "{}" : document;
            Map<String, Object> options = JsonUtils.readValue(json, Map.class);
            return options == null ? Map.of() : options;
        } catch (RuntimeException e) {
            throw new DatawayException(400, "Invalid API options: " + e.getMessage(), e);
        }
    }

    private List<String> parameterNames(ApiDefinition definition) {
        String sample = definition.getSample();
        if (sample == null || sample.isBlank()) {
            return List.of();
        }

        JsonNode requestBody = JsonUtils.readTree(sample).path("requestBody");
        if (requestBody.isString()) {
            requestBody = JsonUtils.readTree(requestBody.stringValue());
        }
        if (requestBody.isMissingNode() || requestBody.isNull()) {
            return List.of();
        }
        if (!requestBody.isObject()) {
            throw new DatawayException(400, "requestBody must be a JSON object");
        }
        return List.copyOf(requestBody.propertyNames());
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new DatawayException(400, "Invalid request: " + e.getMessage(), e);
        }
    }
}

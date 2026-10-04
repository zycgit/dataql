/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.AuthorizationCheck;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.DatawayException;
import tools.jackson.databind.JsonNode;
import static net.hasor.dataway.dal.FieldDef.*;

/** Shared publication lookup and execution for Java and HTTP calls. */
public class ApiServiceImpl implements ApiService {
    private final AuthorizationCheck authorizationCheck;
    private final ApiDataAccessLayer access;
    private final DatawayEngine      engine;

    public ApiServiceImpl(BeanContainer beans) {
        this.authorizationCheck = beans.getBean(AuthorizationCheck.class);
        this.access = beans.getBean(ApiDataAccessLayer.class);
        this.engine = beans.getBean(DatawayEngine.class);
    }

    @Override
    public ResultInfo invokeByPath(String method, String path, Map<String, ?> parameters) throws Exception {
        return this.invokePublished(method, path, parameters, null, ApiCallSource.PROGRAMMATIC, Map.of(), null);
    }

    @Override
    public ResultInfo invokeById(String apiID, Map<String, ?> parameters) throws Exception {
        if (apiID == null || apiID.isBlank()) {
            throw new DatawayException(400, "API id is required");
        }

        Map<FieldDef, String> release = this.findRelease(Map.of(API_ID, apiID, STATUS, "1"));
        return this.execute(release, parameters, null, ApiCallSource.PROGRAMMATIC, Map.of(), null);
    }

    /** HTTP entry points authorize before reading the request body. */
    UserIdentity authorize(UserIdentity identity) {
        UserIdentity caller = identity == null ? UserIdentity.anonymous(Map.of()) : identity;
        if (!this.authorizationCheck.check(caller, Operation.INVOKE)) {
            throw new DatawayException(401, "Unauthorized");
        }
        return caller;
    }

    /** Executes a published API with optional HTTP identity and function bindings. */
    ResultInfo invokePublished(String method, String path, Map<String, ?> parameters, UserIdentity identity, ApiCallSource source, Map<String, ?> request, WebResponse response) throws Exception {
        if (method == null || method.isBlank() || path == null) {
            throw new DatawayException(400, "API method and path are required");
        }

        String apiPath = path.isEmpty() ? "/" : path;
        Map<FieldDef, String> conditions = Map.of(METHOD, method.toUpperCase(Locale.ROOT), PATH, apiPath, STATUS, "1");
        Map<FieldDef, String> release = this.findRelease(conditions);
        return this.execute(release, parameters, identity, source, request, response);
    }

    private Map<FieldDef, String> findRelease(Map<FieldDef, String> conditions) {
        List<Map<FieldDef, String>> releases = this.access.listObjects(EntityType.RELEASE, conditions);
        Comparator<Map<FieldDef, String>> order = Comparator.comparingLong(row -> Long.parseLong(row.get(RELEASE_TIME)));
        return releases.stream().max(order).orElse(null);
    }

    private ResultInfo execute(Map<FieldDef, String> release, Map<String, ?> parameters, UserIdentity identity, ApiCallSource source, Map<String, ?> request, WebResponse response) throws Exception {
        if (release == null) {
            throw new DatawayException(404, "Published API not found");
        }

        ApiDefinition definition = this.definition(release);
        Map<String, Object> options = this.options(definition);
        List<String> parameterNames = this.parameterNames(definition);
        DatawayQuery query = this.engine.newQuery(definition, release.get(ID), parameterNames, options);
        Map<String, ?> values = parameters == null ? Map.of() : parameters;
        return query.execute(Operation.INVOKE, identity, source, values, request, response);
    }

    private ApiDefinition definition(Map<FieldDef, String> release) {
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

}

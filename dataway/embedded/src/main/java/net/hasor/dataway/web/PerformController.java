/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.ConvertUtils;
import net.hasor.dataway.service.admin.AdminService;
import net.hasor.dataway.service.script.ApiCallSource;
import net.hasor.dataway.service.script.DatawayEngine;
import net.hasor.dataway.service.script.DatawayQuery;

/** POST /perform. Previews the editor contents without saving them. */
public final class PerformController extends AbstractApiController {
    private final DatawayEngine engine;

    public PerformController(AdminService adminService, DatawayEngine engine) {
        super(adminService, Operation.DEBUG);
        this.engine = engine;
    }

    @Override
    public ApiDefinition getTargetDefinition(WebRequest request, Map<String, Object> parameters) {
        String id = this.id(Map.of(), parameters);
        if (id.equals("-1")) {
            return ConvertUtils.convertToApiDefinition(id, parameters);
        } else {
            return super.getTargetDefinition(request, parameters);
        }
    }

    @Override
    protected ResultInfo execute(WebRequest request, WebResponse response, UserIdentity identity, Map<String, Object> body) throws Exception {
        return this.executeService(() -> {
            String id = this.id(this.query(request), body);
            ApiDefinition definition = ConvertUtils.convertToApiDefinition(id, body);

            Map<String, Object> parameters = ConvertUtils.convertToApiParameters(body.get("requestBody"));
            List<String> parameterNames = List.copyOf(parameters.keySet());
            Map<String, Object> options = ConvertUtils.convertToApiParameters(definition.getOptions());
            DatawayQuery query = this.engine.newQuery(definition, null, parameterNames, options);

            Map<String, Object> input = new LinkedHashMap<>(parameters);
            Map<String, ?> execution = ConvertUtils.convertToWebContext(request, input, input);
            return query.execute(this.getOperation(), identity, ApiCallSource.DEBUG, parameters, execution, response);
        });
    }
}

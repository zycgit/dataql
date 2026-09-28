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
import net.hasor.dataway.service.HttpSupport;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.admin.AdminService;
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
    protected ResultInfo execute(WebRequest request, WebResponse response, UserIdentity identity, Map<String, Object> body) throws Exception {
        return this.executeService(() -> {
            String id = this.id(this.query(request), body);
            ApiDefinition definition = ConvertUtils.convertToApiDefinition(id, body);

            Map<String, Object> parameters = HttpSupport.parameters(body.getOrDefault("requestBody", Map.of()));
            List<String> parameterNames = List.copyOf(parameters.keySet());
            Map<String, Object> options = HttpSupport.document(definition.getOptions());
            DatawayQuery query = this.engine.newQuery(definition, parameterNames, options);

            Map<String, Object> input = new LinkedHashMap<>(parameters);
            Map<String, ?> execution = HttpSupport.metadata(request, input, input);
            Object result = query.execute(this.getOperation(), identity, parameters, execution, response);
            return ResultInfoUtils.convertToResultInfo(result);
        });
    }
}

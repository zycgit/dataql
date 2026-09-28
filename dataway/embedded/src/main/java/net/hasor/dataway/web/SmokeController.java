/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.HttpSupport;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.admin.AdminService;
import net.hasor.dataway.service.script.DatawayEngine;
import net.hasor.dataway.service.script.DatawayQuery;

/** POST /smoke. Runs the selected version of a saved draft. */
public final class SmokeController extends AbstractApiController {
    private final DatawayEngine engine;

    public SmokeController(AdminService adminService, DatawayEngine engine) {
        super(adminService, Operation.DEBUG);
        this.engine = engine;
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        return this.executeService(() -> {
            String id = this.id(query, body);
            Map<String, Object> parameters = HttpSupport.parameters(body.getOrDefault("requestBody", Map.of()));
            long version = this.version(body);

            ApiDefinition definition = this.adminService.getDraftByApi(id);
            this.checkVersion(id, version);

            Map<String, Object> sample = HttpSupport.document(definition.getSample());
            Map<String, Object> declared = HttpSupport.parameters(sample.getOrDefault("requestBody", Map.of()));
            List<String> parameterNames = List.copyOf(declared.keySet());
            Map<String, Object> options = HttpSupport.document(definition.getOptions());
            DatawayQuery datawayQuery = this.engine.newQuery(definition, parameterNames, options);

            Map<String, ?> execution = this.executionRequest(request, parameters);
            Object result = datawayQuery.execute(this.getOperation(), identity, parameters, execution, response);
            return ResultInfoUtils.convertToResultInfo(result);
        });
    }
}

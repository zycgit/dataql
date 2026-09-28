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
    protected ResultInfo execute(WebRequest request, WebResponse response, UserIdentity identity, Map<String, Object> body) throws Exception {
        return this.executeService(() -> {
            String id = this.id(this.query(request), body);
            Map<String, Object> parameters = ConvertUtils.convertToApiParameters(body.get("requestBody"));
            long version = this.version(body);

            ApiDefinition definition = this.adminService.getDraftByApi(id);
            this.checkVersion(id, version);

            Map<String, Object> sample = ConvertUtils.convertToApiParameters(definition.getSample());
            Map<String, Object> declared = ConvertUtils.convertToApiParameters(sample.get("requestBody"));
            List<String> parameterNames = List.copyOf(declared.keySet());
            Map<String, Object> options = ConvertUtils.convertToApiParameters(definition.getOptions());
            DatawayQuery query = this.engine.newQuery(definition, parameterNames, options);

            Map<String, Object> input = new LinkedHashMap<>(parameters);
            Map<String, ?> execution = ConvertUtils.convertToWebContext(request, input, input);
            Object result = query.execute(this.getOperation(), identity, parameters, execution, response);
            return ResultInfoUtils.convertToResultInfo(result);
        });
    }
}

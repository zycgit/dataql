/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.Map;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.admin.AdminService;
import net.hasor.dataway.service.script.DatawayEngine;

/** GET /get-handlers. Lists the names available in API options. */
public class ResultHandlersController extends AbstractApiController {
    private final DatawayEngine engine;

    public ResultHandlersController(AdminService adminService, DatawayEngine engine) {
        super(adminService, Operation.LIST);
        this.engine = engine;
    }

    @Override
    protected ResultInfo execute(WebRequest request, WebResponse response, UserIdentity identity, Map<String, Object> body) {
        return ResultInfoUtils.buildSuccess(this.engine.getResultHandlers());
    }
}
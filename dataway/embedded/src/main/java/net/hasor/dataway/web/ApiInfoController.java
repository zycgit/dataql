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
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.ConvertUtils;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.admin.AdminService;

/** GET /api-info. Loads request examples for the interface list. */
public final class ApiInfoController extends AbstractApiController {
    public ApiInfoController(AdminService adminService) {
        super(adminService, Operation.READ);
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        return this.executeService(() -> {
            String id = this.id(query, body);

            ApiState state = this.adminService.getApiById(id);
            ApiDefinition draft = this.adminService.getDraftByApi(id);
            this.checkVersion(id, state.getRevision());
            return ResultInfoUtils.buildSuccess(ConvertUtils.convertToApiDetailVO(draft, state));
        });
    }
}

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

/** GET /api-list. Lists the console APIs. */
public final class ApiListController extends AbstractApiController {
    public ApiListController(AdminService adminService) {
        super(adminService, Operation.LIST);
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        return this.executeService(() -> {
            return ResultInfoUtils.buildSuccess(this.adminService.list().stream().map(item -> {
                ApiState state = this.adminService.getApiById(item.getId());
                ApiDefinition draft = this.adminService.getDraftByApi(item.getId());
                this.checkVersion(item.getId(), state.getRevision());
                return ConvertUtils.convertToApiSummaryVO(draft, state);
            }).toList());
        });
    }
}

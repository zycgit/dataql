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
import net.hasor.dataway.model.ApiRelease;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.admin.AdminService;

/** GET /get-history. Loads a release belonging to the selected API. */
public final class ApiHistoryGetController extends AbstractApiController {
    public ApiHistoryGetController(AdminService adminService) {
        super(adminService, Operation.HISTORY);
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        return this.executeService(() -> {
            String id = this.id(query, body);
            ApiState state = this.adminService.historyState(id, this.getOperation(), identity, request, response);
            String releaseId = query.get("historyId");

            ApiRelease release = state.getHistory().stream().filter(item -> {
                return item.getId().equals(releaseId);
            }).findFirst().orElseThrow(() -> {
                return new DatawayException(404, "Release not found");
            });

            return this.result(ConvertUtils.detail(release.getDefinition(), state.getRevision(), ConvertUtils.status(state)));
        });
    }
}

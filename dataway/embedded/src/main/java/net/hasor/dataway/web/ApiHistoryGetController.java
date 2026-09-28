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
import net.hasor.dataway.model.*;
import net.hasor.dataway.service.ConvertUtils;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.admin.AdminService;

/** GET /get-history. Loads a release belonging to the selected API. */
public final class ApiHistoryGetController extends AbstractApiController {
    public ApiHistoryGetController(AdminService adminService) {
        super(adminService, Operation.HISTORY);
    }

    @Override
    protected ResultInfo execute(WebRequest request, WebResponse response, UserIdentity identity, Map<String, Object> body) throws Exception {
        return this.executeService(() -> {
            Map<String, String> query = this.query(request);
            String id = this.id(query, body);
            ApiState state = this.adminService.getApiById(id);
            String historyID = query.get("historyId");
            if (historyID == null || historyID.isBlank()) {
                throw new DatawayException(404, "Release not found");
            }

            ApiRelease release = this.adminService.getHistoryById(historyID);
            if (!id.equals(release.getDefinition().getId())) {
                throw new DatawayException(404, "Release not found");
            }

            this.checkVersion(id, state.getRevision());
            return ResultInfoUtils.buildSuccess(ConvertUtils.convertToApiDetailVO(release.getDefinition(), state));
        });
    }
}
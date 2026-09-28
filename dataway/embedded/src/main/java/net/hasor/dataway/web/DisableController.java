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

/** POST /disable. Disables the published API. */
public final class DisableController extends AbstractApiController {
    public DisableController(AdminService adminService) {
        super(adminService, Operation.DISABLE);
    }

    @Override
    protected ResultInfo execute(WebRequest request, WebResponse response, UserIdentity identity, Map<String, Object> body) throws Exception {
        return this.executeService(() -> {
            String id = this.id(this.query(request), body);

            long version = this.adminService.disableApi(id, this.version(body)).getRevision();
            return ResultInfoUtils.buildSuccess(true, version);
        });
    }
}

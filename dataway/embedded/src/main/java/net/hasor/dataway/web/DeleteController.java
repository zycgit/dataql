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
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.admin.AdminService;

/** POST /delete. Deletes the API and its release history. */
public final class DeleteController extends AbstractApiController {
    public DeleteController(AdminService adminService) {
        super(adminService, Operation.DELETE);
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        return this.executeService(() -> {
            String id = this.id(query, body);

            this.adminService.deleteApi(id, this.version(body), this.getOperation(), identity, request, response);
            return this.result(true);
        });
    }
}

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

/** POST /publish. Publishes a draft as an immutable release. */
public final class PublishController extends AbstractApiController {
    public PublishController(AdminService adminService) {
        super(adminService, Operation.PUBLISH);
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        return this.executeService(() -> {
            String id = this.id(query, body);

            long version = this.adminService.publish(id, this.version(body), this.getOperation(), identity, request, response).getRevision();
            return this.result(true, version);
        });
    }
}

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.Map;
import java.util.UUID;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.*;
import net.hasor.dataway.service.ConvertUtils;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.admin.AdminService;

/** POST /save-api. Creates or updates a versioned draft. */
public final class SaveApiController extends AbstractApiController {
    public SaveApiController(AdminService adminService) {
        super(adminService, Operation.SAVE);
    }

    @Override
    public ApiDefinition getTargetDefinition(WebRequest request, Map<String, Object> parameters) {
        String id = this.id(Map.of(), parameters);
        if (id.equals("-1")) {
            return ConvertUtils.convertToApiDefinition(id, parameters);
        } else {
            return super.getTargetDefinition(request, parameters);
        }
    }

    @Override
    protected ResultInfo execute(WebRequest request, WebResponse response, UserIdentity identity, Map<String, Object> body) throws Exception {
        return this.executeService(() -> {
            String id = this.id(this.query(request), body);
            String target = id.equals("-1") ? UUID.randomUUID().toString() : id;

            ApiDefinition definition = ConvertUtils.convertToApiDefinition(target, body);
            ApiState saved = this.adminService.save(definition, this.version(body));
            return ResultInfoUtils.buildSuccess(target, saved.getRevision());
        });
    }
}

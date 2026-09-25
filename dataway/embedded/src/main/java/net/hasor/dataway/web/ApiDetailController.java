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
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.DatawayService;

/** GET /api-detail. Loads the editor document and its current version. */
public final class ApiDetailController extends AbstractApiController {
    public ApiDetailController(DatawayService service) {
        super(service, Operation.READ);
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        return this.executeService(() -> {
            String id = this.id(query, body);
            ApiState state = this.service.getApiById(id, this.getOperation(), identity, request, response);
            return this.result(ConvertUtils.detail(state.getDraft(), state.getRevision(), ConvertUtils.status(state)));
        });
    }
}

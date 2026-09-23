/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.Map;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.SerializationInfo;
import net.hasor.dataway.service.model.ApiState;
import net.hasor.dataway.spi.CallContext;

/** GET /api-info. Loads request examples for the interface list. */
public final class ApiInfoController extends AbstractApiController {
    public ApiInfoController(DatawayService service) {
        super(service, "GET");
    }

    @Override
    protected SerializationInfo execute(String id, Map<String, String> query, Map<String, Object> body, CallContext context) {
        ApiState state = this.service.getApiById(id, context);
        return this.result(this.documents.detail(state.draft(), state.revision(), this.documents.status(state)));
    }
}

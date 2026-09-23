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
import net.hasor.dataway.spi.CallContext;

/** GET /api-list. Lists the console APIs. */
public final class ApiListController extends AbstractApiController {
    public ApiListController(DatawayService service) {
        super(service, "GET", false);
    }

    @Override
    protected SerializationInfo execute(String id, Map<String, String> query, Map<String, Object> body, CallContext context) {
        return this.result(this.service.list(context).stream().map(this.documents::summary).toList());
    }
}

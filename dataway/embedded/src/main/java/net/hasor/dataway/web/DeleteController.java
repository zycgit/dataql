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

/** POST /delete. Deletes the API and its release history. */
public final class DeleteController extends AbstractApiController {
    public DeleteController(DatawayService service) {
        super(service, "POST");
    }

    @Override
    protected SerializationInfo execute(String id, Map<String, String> query, Map<String, Object> body, CallContext context) {
        this.service.deleteApi(id, this.version(body), context);
        return this.result(true);
    }
}

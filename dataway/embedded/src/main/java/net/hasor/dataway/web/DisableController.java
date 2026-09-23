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

/** POST /disable. Disables the published API. */
public final class DisableController extends AbstractApiController {
    public DisableController(DatawayService service) {
        super(service, "POST");
    }

    @Override
    protected SerializationInfo execute(String id, Map<String, String> query, Map<String, Object> body, CallContext context) {
        long version = this.service.disableApi(id, this.version(body), context).revision();
        return this.result(true, version);
    }
}

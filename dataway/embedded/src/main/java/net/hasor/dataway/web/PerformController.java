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

/** POST /perform. Previews the editor contents without saving them. */
public final class PerformController extends AbstractApiController {
    public PerformController(DatawayService service) {
        super(service, "POST");
    }

    @Override
    protected SerializationInfo execute(String id, Map<String, String> query, Map<String, Object> body, CallContext context) throws Exception {
        Map<String, Object> parameters = this.documents.parameters(body.getOrDefault("requestBody", Map.of()));
        CallContext execution = this.executionContext(context, parameters);
        return this.executeScript(() -> this.service.debug(this.documents.definition(id, body), parameters, execution));
    }
}

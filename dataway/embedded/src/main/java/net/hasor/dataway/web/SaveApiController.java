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
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.SerializationInfo;
import net.hasor.dataway.service.model.ApiDefinition;
import net.hasor.dataway.service.model.ApiState;
import net.hasor.dataway.spi.CallContext;

/** POST /save-api. Creates or updates a versioned draft. */
public final class SaveApiController extends AbstractApiController {
    public SaveApiController(DatawayService service) {
        super(service, "POST");
    }

    @Override
    protected SerializationInfo execute(String id, Map<String, String> query, Map<String, Object> body, CallContext context) {
        String target = id.equals("-1") ? UUID.randomUUID().toString() : id;
        ApiDefinition definition = this.documents.definition(target, body);
        ApiState saved = this.service.save(definition, this.version(body), context);
        return this.result(saved.draft().id(), saved.revision());
    }
}

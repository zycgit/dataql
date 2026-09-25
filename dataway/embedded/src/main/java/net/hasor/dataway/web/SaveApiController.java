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
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.DatawayService;

/** POST /save-api. Creates or updates a versioned draft. */
public final class SaveApiController extends AbstractApiController {
    public SaveApiController(DatawayService service) {
        super(service, Operation.SAVE);
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        return this.executeService(() -> {
            String id = this.id(query, body);
            String target = id.equals("-1") ? UUID.randomUUID().toString() : id;

            ApiDefinition definition = ConvertUtils.definition(target, body);
            ApiState saved = this.service.save(definition, this.version(body), this.getOperation(), identity, request, response);
            return this.result(saved.getDraft().getId(), saved.getRevision());
        });
    }
}

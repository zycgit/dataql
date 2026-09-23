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
import net.hasor.dataway.service.model.ApiRelease;
import net.hasor.dataway.service.model.ApiState;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spi.DatawayException;

/** GET /get-history. Loads a release belonging to the selected API. */
public final class ApiHistoryGetController extends AbstractApiController {
    public ApiHistoryGetController(DatawayService service) {
        super(service, "GET");
    }

    @Override
    protected SerializationInfo execute(String id, Map<String, String> query, Map<String, Object> body, CallContext context) {
        ApiState state = this.service.historyState(id, context);
        String releaseId = query.get("historyId");
        ApiRelease release = state.history().stream().filter(item -> {
            return item.id().equals(releaseId);
        }).findFirst().orElseThrow(() -> {
            return new DatawayException(404, "Release not found");
        });

        return this.result(this.documents.detail(release.definition(), state.revision(), this.documents.status(state)));
    }
}

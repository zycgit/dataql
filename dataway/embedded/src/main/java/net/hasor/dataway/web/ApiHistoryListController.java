/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.SerializationInfo;
import net.hasor.dataway.service.model.ApiState;
import net.hasor.dataway.spi.CallContext;

/** GET /api-history. Lists releases from newest to oldest. */
public final class ApiHistoryListController extends AbstractApiController {
    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public ApiHistoryListController(DatawayService service) {
        super(service, "GET");
    }

    @Override
    protected SerializationInfo execute(String id, Map<String, String> query, Map<String, Object> body, CallContext context) {
        ApiState state = this.service.historyState(id, context);
        return this.result(state.history().reversed().stream().map(release -> {
            return Map.of(                                              //
                    "historyId", release.id(),                          //
                    "time", HISTORY_TIME.format(release.publishedAt()), //
                    "status", state.enabled() && release.equals(state.published()) ? 1 : 3);
        }).toList());
    }
}

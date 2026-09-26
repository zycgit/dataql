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
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.admin.AdminService;

/** GET /api-history. Lists releases from newest to oldest. */
public final class ApiHistoryListController extends AbstractApiController {
    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public ApiHistoryListController(AdminService adminService) {
        super(adminService, Operation.HISTORY);
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        return this.executeService(() -> {
            String id = this.id(query, body);

            ApiState state = this.adminService.historyState(id, this.getOperation(), identity, request, response);
            return this.result(state.getHistory().reversed().stream().map(release -> {
                return Map.of(                                              //
                        "historyId", release.getId(),                          //
                        "time", HISTORY_TIME.format(release.getPublishedAt()), //
                        "status", state.isEnabled() && release.equals(state.getPublished()) ? 1 : 3);
            }).toList());
        });
    }
}

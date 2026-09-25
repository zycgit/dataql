/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.util.Map;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.ResultInfoUtils;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.DatawayService;

/** POST /smoke. Runs the selected version of a saved draft. */
public final class SmokeController extends AbstractApiController {
    public SmokeController(DatawayService service) {
        super(service, Operation.DEBUG);
    }

    @Override
    protected ResultInfo execute(Map<String, String> query, Map<String, Object> body, UserIdentity identity, Map<String, ?> request, WebResponse response) throws Exception {
        String id = this.id(query, body);
        Map<String, Object> parameters = ConvertUtils.parameters(body.getOrDefault("requestBody", Map.of()));

        Map<String, ?> execution = this.executionRequest(request, parameters);
        return this.executeService(() -> {
            return ResultInfoUtils.result(this.service.debug(id, this.version(body), parameters, this.getOperation(), identity, execution, response));
        });
    }
}

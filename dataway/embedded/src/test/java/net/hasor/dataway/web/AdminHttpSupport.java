/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.AuthorizationCheck;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.model.*;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.admin.AdminService;
import net.hasor.dataway.service.admin.DatawayAdminHandler;
import net.hasor.dataway.service.script.DatawayEngine;
import net.hasor.dataway.web.body.UploadStorage;
import net.hasor.dataway.web.support.HttpTestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Controllers and HTTP serialization are real; only the atomic management service is mocked. */
abstract class AdminHttpSupport {
    protected final AdminService   service = mock(AdminService.class);
    protected final ApiState       state   = this.state(4);
    protected final ApiDefinition  draft   = this.definition("api", "return 'draft';");
    private         HttpTestServer server;

    protected ApiState state(long version) {
        ApiState state = new ApiState();
        state.setApiID("api");
        state.setRevision(version);
        state.setPublished(true);
        state.setEnabled(true);
        state.setHasDraft(true);
        return state;
    }

    protected ApiDefinition definition(String id, String script) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(id);
        definition.setMethod("POST");
        definition.setPath("/example");
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript(script);
        definition.setDescription("Example description");
        definition.setSample("{\"requestBody\":{\"name\":\"example\"},\"requestHeader\":\"[]\"}");
        definition.setOptions("{\"resultStructure\":false}");
        definition.setSchema("{\"type\":\"object\"}");
        return definition;
    }

    @BeforeEach
    void startHttpHost() throws IOException {
        when(this.service.getApiById("api")).thenReturn(this.state);
        when(this.service.getDraftByApi("api")).thenReturn(this.draft);
        when(this.service.getVersionById("api")).thenReturn(4L);
        BeanContainer beans = new BeanContainer();
        beans.setBean(IdentityProvider.class, WebRequest::getIdentity);
        beans.setBean(AuthorizationCheck.class, (identity, operation) -> true);
        beans.setBean(UploadStorage.class, UploadStorage.DEFAULT);
        beans.setBean(AdminService.class, this.service);
        beans.setBean(DatawayEngine.class, mock(DatawayEngine.class));
        this.server = new HttpTestServer("/console", new DatawayAdminHandler(beans));
    }

    @AfterEach
    void stopHttpHost() {
        if (this.server != null) {
            this.server.close();
        }
    }

    protected HttpResponse<String> get(String path) throws Exception {
        return this.server.send("GET", "/console" + path, null, new byte[0]);
    }

    protected HttpResponse<String> post(String path, Object body) throws Exception {
        return this.postJson(path, JsonUtils.writeValueAsString(body));
    }

    protected HttpResponse<String> postJson(String path, String json) throws Exception {
        return this.server.send("POST", "/console" + path, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    protected Map<?, ?> success(HttpResponse<String> response) {
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
        Map<?, ?> body = JsonUtils.readValue(response.body(), Map.class);
        assertEquals(true, body.get("success"));
        assertEquals(200, body.get("code"));
        assertEquals("OK", body.get("message"));
        return body;
    }

    protected ApiRelease release(String id, ApiDefinition definition) {
        ApiRelease release = new ApiRelease();
        release.setId(id);
        release.setDefinition(definition);
        release.setPublishedAt(Instant.parse("2026-01-02T03:04:05Z"));
        return release;
    }
}

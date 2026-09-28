/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.config.ServiceTestSupport;
import net.hasor.dataway.web.support.HttpTestServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DebugHttpTest extends ServiceTestSupport {
    @ParameterizedTest
    @ValueSource(strings = { "/perform", "/smoke" })
    void debugExecutesWithBusinessParametersIdentityAndHttpFunctions(String path) throws Exception {
        String script = """
                import 'net.hasor.dataway.function.WebUdfSource' as web;
                run web.setHeader('X-Preview', 'yes');
                run web.setCookie('preview', 'ok');
                return [${name}, web.jsonBody(), web.header('Content-Type')];
                """;
        Map<FieldDef, String> draft = this.info("api", "0", 4);
        draft.put(SCRIPT, script);
        draft.put(SAMPLE, "{\"requestBody\":{\"name\":\"saved-example\"}}");
        this.storeInfo(draft);
        UserIdentity identity = UserIdentity.authenticated("editor");
        AtomicInteger intercepted = new AtomicInteger();
        this.config.resultStructure(false).identityProvider(request -> identity).apiInterceptor((context, chain) -> {
            assertSame(identity, context.getIdentity());
            assertEquals(Operation.DEBUG, context.getOperation());
            assertEquals(Map.of("name", "actual"), context.getParameters());
            intercepted.incrementAndGet();
            return chain.proceed(context);
        });
        Map<String, Object> body = this.editor(script);
        body.put("requestBody", Map.of("name", "actual"));
        try (HttpTestServer server = new HttpTestServer("/console", this.config.createDataway().getAdminHandler())) {
            HttpResponse<String> response = this.post(server, path, body);
            assertEquals(200, response.statusCode(), response.body());
            assertEquals(List.of("actual", Map.of("name", "actual"), "application/json"), JsonUtils.readValue(response.body(), List.class));
            assertEquals("yes", response.headers().firstValue("X-Preview").orElseThrow());
            assertTrue(response.headers().allValues("Set-Cookie").stream().anyMatch(value -> value.startsWith("preview=ok")));
        }
        assertEquals(1, intercepted.get());
        verify(this.access, never()).write(anyList());
        if (path.equals("/perform")) {
            verify(this.access, never()).getObject(any(), anyString());
        }
    }

    private Map<String, Object> editor(String script) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", "api");
        body.put("version", 4);
        body.put("select", "POST");
        body.put("apiPath", "/example");
        body.put("codeType", "DataQL");
        body.put("codeValue", script);
        return body;
    }

    private HttpResponse<String> post(HttpTestServer server, String path, Object body) throws Exception {
        return server.send("POST", "/console" + path, "application/json", JsonUtils.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void smokeExecutesTheSavedDraftAndUsesItsOptionsAndDeclaredFragmentNames() throws Exception {
        Map<FieldDef, String> draft = this.info("api", "0", 4);
        draft.put(TYPE, "SQL");
        draft.put(SCRIPT, "select :name");
        draft.put(SAMPLE, "{\"requestBody\":{\"name\":\"saved\"}}");
        draft.put(OPTION, "{\"resultStructure\":false}");
        this.storeInfo(draft);
        this.config.fragment(ApiScriptType.SQL.getTypeName(), () -> (hints, parameters, script) -> {
            assertEquals("select :name", script);
            assertEquals(Map.of("name", "actual"), parameters);
            return parameters;
        });
        Map<String, Object> body = this.editor("return 'unsaved';");
        body.put("requestBody", Map.of("name", "actual", "undeclared", "ignored"));
        body.put("optionInfo", Map.of("resultStructure", true));
        try (HttpTestServer server = new HttpTestServer("/console", this.config.createDataway().getAdminHandler())) {
            HttpResponse<String> response = this.post(server, "/smoke", body);
            assertEquals(200, response.statusCode());
            assertEquals(Map.of("name", "actual"), JsonUtils.readValue(response.body(), Map.class));
        }
        verify(this.access, never()).write(anyList());
    }

    @Test
    void staleSmokeRequestsDoNotRunAnyScript() throws Exception {
        this.storeInfo(this.info("api", "0", 5));
        AtomicInteger calls = new AtomicInteger();
        this.config.apiInterceptor((context, chain) -> {
            calls.incrementAndGet();
            return chain.proceed(context);
        });
        try (HttpTestServer server = new HttpTestServer("/console", this.config.createDataway().getAdminHandler())) {
            assertEquals(409, this.post(server, "/smoke", this.editor("return 1;")).statusCode());
        }
        assertEquals(0, calls.get());
    }

    @Test
    void performUsesEditorOptionsWithoutMutatingEngineDefaults() throws Exception {
        Map<String, Object> body = this.editor("return ${name};");
        body.put("requestBody", "{\"name\":\"preview\"}");
        body.put("optionInfo", Map.of("resultStructure", false));
        try (HttpTestServer server = new HttpTestServer("/console", this.config.createDataway().getAdminHandler())) {
            assertEquals("preview", JsonUtils.readValue(this.post(server, "/perform", body).body(), Object.class));
            body.remove("optionInfo");
            HttpResponse<String> response = this.post(server, "/perform", body);
            assertEquals(200, response.statusCode());
            Map<?, ?> result = JsonUtils.readValue(response.body(), Map.class);
            assertEquals(true, result.get("success"));
            assertEquals("preview", result.get("value"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "responseFormat", "resultStructure", "wrapAllParameters", "wrapParameterName" })
    void explicitNullOptionsReturn400WhileOmittedOptionsUseDefaults(String option) throws Exception {
        String body = """
                {"id":"-1","select":"POST","apiPath":"/preview","codeType":"DataQL",
                 "codeValue":"return 'preview';","optionInfo":{"%s":null}}
                """.formatted(option);
        AtomicInteger executions = new AtomicInteger();
        this.config.apiInterceptor((context, chain) -> {
            executions.incrementAndGet();
            return chain.proceed(context);
        });
        try (HttpTestServer server = new HttpTestServer("/console", this.config.createDataway().getAdminHandler())) {
            HttpResponse<String> invalid = server.send("POST", "/console/perform", "application/json", body.getBytes(StandardCharsets.UTF_8));
            assertEquals(400, invalid.statusCode(), invalid.body());
            assertTrue(JsonUtils.readTree(invalid.body()).get("message").stringValue().contains(option));
            assertEquals(0, executions.get());

            HttpResponse<String> defaults = this.post(server, "/perform", this.editor("return 'preview';"));
            assertEquals(200, defaults.statusCode(), defaults.body());
            Map<?, ?> result = JsonUtils.readValue(defaults.body(), Map.class);
            assertEquals(true, result.get("success"));
            assertEquals("preview", result.get("value"));
            assertEquals(1, executions.get());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "/perform", "/smoke" })
    void debugScriptFailuresUseTheConfiguredResponseTemplate(String path) throws Exception {
        String script = "throw 422, 'invalid input';";
        Map<FieldDef, String> draft = this.info("api", "0", 4);
        draft.put(SCRIPT, script);
        this.storeInfo(draft);
        try (HttpTestServer server = new HttpTestServer("/console", this.config.createDataway().getAdminHandler())) {
            HttpResponse<String> response = this.post(server, path, this.editor(script));
            assertEquals(200, response.statusCode(), response.body());
            Map<?, ?> result = JsonUtils.readValue(response.body(), Map.class);
            assertEquals(false, result.get("success"));
            assertEquals(422, result.get("code"));
            assertEquals("invalid input", result.get("value"));
        }
    }

    @Test
    void debugAuthorizationIsCheckedBeforeParsingOrLoadingDrafts() throws Exception {
        this.config.authorizationCheck((identity, operation) -> operation != Operation.DEBUG);
        try (HttpTestServer server = new HttpTestServer("/console", this.config.createDataway().getAdminHandler())) {
            for (String path : List.of("/perform", "/smoke")) {
                HttpResponse<String> response = server.send("POST", "/console" + path, "application/json", "malformed".getBytes(StandardCharsets.UTF_8));
                assertEquals(401, response.statusCode());
            }
        }
        verify(this.access, never()).getObject(eq(EntityType.INFO), anyString());
    }
}

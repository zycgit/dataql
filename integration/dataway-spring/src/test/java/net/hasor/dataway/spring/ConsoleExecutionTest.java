/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.DatawayException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ConsoleExecutionTest {
    private final WebApplicationContextRunner context = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(DatawayAutoConfiguration.class, WebMvcAutoConfiguration.class)).withPropertyValues("dataway.admin-enabled=true");

    @Test
    void previewAndSmokeUseQueriesWithIndependentOptionsAndWebContext() {
        List<String> calls = new ArrayList<>();
        UserIdentity identity = UserIdentity.authenticated("editor");
        DatawayConfig config = new DatawayConfig().resultStructure(false).apiInterceptor((invocation, next) -> {
            assertEquals(Operation.DEBUG, invocation.getOperation());
            assertSame(identity, invocation.getIdentity());
            calls.add("api-before");
            Object result = next.proceed(invocation);
            calls.add("api-after");
            return result;
        });

        Dataway dataway = new Dataway(config.dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> identity).adminInterceptor((action, next) -> {
            assertEquals(Operation.DEBUG, action.getOperation());
            assertSame(identity, action.getIdentity());
            calls.add("admin-before");
            Object result = next.proceed();
            calls.add("admin-after");
            return result;
        }));
        String savedScript = """
                import 'net.hasor.dataway.function.WebUdfSource' as web;
                run web.setHeader('X-Result', web.header('x-request'));
                run web.setCookie('echo', web.cookie('session'));
                return {'source': 'saved', 'parameter': ${name}, 'body': web.jsonBody()};
                """;
        ApiDefinition draft = this.draft("draft", ApiScriptType.DATA_QL, savedScript);
        draft.setSample("{\"requestBody\":{\"name\":\"sample\"}}");
        dataway.getAdminService().save(draft, 0);

        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            assertNull(c.getStartupFailure());
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            String previewScript = """
                    import 'net.hasor.dataway.function.WebUdfSource' as web;
                    run web.setHeader('X-Result', web.header('x-request'));
                    run web.setCookie('echo', web.cookie('session'));
                    return {'source': 'preview', 'parameter': ${args}.name, 'body': web.jsonBody()};
                    """;
            Map<String, Object> preview = Map.of("id", "draft", "select", "POST", "apiPath", "/draft", "codeType", "DataQL", "codeValue", previewScript, "requestBody", Map.of("name", "preview"), "optionInfo", Map.of("resultStructure", true, "wrapAllParameters", true, "wrapParameterName", "args", "responseFormat", "{\"ok\":\"@resultStatus\",\"data\":\"@resultData\"}"));
            var performed = mvc.perform(post("/dataway/api/perform?id=draft").header("X-Request", "preview-header").cookie(new Cookie("session", "preview-cookie")).contentType("application/json").content(JsonUtils.writeValueAsString(preview))).andReturn().getResponse();
            assertEquals(200, performed.getStatus());
            assertEquals("preview-header", performed.getHeader("X-Result"));
            assertTrue(performed.getHeaders("Set-Cookie").contains("echo=preview-cookie; Path=/"));
            var previewResult = JsonUtils.readTree(performed.getContentAsString());
            assertTrue(previewResult.get("ok").booleanValue());
            assertEquals(Map.of("source", "preview", "parameter", "preview", "body", Map.of("name", "preview")), JsonUtils.readValue(JsonUtils.writeValueAsString(previewResult.get("data")), Map.class));
            assertEquals(List.of("admin-before", "api-before", "api-after", "admin-after"), calls);

            calls.clear();
            Map<String, Object> smoke = Map.of("id", "draft", "version", 1, "requestBody", Map.of("name", "smoke"), "codeValue", "throw 500, 'must not execute';", "optionInfo", preview.get("optionInfo"));
            var smoked = mvc.perform(post("/dataway/api/smoke?id=draft").header("X-Request", "smoke-header").cookie(new Cookie("session", "smoke-cookie")).contentType("application/json").content(JsonUtils.writeValueAsString(smoke))).andReturn().getResponse();
            assertEquals(200, smoked.getStatus());
            assertEquals("smoke-header", smoked.getHeader("X-Result"));
            assertTrue(smoked.getHeaders("Set-Cookie").contains("echo=smoke-cookie; Path=/"));
            assertEquals(Map.of("source", "saved", "parameter", "smoke", "body", Map.of("name", "smoke")), JsonUtils.readValue(smoked.getContentAsString(), Map.class));
            assertEquals(List.of("admin-before", "api-before", "api-after", "admin-after"), calls);

            var saved = dataway.getAdminService().getApiById("draft");
            assertEquals(1, saved.getRevision());
            assertEquals(savedScript, dataway.getAdminService().getDraftByApi("draft").getScript());
            assertTrue(dataway.getAdminService().getHistoryByApi("draft").isEmpty());
        });
    }

    private ApiDefinition draft(String id, ApiScriptType type, String script) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(id);
        definition.setMethod("POST");
        definition.setPath("/" + id);
        definition.setType(type);
        definition.setScript(script);
        definition.setDescription("");
        return definition;
    }

    @Test
    void smokeRejectsStaleOrMissingDraftsBeforeCreatingAQuery() {
        AtomicInteger queries = new AtomicInteger();
        DatawayConfig config = new DatawayConfig().configureQuery(builder -> queries.incrementAndGet());
        Dataway dataway = new Dataway(config.dataAccessLayer(TestDatabase.dataAccessLayer()));
        ApiDefinition draft = this.draft("draft", ApiScriptType.DATA_QL, "return 'saved';");
        dataway.getAdminService().save(draft, 0);
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            var stale = assertThrows(ServletException.class, () -> mvc.perform(post("/dataway/api/smoke").contentType("application/json").content("{\"id\":\"draft\",\"version\":0,\"requestBody\":{}}")));
            assertEquals(409, assertInstanceOf(DatawayException.class, stale.getCause()).status());
            var missing = assertThrows(ServletException.class, () -> mvc.perform(post("/dataway/api/smoke").contentType("application/json").content("{\"id\":\"missing\",\"version\":0,\"requestBody\":{}}")));
            assertEquals(404, assertInstanceOf(DatawayException.class, missing.getCause()).status());
            assertEquals(0, queries.get());
        });
    }

    @Test
    void fragmentQueriesUseSavedDeclarationsOrPreviewParametersAndQueryOptions() {
        DatawayConfig config = new DatawayConfig().resultStructure(false).fragment("sql", () -> (hints, parameters, script) -> {
            assertEquals("SELECT :value", script);
            return parameters;
        });
        Dataway dataway = new Dataway(config.dataAccessLayer(TestDatabase.dataAccessLayer()));
        ApiDefinition draft = this.draft("sql", ApiScriptType.SQL, "SELECT :value");
        draft.setSample("{\"requestBody\":\"{\\\"value\\\":0}\"}");
        dataway.getAdminService().save(draft, 0);
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            var smoked = mvc.perform(post("/dataway/api/smoke").contentType("application/json").content("{\"id\":\"sql\",\"version\":1,\"requestBody\":{\"value\":7,\"extra\":9}}")).andReturn().getResponse();
            assertEquals(Map.of("value", 7), JsonUtils.readValue(smoked.getContentAsString(), Map.class));

            Map<String, Object> parameters = Map.of("value", 11, "extra", 13);
            for (boolean wrap : List.of(false, true)) {
                Map<String, Object> preview = Map.of("id", "-1", "select", "POST", "apiPath", "/preview", "codeType", "SQL", "codeValue", "SELECT :value", "requestBody", parameters, "optionInfo", Map.of("wrapAllParameters", wrap, "wrapParameterName", "args"));
                var performed = mvc.perform(post("/dataway/api/perform").contentType("application/json").content(JsonUtils.writeValueAsString(preview))).andReturn().getResponse();
                assertEquals(200, performed.getStatus());
                assertEquals(wrap ? Map.of("args", parameters) : parameters, JsonUtils.readValue(performed.getContentAsString(), Map.class));
            }

            var failed = mvc.perform(post("/dataway/api/perform").contentType("application/json").content(JsonUtils.writeValueAsString(Map.of("id", "-1", "select", "POST", "apiPath", "/preview", "codeType", "DataQL", "codeValue", "throw 409, 'conflict';", "optionInfo", Map.of("resultStructure", true))))).andReturn().getResponse();
            var failure = JsonUtils.readTree(failed.getContentAsString());
            assertFalse(failure.get("success").booleanValue());
            assertEquals(409, failure.get("code").intValue());
            assertEquals("conflict", failure.get("value").stringValue());
            assertEquals(1, dataway.getAdminService().list().size());
        });
    }
}

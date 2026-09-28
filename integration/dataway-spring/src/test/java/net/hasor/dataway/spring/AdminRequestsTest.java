/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.ServletException;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.DatawayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class AdminRequestsTest {
    private final WebApplicationContextRunner context = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(DatawayAutoConfiguration.class, WebMvcAutoConfiguration.class)).withPropertyValues("dataway.admin-enabled=true", "dataway.api-enabled=true");

    @ParameterizedTest
    @CsvSource({ "api-list,GET", "api-info,GET", "api-detail,GET", "api-history,GET", "get-history,GET", "save-api,POST", "perform,POST", "smoke,POST", "publish,POST", "disable,POST", "delete,POST" })
    void duplicateQueryParametersAreRejectedByEveryController(String action, String method) {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).createDataway();
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            String path = "/dataway/api/" + action;
            MockHttpServletRequestBuilder request = "GET".equals(method) ? get(path) : post(path);
            request.queryParam("id", "one", "two").contentType("application/json").content("{}");

            ServletException failure = assertThrows(ServletException.class, () -> mvc.perform(request));
            DatawayException error = assertInstanceOf(DatawayException.class, failure.getCause());
            assertEquals(400, error.status());
            assertEquals("Duplicate query parameter: id", error.getMessage());
        });
    }

    @Test
    void conflictingQueryAndBodyIdsAreRejectedBeforeDeletion() {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).createDataway();
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            var request = this.json("delete", Map.of("id", "body-id", "version", 1)).queryParam("id", "query-id");

            ServletException failure = assertThrows(ServletException.class, () -> mvc.perform(request));
            DatawayException error = assertInstanceOf(DatawayException.class, failure.getCause());
            assertEquals(400, error.status());
            assertEquals("Conflicting API ids", error.getMessage());
        });
    }

    @Test
    void managementRequestsPreserveConsoleDocumentsAndPublicationLifecycle() {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).resultStructure(false).createDataway();
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            JsonNode saved = this.request(mvc, this.save("-1", 0, "/sample", "return 'first';"));
            String apiID = saved.get("result").stringValue();
            assertEquals(1, saved.get("version").longValue());
            assertEquals(1, this.request(mvc, get("/dataway/api/api-list")).get("result").size());
            JsonNode summary = this.request(mvc, get("/dataway/api/api-list")).get("result").get(0);
            assertEquals(7, summary.size());
            assertFalse(summary.get("checked").booleanValue());
            assertEquals(apiID, summary.get("id").stringValue());
            assertEquals("GET", summary.get("select").stringValue());
            assertEquals("/sample", summary.get("path").stringValue());
            assertEquals("example", summary.get("comment").stringValue());
            assertFalse(summary.has("script"));
            assertFalse(summary.has("codeInfo"));
            assertEquals(0, summary.get("status").intValue());
            assertEquals(1, summary.get("version").longValue());
            assertNull(dataway.getAdminService().list().getFirst().getScript());

            JsonNode detail = this.request(mvc, get("/dataway/api/api-detail").queryParam("id", apiID)).get("result");
            JsonNode info = this.request(mvc, get("/dataway/api/api-info").queryParam("id", apiID)).get("result");
            assertEquals(detail, info);
            assertEquals(13, detail.size());
            assertEquals(3, detail.get("codeInfo").size());
            assertEquals(detail.get("requestBody"), detail.get("codeInfo").get("requestBody"));
            assertEquals(detail.get("headerData"), detail.get("codeInfo").get("headerData"));
            assertEquals("return 'first';", detail.get("codeInfo").get("codeValue").stringValue());
            assertEquals("DataQL", detail.get("codeType").stringValue());
            assertEquals("example", detail.get("apiComment").stringValue());
            assertEquals("demo", detail.get("headerData").get(0).get("value").stringValue());
            assertEquals("object", detail.get("schema").get("type").stringValue());
            assertFalse(detail.get("optionData").get("resultStructure").booleanValue());
            assertEquals(Map.of("value", "sample"), JsonUtils.readValue(detail.get("requestBody").stringValue(), Map.class));
            assertEquals(0, this.request(mvc, get("/dataway/api/api-history").queryParam("id", apiID)).get("result").size());

            assertEquals(2, this.request(mvc, this.action("publish", apiID, 1)).get("version").longValue());
            assertEquals("first", this.request(mvc, get("/api/sample")).stringValue());
            JsonNode history = this.request(mvc, get("/dataway/api/api-history").queryParam("id", apiID)).get("result");
            assertEquals(3, history.get(0).size());
            String firstID = history.get(0).get("historyId").stringValue();
            assertEquals(1, history.get(0).get("status").intValue());
            assertFalse(history.get(0).get("time").stringValue().isBlank());
            assertEquals(3, this.request(mvc, this.save(apiID, 2, "/sample", "return 'second';")).get("version").longValue());
            assertEquals(2, this.request(mvc, get("/dataway/api/api-list")).get("result").get(0).get("status").intValue());
            assertEquals("first", this.request(mvc, get("/api/sample")).stringValue());
            assertEquals("second", this.request(mvc, this.json("smoke", Map.of("id", apiID, "version", 3, "requestBody", Map.of()))).stringValue());
            JsonNode historical = this.request(mvc, get("/dataway/api/get-history").queryParam("id", apiID).queryParam("historyId", firstID)).get("result");
            assertEquals("return 'first';", historical.get("codeInfo").get("codeValue").stringValue());
            assertEquals(3, historical.get("version").longValue());
            assertEquals(2, historical.get("status").intValue());

            assertEquals(4, this.request(mvc, this.action("disable", apiID, 3)).get("version").longValue());
            assertEquals(3, this.request(mvc, get("/dataway/api/api-detail").queryParam("id", apiID)).get("result").get("status").intValue());
            assertEquals(3, this.request(mvc, get("/dataway/api/api-history").queryParam("id", apiID)).get("result").get(0).get("status").intValue());
            this.error(mvc, get("/api/sample"), 404);
            assertEquals(firstID, dataway.getAdminService().getReleaseByApi(apiID).getId());

            assertEquals(5, this.request(mvc, this.action("publish", apiID, 4)).get("version").longValue());
            history = this.request(mvc, get("/dataway/api/api-history").queryParam("id", apiID)).get("result");
            assertEquals(2, history.size());
            assertEquals(1, history.get(0).get("status").intValue());
            assertEquals(3, history.get(1).get("status").intValue());
            assertEquals(firstID, history.get(1).get("historyId").stringValue());
            assertEquals("second", this.request(mvc, get("/api/sample")).stringValue());
            assertTrue(this.request(mvc, this.action("delete", apiID, 5)).get("result").booleanValue());
            assertEquals(0, this.request(mvc, get("/dataway/api/api-list")).get("result").size());
            this.error(mvc, get("/dataway/api/api-detail").queryParam("id", apiID), 404);
            this.error(mvc, get("/dataway/api/get-history").queryParam("id", apiID).queryParam("historyId", firstID), 404);
            this.error(mvc, get("/api/sample"), 404);
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " \t\n", "null" })
    void emptyDocumentsSupportSavingPreviewAndSmoke(String document) {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).resultStructure(false).createDataway();
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            String value = JsonUtils.writeValueAsString(document);
            String input = """
                    {"id":"-1","version":0,"select":"POST","apiPath":"/empty","codeType":"DataQL",
                     "codeValue":"return 7;","requestBody":%1$s,"sample":%1$s,"schema":%1$s,"optionInfo":%1$s}
                    """.formatted(value);
            JsonNode saved = this.request(mvc, post("/dataway/api/save-api").contentType("application/json").content(input));
            String apiID = saved.get("result").stringValue();
            JsonNode detail = this.request(mvc, get("/dataway/api/api-detail").queryParam("id", apiID)).get("result");
            assertEquals("{}", detail.get("requestBody").stringValue());
            assertEquals(0, detail.get("schema").size());
            assertEquals(0, detail.get("optionData").size());

            JsonNode preview = this.request(mvc, post("/dataway/api/perform").contentType("application/json").content(input));
            assertEquals(7, preview.intValue());
            String smoke = """
                    {"id":"%s","version":1,"requestBody":%s}
                    """.formatted(apiID, value);
            JsonNode result = this.request(mvc, post("/dataway/api/smoke").contentType("application/json").content(smoke));
            assertEquals(7, result.intValue());
            assertEquals(1, dataway.getAdminService().getApiById(apiID).getRevision());
        });
    }

    @ParameterizedTest
    @CsvSource({ "requestBody,[", "requestBody,[]", "sample,[", "schema,[]", "optionInfo,1" })
    void invalidDocumentsDoNotSaveOrExecute(String field, String document) {
        AtomicInteger queries = new AtomicInteger();
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).configureQuery(builder -> queries.incrementAndGet()).createDataway();
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            Map<String, Object> input = new LinkedHashMap<>(Map.of("id", "-1", "version", 0, "select", "POST", "apiPath", "/invalid", "codeType", "DataQL", "codeValue", "return 7;"));
            input.put(field, document);
            this.error(mvc, this.json("save-api", input), 400);
            this.error(mvc, this.json("perform", input), 400);
            assertTrue(dataway.getAdminService().list().isEmpty());
            assertEquals(0, queries.get());
        });
    }

    private JsonNode request(MockMvc mvc, MockHttpServletRequestBuilder request) throws Exception {
        var response = mvc.perform(request).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        return JsonUtils.readTree(response.getContentAsString());
    }

    private MockHttpServletRequestBuilder save(String apiID, long version, String path, String script) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", apiID);
        body.put("version", version);
        body.put("select", "GET");
        body.put("apiPath", path);
        body.put("codeType", "DataQL");
        body.put("codeValue", script);
        body.put("comment", "example");
        body.put("requestBody", Map.of("value", "sample"));
        body.put("headerData", List.of(Map.of("checked", true, "name", "X-Example", "value", "demo")));
        body.put("schema", Map.of("type", "object"));
        body.put("optionInfo", Map.of("resultStructure", false));
        return this.json("save-api", body);
    }

    private MockHttpServletRequestBuilder json(String action, Map<String, ?> body) {
        return post("/dataway/api/" + action).contentType("application/json").content(JsonUtils.writeValueAsString(body));
    }

    private MockHttpServletRequestBuilder action(String action, String apiID, long version) {
        return this.json(action, Map.of("id", apiID, "version", version));
    }

    private void error(MockMvc mvc, MockHttpServletRequestBuilder request, int status) {
        ServletException failure = assertThrows(ServletException.class, () -> mvc.perform(request));
        DatawayException cause = assertInstanceOf(DatawayException.class, failure.getCause());
        assertEquals(status, cause.status());
    }

    @Test
    void historyRequestsCannotReadAnotherApisPublication() {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).createDataway();
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            String first = this.request(mvc, this.save("-1", 0, "/first", "return 'first';")).get("result").stringValue();
            String second = this.request(mvc, this.save("-1", 0, "/second", "return 'second';")).get("result").stringValue();
            this.request(mvc, this.action("publish", first, 1));
            this.request(mvc, this.action("publish", second, 1));
            String releaseID = this.request(mvc, get("/dataway/api/api-history").queryParam("id", second)).get("result").get(0).get("historyId").stringValue();
            this.error(mvc, get("/dataway/api/get-history").queryParam("id", first).queryParam("historyId", releaseID), 404);
            this.error(mvc, get("/dataway/api/get-history").queryParam("id", first), 404);
            this.error(mvc, get("/dataway/api/get-history").queryParam("id", first).queryParam("historyId", "missing"), 404);
            JsonNode document = this.request(mvc, get("/dataway/api/get-history").queryParam("id", second).queryParam("historyId", releaseID)).get("result");
            assertEquals(second, document.get("id").stringValue());
            assertEquals("return 'second';", document.get("codeInfo").get("codeValue").stringValue());
        });
    }

    @Test
    void staleCommandsAndSmokeDoNotChangeOrExecuteNewerDrafts() {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).createDataway();
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            String apiID = this.request(mvc, this.save("-1", 0, "/versioned", "return 'saved';")).get("result").stringValue();
            this.request(mvc, this.action("publish", apiID, 1));
            this.error(mvc, this.save(apiID, 1, "/versioned", "return 'stale';"), 409);
            for (String action : List.of("publish", "disable", "delete")) {
                this.error(mvc, this.action(action, apiID, 1), 409);
            }
            this.error(mvc, this.json("smoke", Map.of("id", apiID, "version", 1, "requestBody", Map.of())), 409);
            this.error(mvc, this.save(apiID, 2, "/different", "return 'renamed';"), 409);
            JsonNode detail = this.request(mvc, get("/dataway/api/api-detail").queryParam("id", apiID)).get("result");
            assertEquals(2, detail.get("version").longValue());
            assertEquals(1, detail.get("status").intValue());
            assertEquals("return 'saved';", detail.get("codeInfo").get("codeValue").stringValue());
            assertEquals(1, this.request(mvc, get("/dataway/api/api-history").queryParam("id", apiID)).get("result").size());
        });
    }

    @Test
    void compositeRequestsAreAuthorizedAndInterceptedOnce() {
        UserIdentity identity = UserIdentity.authenticated("editor");
        List<Operation> calls = new ArrayList<>();
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> identity).authorizationCheck((user, operation) -> operation != Operation.DELETE).adminInterceptor((action, next) -> {
            assertSame(identity, action.getIdentity());
            calls.add(action.getOperation());
            if (action.getOperation() == Operation.DISABLE) {
                return Map.of("intercepted", true);
            }

            return next.proceed();
        }).createDataway();
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            String apiID = this.request(mvc, this.save("-1", 0, "/intercepted", "return 1;")).get("result").stringValue();
            this.request(mvc, this.action("publish", apiID, 1));
            this.request(mvc, get("/dataway/api/api-list"));
            this.request(mvc, get("/dataway/api/api-detail").queryParam("id", apiID));
            this.request(mvc, get("/dataway/api/api-history").queryParam("id", apiID));
            assertEquals(List.of(Operation.SAVE, Operation.PUBLISH, Operation.LIST, Operation.READ, Operation.HISTORY), calls);
            assertTrue(this.request(mvc, this.action("disable", apiID, 2)).get("intercepted").booleanValue());
            assertTrue(dataway.getAdminService().getApiById(apiID).isEnabled());
            assertEquals(2, dataway.getAdminService().getVersionById(apiID));
            this.error(mvc, this.action("delete", apiID, 2), 401);
            assertEquals(Operation.DISABLE, calls.getLast());
            assertEquals(6, calls.size());
        });
    }

    @ParameterizedTest
    @CsvSource({ "api-detail,3", "api-info,3", "api-list,3", "api-history,3", "get-history,3", "smoke,1" })
    void independentlyLoadedDataCannotBeCombinedAcrossRevisions(String action, int changeAt) {
        ApiDataAccessLayer stored = TestDatabase.dataAccessLayer();
        ApiDataAccessLayer access = spy(stored);
        Dataway dataway = new DatawayConfig().dataAccessLayer(access).createDataway();
        this.context.withBean(Dataway.class, () -> dataway).run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            String apiID = this.request(mvc, this.save("-1", 0, "/race", "return 'old';")).get("result").stringValue();
            this.request(mvc, this.action("publish", apiID, 1));
            String historyID = dataway.getAdminService().getReleaseByApi(apiID).getId();
            AtomicInteger reads = new AtomicInteger();
            doAnswer(invocation -> {
                Object row = invocation.callRealMethod();
                if (reads.incrementAndGet() == changeAt) {
                    stored.updateObject(EntityType.INFO, apiID, 2, Map.of(FieldDef.SCRIPT, "return 'changed';"));
                }
                return row;
            }).when(access).getObject(EntityType.INFO, apiID);

            MockHttpServletRequestBuilder request;
            if ("smoke".equals(action)) {
                request = this.json(action, Map.of("id", apiID, "version", 2, "requestBody", Map.of()));
            } else {
                request = get("/dataway/api/" + action).queryParam("id", apiID).queryParam("historyId", historyID);
            }
            this.error(mvc, request, 409);
            assertEquals(3, dataway.getAdminService().getVersionById(apiID));
            assertEquals("return 'changed';", dataway.getAdminService().getDraftByApi(apiID).getScript());
        });
    }
}

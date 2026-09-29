/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase.admin;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.spring.testcase.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

class ApiLifecycleTest {
    @Test
    void consoleRequestsPublishDebugDisableAndDeleteThroughNativeMvc() throws Throwable {
        List<String> events = new ArrayList<>();
        var config = this.configure(TestSettings.configuration(), events);
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(config, database.access, TestSettings.enabled()); HttpClient host = new HttpClient(app.baseUrl())) {
            assertEquals(200, host.login("admin").status);
            Map<String, Object> draft = new LinkedHashMap<>();
            draft.put("id", "-1");
            draft.put("version", 0);
            draft.put("select", "POST");
            draft.put("apiPath", "/managed");
            draft.put("codeType", "DataQL");
            draft.put("codeValue", "return ${value} + 1;");
            draft.put("comment", "Managed through MVC");
            draft.put("requestBody", Map.of("value", 0));
            draft.put("optionInfo", Map.of("resultStructure", false));
            JsonNode saved = this.send(host, "POST", "/admin/api/save-api", draft, 200);
            assertTrue(saved.path("success").asBoolean());
            assertEquals(1, saved.path("version").asLong());
            String id = saved.path("result").asText();
            assertFalse(id.isBlank());
            this.send(host, "POST", "/api/managed", Map.of("value", 9), 404);
            JsonNode detail = this.send(host, "GET", "/admin/api/api-detail?id=" + id, null, 200).path("result");
            assertEquals(id, detail.path("id").asText());
            assertEquals(1, detail.path("version").asLong());
            assertEquals(1, this.send(host, "GET", "/admin/api/api-list", null, 200).path("result").size());
            JsonNode info = this.send(host, "GET", "/admin/api/api-info?id=" + id, null, 200).path("result");
            assertEquals("POST", info.path("select").asText());
            assertEquals("return ${value} + 1;", info.path("codeInfo").path("codeValue").asText());

            events.clear();
            assertEquals(10, this.send(host, "POST", "/admin/api/smoke", Map.of("id", id, "version", 1, "requestBody", Map.of("value", 9)), 200).asInt());
            assertEquals(List.of("admin-before", "api-before", "api-after", "admin-after"), events);
            assertEquals(2, this.send(host, "POST", "/admin/api/publish", Map.of("id", id, "version", 1), 200).path("version").asLong());
            assertEquals(10, this.send(host, "POST", "/api/managed", Map.of("value", 9), 200).asInt());

            draft.put("id", id);
            draft.put("version", 2);
            draft.put("codeValue", "return ${value} + 2;");
            assertEquals(3, this.send(host, "POST", "/admin/api/save-api", draft, 200).path("version").asLong());
            assertEquals(10, this.send(host, "POST", "/api/managed", Map.of("value", 9), 200).asInt());
            assertEquals(11, this.send(host, "POST", "/admin/api/smoke", Map.of("id", id, "version", 3, "requestBody", Map.of("value", 9)), 200).asInt());
            this.send(host, "POST", "/admin/api/publish", Map.of("id", id, "version", 2), 409);
            assertEquals(4, this.send(host, "POST", "/admin/api/publish", Map.of("id", id, "version", 3), 200).path("version").asLong());
            assertEquals(11, this.send(host, "POST", "/api/managed", Map.of("value", 9), 200).asInt());
            JsonNode history = this.send(host, "GET", "/admin/api/api-history?id=" + id, null, 200).path("result");
            assertEquals(2, history.size());
            String historyId = history.get(1).path("historyId").asText();
            JsonNode original = this.send(host, "GET", "/admin/api/get-history?id=" + id + "&historyId=" + historyId, null, 200).path("result");
            assertEquals("return ${value} + 1;", original.path("codeInfo").path("codeValue").asText());
            this.send(host, "GET", "/admin/api/get-history?id=" + id + "&historyId=missing", null, 404);
            this.send(host, "GET", "/admin/api/publish", null, 405);
            this.send(host, "GET", "/admin/api/not-an-operation", null, 404);
            assertEquals(5, this.send(host, "POST", "/admin/api/disable", Map.of("id", id, "version", 4), 200).path("version").asLong());
            this.send(host, "POST", "/api/managed", Map.of("value", 9), 404);
            this.send(host, "POST", "/admin/api/delete", Map.of("id", id, "version", 5), 200);
            assertTrue(this.send(host, "GET", "/admin/api/api-list", null, 200).path("result").isEmpty());
            assertEquals(0, database.count("interface_info"));
            assertEquals(0, database.count("interface_release"));
        }
    }

    private DatawayConfig configure(DatawayConfig config, List<String> events) {
        return config.adminInterceptor((context, chain) -> {
            assertEquals("admin", context.identity().identityId());
            events.add("admin-before");
            Object result = chain.proceed();
            events.add("admin-after");
            return result;
        }).apiInterceptor((context, chain) -> {
            events.add("api-before");
            Object result = chain.proceed(context);
            events.add("api-after");
            return result;
        });
    }

    private JsonNode send(HttpClient client, String method, String path, Map<String, ?> body, int status) throws Throwable {
        HttpResult response = body == null ? client.send(method, path, null) : client.json(path, body);
        assertEquals(status, response.status, response.text());
        return JsonUtils.readTree(response.text());
    }
}

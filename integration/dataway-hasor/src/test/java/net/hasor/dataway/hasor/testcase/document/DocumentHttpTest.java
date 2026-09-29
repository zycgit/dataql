/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase.document;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.hasor.testcase.H2Database;
import net.hasor.dataway.hasor.testcase.HttpClient;
import net.hasor.dataway.hasor.testcase.TestApplication;
import net.hasor.dataway.hasor.testcase.TestSettings;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentHttpTest {
    @Test
    void swaggerAndOpenapiDescribeOnlyPublishedApisWithoutExecutingThem() throws Throwable {
        AtomicInteger executions = new AtomicInteger();
        var config = TestSettings.configuration().documentTitle("Example APIs").documentVersion("2.0").documentServer("/gateway/api").apiInterceptor((context, chain) -> {
            executions.incrementAndGet();
            return chain.proceed(context);
        });
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(config, database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/published", "return 1;");
            var draft = new ApiDefinition();
            draft.setId("unpublished");
            draft.setMethod("POST");
            draft.setPath("/draft");
            draft.setType(ApiScriptType.DATA_QL);
            draft.setScript("return 2;");
            draft.setDescription("Unpublished draft");
            draft.setSchema("{}");
            draft.setSample("{}");
            draft.setOptions("{}");
            app.dataway().getAdminService().save(draft, 0);
            assertEquals(401, client.get("/docs/openapi.json").status);
            assertEquals(200, client.login("api").status);
            for (String file : new String[] { "swagger2.json", "openapi.json" }) {
                var response = client.get("/docs/" + file);
                assertEquals(200, response.status, response.text());
                assertTrue(response.headers.get("Content-Type").startsWith("application/json"));
                var document = JsonUtils.readTree(response.text());
                assertEquals("Example APIs", document.path("info").path("title").asText());
                assertEquals("2.0", document.path("info").path("version").asText());
                assertEquals(1, document.path("paths").size());
                assertTrue(document.path("paths").path("/published").has("get"));
                if (file.equals("swagger2.json")) {
                    assertEquals("2.0", document.path("swagger").asText());
                    assertEquals("/gateway/api", document.path("basePath").asText());
                } else {
                    assertTrue(document.path("openapi").asText().startsWith("3."));
                    assertEquals("/gateway/api", document.path("servers").get(0).path("url").asText());
                }
                var head = client.send("HEAD", "/docs/" + file, null);
                assertEquals(200, head.status);
                assertEquals(0, head.bytes.length);
                assertEquals(405, client.json("/docs/" + file, Map.of()).status);
            }
            assertEquals(0, executions.get());
            assertEquals(404, client.get("/docs/unknown.json").status);
            assertEquals(200, client.login("admin").status);
            String apiID = app.dataway().getAdminService().list().stream().filter(api -> api.getPath().equals("/published")).findFirst().orElseThrow().getId();
            assertEquals(200, client.json("/admin/api/disable", Map.of("id", apiID, "version", 2)).status);
            assertTrue(JsonUtils.readTree(client.get("/docs/openapi.json").text()).path("paths").isEmpty());
        }
    }
}

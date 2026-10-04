/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.example.testcase;
import java.util.UUID;
import net.hasor.boot.Boot;
import net.hasor.boot.BootApplication;
import net.hasor.boot.web.WebServer;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.hasor.example.config.DatabaseConfiguration;
import net.hasor.dataway.hasor.example.config.DatawayConfiguration;
import net.hasor.dataway.hasor.example.config.MetadataConfiguration;
import net.hasor.dataway.hasor.example.config.WebConfiguration;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies article examples through a real Hasor Boot HTTP server and H2 databases. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlogApiTest {
    private final OkHttpClient http = new OkHttpClient();
    private BootApplication application;
    private String baseUrl;
    private String cookie;

    @BeforeAll
    void start() throws Exception {
        String database = "jdbc:h2:mem:" + UUID.randomUUID();
        this.application = new Boot().hconfigFile("hconfig.xml")
                .sources(DatabaseConfiguration.class, MetadataConfiguration.class, DatawayConfiguration.class, WebConfiguration.class)
                .property("hasor.boot.web.connectors.http.port", 0)
                .property("example.database.main.url", database + "-main")
                .property("example.database.ds1.url", database + "-ds1")
                .property("example.database.ds2.url", database + "-ds2")
                .start();
        int port = this.application.getAppContext().getInstance(WebServer.class).getPort();
        this.baseUrl = "http://127.0.0.1:" + port;
        FormBody form = new FormBody.Builder().add("username", "admin").add("password", "example-password").build();
        try (Response response = this.http.newCall(new Request.Builder().url(this.baseUrl + "/session/login").post(form).build()).execute()) {
            assertEquals(200, response.code());
            this.cookie = response.header("Set-Cookie").split(";", 2)[0];
        }
    }

    @AfterAll
    void stop() throws Exception {
        if (this.application != null) {
            this.application.close();
        }
        this.http.connectionPool().evictAll();
        this.http.dispatcher().executorService().shutdownNow();
    }

    @Test
    void sqlQueryReturnsPublishedResults() throws Exception {
        JsonNode rows = this.post("spring-query", "{}");
        assertEquals(2, rows.size());
        assertEquals("Alice", rows.get(0).get("name").asString());
    }

    private JsonNode post(String name, String json) throws Exception {
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        Request request = new Request.Builder().url(this.baseUrl + "/api/blog/" + name).header("Cookie", this.cookie).post(body).build();
        try (Response response = this.http.newCall(request).execute()) {
            assertEquals(200, response.code());
            return JsonUtils.readTree(response.body().string());
        }
    }

    @Test
    void parametersBindValuesAndRenameFields() throws Exception {
        JsonNode result = this.post("parameters", "{\"name\":\"Ali\"}");
        assertEquals(1, result.get("users").size());
        assertEquals("Alice", result.get("users").get(0).get("userName").asString());
        assertTrue(this.post("parameters", "{\"name\":\"' OR 1=1 --\"}").get("users").isEmpty());
    }

    @Test
    void paginationReturnsBothPagesAndTotal() throws Exception {
        JsonNode first = this.post("pagination", "{\"page\":1,\"size\":1}");
        JsonNode second = this.post("pagination", "{\"page\":2,\"size\":1}");
        assertEquals("Alice", first.get("items").get(0).get("name").asString());
        assertEquals("Bob", second.get("items").get(0).get("name").asString());
        assertEquals(2, first.get("pagination").get("totalCount").asInt());
    }

    @Test
    void responseTemplateHandlesSuccessAndScriptFailure() throws Exception {
        JsonNode ok = this.post("response-format", "{\"message\":\"Hello\",\"fail\":false}");
        assertTrue(ok.get("ok").asBoolean());
        assertEquals("Hello", ok.get("data").get("message").asString());
        JsonNode failure = this.post("response-format", "{\"message\":\"Hello\",\"fail\":true}");
        assertFalse(failure.get("ok").asBoolean());
        assertEquals(400, failure.get("code").asInt());
    }

    @Test
    void swaggerDocumentsAndUiExposeThePublishedApi() throws Exception {
        JsonNode document = this.getJson("/docs/swagger2.json");
        assertEquals("2.0", document.get("swagger").asString());
        assertNotNull(document.get("paths").get("/blog/swagger").get("post"));
        assertNotNull(this.getJson("/docs/openapi.json").get("openapi"));
        assertEquals("Hello Swagger", this.post("swagger", "{\"message\":\"Hello Swagger\"}").get("message").asString());
        try (Response response = this.http.newCall(new Request.Builder().url(this.baseUrl + "/swagger/initializer.js").header("Cookie", this.cookie).build()).execute()) {
            assertEquals(200, response.code());
            assertTrue(response.body().string().contains("configuration.swagger"));
        }
    }

    private JsonNode getJson(String path) throws Exception {
        try (Response response = this.http.newCall(new Request.Builder().url(this.baseUrl + path).header("Cookie", this.cookie).build()).execute()) {
            assertEquals(200, response.code());
            return JsonUtils.readTree(response.body().string());
        }
    }

    @Test
    void headersAreCaseInsensitiveAndCopiedToTheResponse() throws Exception {
        Request request = new Request.Builder().url(this.baseUrl + "/api/blog/headers")
                .header("Cookie", this.cookie).header("x-trace-id", "blog-001")
                .post(RequestBody.create("{\"message\":\"Hello Header\"}", MediaType.get("application/json"))).build();
        try (Response response = this.http.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertEquals("blog-001", response.header("X-Trace-Id"));
            assertEquals("blog-001", JsonUtils.readTree(response.body().string()).get("traceId").asString());
        }
    }
}

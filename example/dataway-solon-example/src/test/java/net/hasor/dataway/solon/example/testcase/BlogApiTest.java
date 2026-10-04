/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.testcase;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.UUID;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.solon.example.ExampleApplication;
import okhttp3.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.noear.solon.SimpleSolonApp;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies article examples through a real Solon HTTP server and H2 databases. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlogApiTest {
    private final OkHttpClient   http = new OkHttpClient();
    private       SimpleSolonApp application;
    private       String         baseUrl;
    private       String         cookie;

    @BeforeAll
    void start() throws Throwable {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        String database = "jdbc:h2:mem:" + UUID.randomUUID();
        this.application = new SimpleSolonApp(ExampleApplication.class, "--cfg=app.properties", "--server.port=" + port, "--example.database.main.url=" + database + "-main", "--example.database.ds1.url=" + database + "-ds1", "--example.database.ds2.url=" + database + "-ds2").globalize(true);
        this.application.start(app -> app.enableHttp(true));
        this.baseUrl = "http://127.0.0.1:" + port;
        FormBody form = new FormBody.Builder().add("username", "admin").add("password", "example-password").build();
        try (Response response = this.http.newCall(new Request.Builder().url(this.baseUrl + "/session/login").post(form).build()).execute()) {
            assertEquals(200, response.code());
            this.cookie = response.header("Set-Cookie").split(";", 2)[0];
        }
    }

    @AfterAll
    void stop() {
        if (this.application != null) {
            this.application.stop();
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
        Request request = new Request.Builder().url(this.baseUrl + "/api/blog/headers").header("Cookie", this.cookie).header("x-trace-id", "blog-001").post(RequestBody.create("{\"message\":\"Hello Header\"}", MediaType.get("application/json"))).build();
        try (Response response = this.http.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertEquals("blog-001", response.header("X-Trace-Id"));
            assertEquals("blog-001", JsonUtils.readTree(response.body().string()).get("traceId").asString());
        }
    }
}

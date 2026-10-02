/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.nacos.testcase;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import javax.imageio.ImageIO;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.nacos.NacosSnapshot;
import net.hasor.dataway.spring.nacos.example.ExampleApplication;
import okhttp3.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the packaged dependencies through real Spring MVC and Nacos transports. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NacosExampleTest {
    // Management lists read metadata for every API through the real Nacos transport.
    private final OkHttpClient                   http   = new OkHttpClient.Builder().followRedirects(false).readTimeout(Duration.ofSeconds(30)).callTimeout(Duration.ofSeconds(60)).build();
    private final String                         dataId = "dataway-test-" + UUID.randomUUID();
    private       NacosTestServer                nacos;
    private       ConfigurableApplicationContext application;
    private       ConfigService                  observer;
    private       String                         baseUrl;
    private       Path                           uploads;

    @BeforeAll
    void start() throws Exception {
        try {
            this.nacos = new NacosTestServer(Path.of("target"));
            this.uploads = this.nacos.directory().resolve("uploads");
            this.application = SpringApplication.run(ExampleApplication.class,//
                    "--spring.config.location=classpath:application.yml", "--server.port=0",//
                    "--example.nacos.server-addr=" + this.nacos.serverAddress(), "--example.nacos.data-id=" + this.dataId,//
                    "--example.upload.directory=" + this.uploads);
            this.baseUrl = "http://127.0.0.1:" + this.application.getEnvironment().getRequiredProperty("local.server.port");
            Properties properties = new Properties();
            properties.setProperty("serverAddr", this.nacos.serverAddress());
            this.observer = NacosFactory.createConfigService(properties);
        } catch (Exception failure) {
            this.stop();
            throw failure;
        }
    }

    @AfterAll
    void stop() throws Exception {
        try {
            if (this.application != null) {
                this.application.close();
                this.application = null;
            }
        } finally {
            try {
                if (this.observer != null) {
                    this.observer.shutDown();
                    this.observer = null;
                }
            } finally {
                if (this.nacos != null) {
                    this.nacos.close();
                }
                this.http.dispatcher().executorService().shutdownNow();
                this.http.connectionPool().evictAll();
            }
        }
    }

    @Test
    void loginProvidesTheConfiguredIdentityAndLogoutClearsTheCookie() throws Exception {
        try (Response response = this.request("GET", "/", null, null)) {
            assertEquals(200, response.code());
            assertTrue(response.body().string().contains("Dataway"));
        }
        for (String user : new String[] { "api", "reader", "admin" }) {
            String cookie = this.login(user);
            JsonNode identity = this.post("/session/me", Map.of(), cookie);
            assertEquals(user, identity.path("identity").asText());
            assertTrue(identity.path("authenticated").asBoolean());
            assertEquals(!"api".equals(user), identity.path("consoleAccess").asBoolean());
            assertEquals("admin".equals(user), identity.path("consoleManage").asBoolean());
            assertTrue(identity.path("documentAccess").asBoolean());
            if (!"api".equals(user)) {
                assertFalse(this.get("/admin/api/api-list", cookie).path("result").isEmpty());
            }
            try (Response response = this.request("POST", "/session/logout", RequestBody.create("", null), cookie)) {
                assertEquals(200, response.code());
                assertTrue(response.header("Set-Cookie", "").contains("Max-Age=0"));
            }
        }
    }

    @Test
    void publishedApisExecuteJsonAndSqlAcrossBothDataSources() throws Exception {
        String cookie = this.login("api");
        JsonNode echo = this.post("/api/echo", Map.of("message", "Real Nacos"), cookie);
        assertTrue(echo.path("success").asBoolean());
        assertEquals("Real Nacos", echo.path("value").path("message").asText());
        JsonNode combined = this.post("/api/person-orders", Map.of("id", 1), cookie).path("value");
        assertEquals("Alice", combined.path("person").path("name").asText());
        assertEquals(2, combined.path("orders").size());
        assertEquals("Keyboard", combined.path("orders").get(0).path("product").asText());
    }

    @Test
    void formsAndMultipartUploadsUseTheActualServletRequest() throws Exception {
        String cookie = this.login("api");
        FormBody form = new FormBody.Builder().add("name", "Dataway 表单").add("tag", "one").add("tag", "two").build();
        try (Response response = this.request("POST", "/api/form", form, cookie)) {
            JsonNode value = this.result(response).path("value");
            assertEquals("Dataway 表单", value.path("name").asText());
            assertEquals(2, value.path("tag").size());
            assertEquals("two", value.path("tag").get(1).asText());
        }
        byte[] content = "Dataway upload\n".repeat(6000).getBytes(StandardCharsets.UTF_8);
        MultipartBody multipart = new MultipartBody.Builder().setType(MultipartBody.FORM)//
                .addFormDataPart("title", "Nacos upload")//
                .addFormDataPart("file", "example.txt", RequestBody.create(content, MediaType.get("text/plain"))).build();
        try (Response response = this.request("POST", "/api/upload", multipart, cookie)) {
            JsonNode value = this.result(response).path("value");
            assertEquals("Nacos upload", value.path("title").asText());
            JsonNode file = value.path("file");
            assertEquals("example.txt", file.path("name").asText());
            assertEquals(content.length, file.path("size").asInt());
            assertEquals("text/plain", file.path("contentType").asText());
            assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)), file.path("sha256").asText());
        }
        if (Files.exists(this.uploads)) {
            try (var files = Files.walk(this.uploads)) {
                assertFalse(files.anyMatch(Files::isRegularFile), "Upload temporary files must be released after the request");
            }
        }
    }

    @Test
    void consoleAndSwaggerDescribeThePublishedNacosApis() throws Exception {
        String cookie = this.login("reader");
        try (Response response = this.request("GET", "/admin/", null, cookie)) {
            assertEquals(200, response.code());
            assertTrue(response.body().string().contains("initializer.js"));
        }
        JsonNode openapi = this.get("/docs/openapi.json", cookie);
        assertTrue(openapi.path("openapi").asText().startsWith("3."));
        assertEquals("/api", openapi.path("servers").get(0).path("url").asText());
        assertTrue(openapi.path("paths").path("/echo").has("post"));
        assertTrue(openapi.path("paths").path("/upload").path("post").path("requestBody").path("content").has("multipart/form-data"));
        assertEquals("2.0", this.get("/docs/swagger2.json", cookie).path("swagger").asText());
        try (Response response = this.request("GET", "/swagger/index.html", null, cookie)) {
            assertEquals(200, response.code());
            assertTrue(response.body().string().contains("swagger-ui-bundle.js"));
        }
        try (Response response = this.request("GET", "/swagger/swagger-ui-bundle.js", null, cookie)) {
            assertEquals(200, response.code());
        }
    }

    @Test
    void consoleSavesPublishesUpdatesDisablesAndDeletesRealMetadata() throws Exception {
        String cookie = this.login("admin");
        Map<String, Object> draft = this.draft("/managed", "return ${value} + 1;");
        JsonNode saved = this.post("/admin/api/save-api", draft, cookie);
        assertTrue(saved.path("success").asBoolean());
        String apiID = saved.path("result").asText();
        this.awaitVersion(apiID, 1);
        assertEquals("return ${value} + 1;", this.get("/admin/api/api-info?id=" + apiID, cookie).path("result").path("codeInfo").path("codeValue").asText());
        assertEquals(2, this.post("/admin/api/publish", Map.of("id", apiID, "version", 1), cookie).path("version").asLong());
        this.awaitVersion(apiID, 2);
        assertEquals(10, this.post("/api/managed", Map.of("value", 9), cookie).asInt());

        draft.put("id", apiID);
        draft.put("version", 2);
        draft.put("codeValue", "return ${value} + 2;");
        assertEquals(3, this.post("/admin/api/save-api", draft, cookie).path("version").asLong());
        this.awaitVersion(apiID, 3);
        assertEquals(10, this.post("/api/managed", Map.of("value", 9), cookie).asInt());
        assertEquals(4, this.post("/admin/api/publish", Map.of("id", apiID, "version", 3), cookie).path("version").asLong());
        this.awaitVersion(apiID, 4);
        assertEquals(11, this.post("/api/managed", Map.of("value", 9), cookie).asInt());
        assertEquals(2, this.get("/admin/api/api-history?id=" + apiID, cookie).path("result").size());
        assertTrue(this.get("/docs/openapi.json", cookie).path("paths").has("/managed"));

        assertEquals(5, this.post("/admin/api/disable", Map.of("id", apiID, "version", 4), cookie).path("version").asLong());
        this.awaitVersion(apiID, 5);
        assertFalse(this.get("/docs/openapi.json", cookie).path("paths").has("/managed"));
        this.post("/admin/api/delete", Map.of("id", apiID, "version", 5), cookie);
        this.awaitVersion(apiID, 0);
        assertFalse(this.snapshot().getRecords().get(EntityType.RELEASE).values().stream().anyMatch(row -> apiID.equals(row.get(FieldDef.API_ID))));
    }

    private Map<String, Object> draft(String path, String script) {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("id", "-1");
        draft.put("version", 0);
        draft.put("select", "POST");
        draft.put("apiPath", path);
        draft.put("codeType", "DataQL");
        draft.put("codeValue", script);
        draft.put("comment", "Created through the real console API");
        draft.put("requestBody", Map.of("value", 0));
        draft.put("optionInfo", Map.of("resultHandler", "raw"));
        return draft;
    }

    private void awaitVersion(String apiID, long version) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        do {
            Map<FieldDef, String> row = this.snapshot().getRecords().get(EntityType.INFO).get(apiID);
            if ((version == 0 && row == null) || (row != null && Long.toString(version).equals(row.get(FieldDef.REVISION)))) {
                return;
            }
            Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        fail("Nacos metadata did not reach version " + version + " for API " + apiID);
    }

    private NacosSnapshot snapshot() throws Exception {
        return NacosSnapshot.parse(this.observer.getConfig(this.dataId, "DATAWAY_EXAMPLE", 2000));
    }

    @Test
    void independentNacosClientReadsTheSnapshotAndRejectsAStaleWrite() throws Exception {
        String cookie = this.login("admin");
        String original = this.observer.getConfig(this.dataId, "DATAWAY_EXAMPLE", 2000);
        assertTrue(NacosSnapshot.parse(original).getRecords().get(EntityType.INFO).containsKey("example-echo"));
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(original.getBytes(StandardCharsets.UTF_8)));
        String apiID = this.post("/admin/api/save-api", this.draft("/cas-check", "return 1;"), cookie).path("result").asText();
        this.awaitVersion(apiID, 1);
        assertFalse(this.observer.publishConfigCas(this.dataId, "DATAWAY_EXAMPLE", original, digest));
        assertEquals("return 1;", this.snapshot().getRecords().get(EntityType.INFO).get(apiID).get(FieldDef.SCRIPT));
        this.post("/admin/api/delete", Map.of("id", apiID, "version", 1), cookie);
        this.awaitVersion(apiID, 0);
    }

    @Test
    void resultHandlersAndBinaryUploadsWorkWithNacosMetadata() throws Exception {
        String cookie = this.login("admin");
        assertTrue(this.get("/admin/api/get-handlers", cookie).path("result").toString().contains("csv"));
        assertTrue(this.get("/admin/api/get-handlers", cookie).path("result").toString().contains("verifyCode"));
        Map<String, Object> parameters = Map.of("message", "Hello Dataway");
        JsonNode structured = this.post("/api/result-structure", parameters, cookie);
        assertTrue(structured.path("success").asBoolean());
        assertEquals("Hello Dataway", structured.path("value").path("message").asText());
        JsonNode raw = this.post("/api/result-raw", parameters, cookie);
        assertEquals("Hello Dataway", raw.path("message").asText());
        assertFalse(raw.has("success"));
        RequestBody message = RequestBody.create(JsonUtils.writeValueAsString(parameters), MediaType.get("application/json"));
        try (Response response = this.request("POST", "/api/result-text", message, cookie)) {
            assertEquals(200, response.code());
            assertTrue(response.header("Content-Type").startsWith("text/plain"));
            assertEquals("Hello Dataway", response.body().string());
        }
        RequestBody code = RequestBody.create("{\"text\":\"A7K9\"}", MediaType.get("application/json"));
        try (Response response = this.request("POST", "/api/verifyCode", code, cookie)) {
            assertEquals(200, response.code());
            assertEquals("image/png", response.header("Content-Type").split(";")[0]);
            var image = ImageIO.read(new ByteArrayInputStream(response.body().bytes()));
            assertNotNull(image);
            assertEquals(160, image.getWidth());
            assertEquals(64, image.getHeight());
        }
        RequestBody empty = RequestBody.create("{}", MediaType.get("application/json"));
        try (Response response = this.request("POST", "/api/people-csv", empty, cookie)) {
            assertEquals(200, response.code());
            assertTrue(response.header("Content-Type").startsWith("text/csv"));
            assertTrue(response.body().string().startsWith("id,name,balance\r\n"));
        }
        try (Response response = this.request("POST", "/api/binary", empty, cookie)) {
            assertEquals(200, response.code());
            assertEquals("application/octet-stream", response.header("Content-Type"));
            assertEquals("Hello Dataway", response.body().string());
        }
        byte[] content = new byte[70000];
        new Random(7).nextBytes(content);
        MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", "original.bin", RequestBody.create(content, MediaType.get("application/octet-stream"))).build();
        try (Response response = this.request("POST", "/api/upload-download", body, cookie)) {
            assertEquals(200, response.code());
            assertTrue(response.header("Content-Disposition").contains("original.bin"));
            assertArrayEquals(content, response.body().bytes());
        }
        if (Files.exists(this.uploads)) {
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
                while (true) {
                    try (var files = Files.walk(this.uploads)) {
                        if (files.noneMatch(Files::isRegularFile)) {
                            break;
                        }
                    }
                    Thread.sleep(10);
                }
            });
        }
    }

    @Test
    void verifyCodeUsesScriptTextInPublicCallsAndConsolePreviews() throws Exception {
        String apiCookie = this.login("api");
        RequestBody code = RequestBody.create("{\"text\":\"ABC123\"}", MediaType.get("application/json"));
        try (Response response = this.request("POST", "/api/verifyCode", code, apiCookie)) {
            this.assertVerifyCode(response, 6);
        }
        JsonNode failure = this.post("/api/verifyCode", Map.of("text", ""), apiCookie);
        assertFalse(failure.path("success").asBoolean());
        assertTrue(failure.path("message").asText().startsWith("VerifyCode"));

        String adminCookie = this.login("admin");
        Map<String, Object> preview = this.draft("/verifyCode-preview", "return ${text};");
        preview.put("requestBody", Map.of("text", "B8L2"));
        preview.put("optionInfo", Map.of("resultHandler", "verifyCode"));
        RequestBody perform = RequestBody.create(JsonUtils.writeValueAsString(preview), MediaType.get("application/json"));
        try (Response response = this.request("POST", "/admin/api/perform", perform, adminCookie)) {
            this.assertVerifyCode(response, 4);
        }
        Map<String, Object> command = Map.of("id", "example-verifyCode", "version", 2, "requestBody", Map.of("text", "X9Y"));
        RequestBody smoke = RequestBody.create(JsonUtils.writeValueAsString(command), MediaType.get("application/json"));
        try (Response response = this.request("POST", "/admin/api/smoke", smoke, adminCookie)) {
            this.assertVerifyCode(response, 3);
        }
        JsonNode document = this.get("/docs/openapi.json", apiCookie);
        JsonNode responseContent = document.path("paths").path("/verifyCode").path("post").path("responses").path("200").path("content");
        assertTrue(responseContent.has("image/png"));
        String reference = responseContent.path("image/png").path("schema").path("$ref").asText();
        assertTrue(reference.startsWith("#/components/schemas/"));
        assertEquals("binary", document.at(reference.substring(1)).path("format").asText());
    }

    private void assertVerifyCode(Response response, int characters) throws Exception {
        assertEquals(200, response.code());
        assertEquals("image/png", response.header("Content-Type").split(";")[0]);
        assertEquals("no-store", response.header("Cache-Control"));
        var image = ImageIO.read(new ByteArrayInputStream(response.body().bytes()));
        assertNotNull(image);
        assertEquals(characters * 32 + 32, image.getWidth());
        assertEquals(64, image.getHeight());
    }

    private String login(String username) throws Exception {
        FormBody body = new FormBody.Builder().add("username", username).add("password", "example-password").build();
        try (Response response = this.request("POST", "/session/login", body, null)) {
            assertEquals(username, this.result(response).path("identity").asText());
            String cookie = response.header("Set-Cookie");
            assertNotNull(cookie);
            assertTrue(cookie.contains("HttpOnly"));
            return cookie.split(";", 2)[0];
        }
    }

    private JsonNode post(String path, Map<String, ?> body, String cookie) throws Exception {
        RequestBody request = RequestBody.create(JsonUtils.writeValueAsString(body), MediaType.get("application/json"));
        try (Response response = this.request("POST", path, request, cookie)) {
            return this.result(response);
        }
    }

    private JsonNode get(String path, String cookie) throws Exception {
        try (Response response = this.request("GET", path, null, cookie)) {
            return this.result(response);
        }
    }

    private JsonNode result(Response response) throws Exception {
        String body = response.body().string();
        assertEquals(200, response.code(), response.request().url() + "\n" + body);
        return JsonUtils.readTree(body);
    }

    private Response request(String method, String path, RequestBody body, String cookie) throws Exception {
        Request.Builder request = new Request.Builder().url(this.baseUrl + path).method(method, body);
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return this.http.newCall(request.build()).execute();
    }
}

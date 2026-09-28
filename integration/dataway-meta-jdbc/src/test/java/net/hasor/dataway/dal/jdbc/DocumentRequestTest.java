/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.TestDatabase;
import net.hasor.dataway.TestWebRequest;
import net.hasor.dataway.TestWebResponse;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.DatawayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

class DocumentRequestTest {
    @ParameterizedTest
    @ValueSource(strings = { "/swagger2.json", "/openapi.json" })
    void exportsActivePublicationsAndMergesMethodsWithoutExecutingScripts(String path) throws Exception {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).documentTitle("Orders API").documentVersion("2026.1").documentServer("https://public.example/gateway/api").apiInterceptor((context, chain) -> {
            throw new AssertionError("Documentation must not execute scripts");
        }).createDataway();
        var admin = dataway.getAdminService();
        ApiDefinition get = this.definition("get", "GET", "/orders");
        get.setSchema("""
                {"requestBody":{"type":"object","properties":{"name":{"type":"string"},"ids":{"type":"array","items":{"type":"integer"}}},"required":["name"]},
                 "requestHeader":{"type":"object","properties":{"X-Tenant":{"type":"string"}},"required":["X-Tenant"]},
                 "responseBody":{"type":"object","properties":{"ok":{"type":"boolean"}}},
                 "responseHeader":{"type":"object","properties":{"X-Count":{"type":"integer"}}}}
                """);
        get.setSample("""
                {"requestBody":{"name":"Ada","ids":[1,2]},"requestHeader":[{"checked":true,"name":"x-tenant","value":"demo"},{"checked":false,"name":"X-Disabled","value":"no"}],"responseBody":{"ok":true}}
                """);
        this.publish(dataway, get);
        get.setDescription("Unpublished changes");
        get.setSchema("{}");
        admin.save(get, 2);

        ApiDefinition post = this.definition("post", "POST", "/orders");
        post.setSchema("{\"requestBody\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}},\"responseBody\":{\"type\":\"string\"}}");
        post.setSample("{\"requestBody\":{\"name\":\"Ada\"},\"responseBody\":\"created\"}");
        this.publish(dataway, post);
        this.publish(dataway, this.definition("disabled", "GET", "/disabled"));
        admin.disableApi("disabled", 2);
        admin.save(this.definition("draft", "GET", "/draft"), 0);
        this.publish(dataway, this.definition("deleted", "GET", "/deleted"));
        admin.deleteApi("deleted", 2);

        JsonNode document = this.read(dataway, path);
        assertEquals("Orders API", document.path("info").path("title").asText());
        assertEquals("2026.1", document.path("info").path("version").asText());
        assertFalse(document.has("success"));
        assertEquals(1, document.path("paths").size());
        JsonNode operations = document.path("paths").path("/orders");
        assertEquals(2, operations.size());
        assertEquals("Published get", operations.path("get").path("summary").asText());
        assertEquals(3, operations.path("get").path("parameters").size());
        assertEquals("X-Tenant", operations.path("get").path("parameters").get(0).path("name").asText());
        assertTrue(operations.path("get").path("parameters").get(0).path("required").asBoolean());
        assertEquals("api_get_get", operations.path("get").path("operationId").asText());
        if (path.contains("swagger")) {
            assertEquals("2.0", document.path("swagger").asText());
            assertEquals("public.example", document.path("host").asText());
            assertEquals("https", document.path("schemes").get(0).asText());
            assertEquals("/gateway/api", document.path("basePath").asText());
            assertEquals("body", operations.path("post").path("parameters").get(0).path("in").asText());
            assertEquals("Ada", operations.path("post").path("parameters").get(0).path("schema").path("example").path("name").asText());
            assertEquals("integer", operations.path("get").path("responses").path("200").path("headers").path("X-Count").path("type").asText());
        } else {
            assertEquals("3.2.1", document.path("openapi").asText());
            assertEquals("https://public.example/gateway/api", document.path("servers").get(0).path("url").asText());
            assertEquals("Ada", operations.path("post").path("requestBody").path("content").path("application/json").path("example").path("name").asText());
        }
        String json = JsonUtils.writeValueAsString(document);
        assertFalse(json.contains("DO_NOT_EXPOSE_OR_EXECUTE"));
        Path output = Path.of("build/document-fixtures" + path);
        Files.createDirectories(output.getParent());
        Files.writeString(output, json);

        admin.publish("get", 3);
        assertEquals("Unpublished changes", this.read(dataway, path).path("paths").path("/orders").path("get").path("summary").asText());
    }

    private ApiDefinition definition(String id, String method, String path) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(id);
        definition.setMethod(method);
        definition.setPath(path);
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript("DO_NOT_EXPOSE_OR_EXECUTE invalid script");
        definition.setDescription("Published " + id);
        return definition;
    }

    private void publish(Dataway dataway, ApiDefinition definition) {
        dataway.getAdminService().save(definition, 0);
        dataway.getAdminService().publish(definition.getId(), 1);
    }

    private JsonNode read(Dataway dataway, String path) throws Exception {
        TestWebResponse response = new TestWebResponse();
        dataway.getDocumentHandler().handle(new TestWebRequest("GET", path, Map.of()), response);
        return JsonUtils.convertValue(response.getResult(), JsonNode.class);
    }

    @Test
    void identityAndDocumentAuthorizationAreIndependentOfManagementAndInvocation() throws Exception {
        AtomicInteger checks = new AtomicInteger();
        UserIdentity user = UserIdentity.authenticated("reader");
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> user).authorizationCheck((identity, operation) -> {
            assertSame(user, identity);
            assertEquals(Operation.DOCUMENT, operation);
            return checks.incrementAndGet() == 1;
        }).adminInterceptor((context, chain) -> {
            throw new AssertionError("Not an admin request");
        }).createDataway();
        assertTrue(this.read(dataway, "/openapi.json").path("paths").isEmpty());
        DatawayException denied = assertThrows(DatawayException.class, () -> this.read(dataway, "/swagger2.json"));
        assertEquals(401, denied.status());
        assertEquals(2, checks.get());
    }

    @Test
    void relativeServerIgnoresHostHeadersAndUnknownRoutesAndMethodsAreRejected() throws Exception {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).documentServer("/external/api").createDataway();
        TestWebRequest request = new TestWebRequest("get", "/swagger2.json", Map.of());
        request.setHeaders(Map.of("Host", "internal.invalid", "X-Forwarded-Host", "untrusted.invalid"));
        TestWebResponse response = new TestWebResponse();
        dataway.getDocumentHandler().handle(request, response);
        JsonNode document = JsonUtils.convertValue(response.getResult(), JsonNode.class);
        assertFalse(document.has("host"));
        assertEquals("/external/api", document.path("basePath").asText());
        assertEquals(404, assertThrows(DatawayException.class, () -> this.read(dataway, "/missing.json")).status());
        assertEquals(405, assertThrows(DatawayException.class, () -> dataway.getDocumentHandler().handle(new TestWebRequest("POST", "/openapi.json", Map.of()), new TestWebResponse())).status());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "relative/api", "//example.com/api", "ftp://example.com/api", "https://user:password@example.com/api", "/api?secret=1", "/api#fragment" })
    void invalidPublicServerConfigurationFailsDuringInitialization(String server) {
        assertThrows(IllegalArgumentException.class, () -> new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).documentServer(server).createDataway());
    }

    @Test
    void openApiKeepsModernSchemaAndSwaggerReportsUnsupportedKeywords() throws Exception {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).createDataway();
        ApiDefinition definition = this.definition("modern", "POST", "/modern");
        definition.setSchema("{\"responseBody\":{\"oneOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}}");
        this.publish(dataway, definition);
        JsonNode document = this.read(dataway, "/openapi.json");
        String ref = document.path("paths").path("/modern").path("post").path("responses").path("200").path("content").path("application/json").path("schema").path("$ref").asText();
        assertEquals(2, document.at(ref.substring(1)).path("oneOf").size());
        assertEquals(422, assertThrows(DatawayException.class, () -> this.read(dataway, "/swagger2.json")).status());
    }

    @Test
    void schemaReferencesRemainResolvableAfterEmbeddingAndDoNotRewriteExamples() throws Exception {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).createDataway();
        ApiDefinition definition = this.definition("references", "POST", "/references");
        definition.setSchema("""
                {"responseBody":{"type":"object","properties":{"name":{"type":"string"},"alias":{"$ref":"#/properties/name"}},
                  "example":{"$ref":"this is ordinary example data"}}}
                """);
        this.publish(dataway, definition);
        for (String path : new String[] { "/swagger2.json", "/openapi.json" }) {
            JsonNode document = this.read(dataway, path);
            JsonNode schemas = path.contains("swagger") ? document.path("definitions") : document.path("components").path("schemas");
            JsonNode schema = schemas.properties().iterator().next().getValue();
            String ref = schema.path("properties").path("alias").path("$ref").asText();
            assertEquals("string", document.at(ref.substring(1)).path("type").asText());
            assertEquals("this is ordinary example data", schema.path("example").path("$ref").asText());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "/swagger2.json", "/openapi.json" })
    void multipartSchemaAndLegacyJsonTextExamplesAreExported(String path) throws Exception {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).createDataway();
        ApiDefinition definition = this.definition("upload", "POST", "/upload");
        definition.setSchema(JsonUtils.writeValueAsString(Map.of("requestBody", "{\"type\":\"object\",\"properties\":{\"file\":{\"type\":\"string\",\"format\":\"binary\"}},\"required\":[\"file\"]}")));
        definition.setSample(JsonUtils.writeValueAsString(Map.of("requestHeader", "[{\"name\":\"content-TYPE\",\"value\":\"multipart/form-data; boundary=debug\",\"checked\":true}]")));
        this.publish(dataway, definition);
        JsonNode operation = this.read(dataway, path).path("paths").path("/upload").path("post");
        if (path.contains("swagger")) {
            assertEquals("multipart/form-data", operation.path("consumes").get(0).asText());
            JsonNode parameter = operation.path("parameters").get(0);
            assertEquals("formData", parameter.path("in").asText());
            assertEquals("file", parameter.path("type").asText());
            assertTrue(parameter.path("required").asBoolean());
        } else {
            assertTrue(operation.path("requestBody").path("content").has("multipart/form-data"));
        }
        assertEquals(1, operation.path("responses").path("200").size());
    }
}

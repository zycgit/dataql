/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.document;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest extends ServiceTestSupport {
    private DocumentService service(String server) {
        BeanContainer beans = new BeanContainer();
        beans.setBean(ApiDataAccessLayer.class, this.access);
        return new DocumentService(beans, "Orders", "v2", server);
    }

    @Test
    void emptyExportsHaveTheSelectedVersionAndExplicitServer() {
        Map<String, Object> swagger = this.service("HTTPS://api.example.test:8443").swagger2();
        assertEquals("2.0", swagger.get("swagger"));
        assertEquals("api.example.test:8443", swagger.get("host"));
        assertEquals(List.of("https"), swagger.get("schemes"));
        assertEquals("/", swagger.get("basePath"));
        assertEquals(Map.of(), swagger.get("paths"));
        assertFalse(swagger.containsKey("definitions"));
        assertEquals(Map.of("title", "Orders", "version", "v2"), swagger.get("info"));
        Map<String, Object> relative = this.service("/proxy/api").swagger2();
        assertEquals("/proxy/api", relative.get("basePath"));
        assertFalse(relative.containsKey("host"));
        Map<String, Object> openapi = this.service("/proxy/api").openapi();
        assertEquals("3.2.1", openapi.get("openapi"));
        assertEquals(List.of(Map.of("url", "/proxy/api")), openapi.get("servers"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "relative", "//example.test/api", "ftp://example.test", "https://example.test?a=1", "/api#fragment", "https://user:password@example.test", "http:/missing-host" })
    void invalidServerLocationsFailAtInitialization(String server) {
        assertThrows(IllegalArgumentException.class, () -> this.service(server));
    }

    @Test
    void titleAndVersionMustBePresent() {
        BeanContainer beans = new BeanContainer();
        beans.setBean(ApiDataAccessLayer.class, this.access);
        assertThrows(IllegalArgumentException.class, () -> new DocumentService(beans, null, "v1", "/api"));
        assertThrows(IllegalArgumentException.class, () -> new DocumentService(beans, "Title", " ", "/api"));
    }

    @Test
    void exportSelectsTheNewestReleasePerMethodAndPathWithoutLeakingScripts() {
        Map<FieldDef, String> info = this.info("api", "1", 1);
        Map<FieldDef, String> old = this.release(info, "old", "1", 1);
        Map<FieldDef, String> current = this.release(info, "current", "1", 2);
        current.put(COMMENT, "Latest description");
        Map<FieldDef, String> post = this.release(info, "post", "1", 2);
        post.put(METHOD, "POST");
        when(this.access.listObjects(EntityType.RELEASE, Map.of(STATUS, "1"))).thenReturn(List.of(old, post, current));
        Map<?, ?> paths = (Map<?, ?>) this.service("/api").openapi().get("paths");
        Map<?, ?> operations = (Map<?, ?>) paths.get("/api");
        assertEquals(2, operations.size());
        assertEquals("Latest description", ((Map<?, ?>) operations.get("get")).get("summary"));
        assertEquals("api_api_get", ((Map<?, ?>) operations.get("get")).get("operationId"));
        assertFalse(operations.toString().contains("return 'value'"));
        verify(this.access).listObjects(EntityType.RELEASE, Map.of(STATUS, "1"));
        verify(this.access, never()).write(anyList());
    }

    @Test
    void postMetadataProducesReusableSchemasExamplesAndResponseHeadersInBothFormats() {
        Map<FieldDef, String> row = this.release(this.info("orders", "1", 1), "release", "1", 1);
        row.put(METHOD, "POST");
        row.put(SCHEMA, """
                {"requestBody":{"type":"object","properties":{"name":{"type":"string"}}},
                 "responseBody":{"type":"array","items":{"type":"string"}},
                 "responseHeader":{"type":"object","properties":{"X-Count":{"type":"integer"},"Content-Type":{"type":"string"}}}}
                """);
        row.put(SAMPLE, """
                {"requestBody":"{\\"name\\":\\"order\\"}","responseBody":"[\\"ok\\"]",
                 "responseHeader":{"Content-Type":"application/json; charset=utf-8"}}
                """);
        when(this.access.listObjects(EntityType.RELEASE, Map.of(STATUS, "1"))).thenReturn(List.of(row));
        Map<String, Object> openapi = this.service("/api").openapi();
        Map<?, ?> operation = this.operation(openapi, "/orders", "post");
        Map<?, ?> request = (Map<?, ?>) operation.get("requestBody");
        Map<?, ?> media = (Map<?, ?>) ((Map<?, ?>) request.get("content")).get("application/json");
        assertEquals(Map.of("name", "order"), media.get("example"));
        assertTrue(((Map<?, ?>) media.get("schema")).get("$ref").toString().startsWith("#/components/schemas/"));
        assertTrue(openapi.containsKey("components"));
        Map<?, ?> response = (Map<?, ?>) ((Map<?, ?>) operation.get("responses")).get("200");
        assertEquals(Map.of("X-Count", Map.of("schema", Map.of("type", "integer"))), response.get("headers"));

        Map<String, Object> swagger = this.service("/api").swagger2();
        Map<?, ?> swaggerOperation = this.operation(swagger, "/orders", "post");
        Map<?, ?> body = (Map<?, ?>) ((List<?>) swaggerOperation.get("parameters")).get(0);
        assertEquals("body", body.get("in"));
        assertTrue(((Map<?, ?>) body.get("schema")).containsKey("allOf"));
        assertEquals(List.of("application/json"), swaggerOperation.get("consumes"));
        assertTrue(swagger.containsKey("definitions"));
        assertEquals(row.get(SCHEMA), this.access.listObjects(EntityType.RELEASE, Map.of(STATUS, "1")).get(0).get(SCHEMA));
    }

    private Map<?, ?> operation(Map<String, Object> document, String path, String method) {
        return (Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) document.get("paths")).get(path)).get(method);
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "HEAD" })
    void readMethodsDescribeBodyPropertiesAsQueryParameters(String method) {
        Map<FieldDef, String> row = this.release(this.info("api", "1", 1), "release", "1", 1);
        row.put(METHOD, method);
        row.put(SAMPLE, "{\"requestBody\":{\"page\":1}}");
        when(this.access.listObjects(EntityType.RELEASE, Map.of(STATUS, "1"))).thenReturn(List.of(row));
        Map<?, ?> operation = this.operation(this.service("/api").openapi(), "/api", method.toLowerCase());
        assertFalse(operation.containsKey("requestBody"));
        Map<?, ?> parameter = (Map<?, ?>) ((List<?>) operation.get("parameters")).get(0);
        assertEquals("query", parameter.get("in"));
        assertEquals("page", parameter.get("name"));
        assertEquals(1, parameter.get("example"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "multipart/form-data", "application/x-www-form-urlencoded" })
    void swaggerFormBodiesUseFormParameters(String type) {
        Map<FieldDef, String> row = this.release(this.info("api", "1", 1), "release", "1", 1);
        row.put(METHOD, "POST");
        row.put(SAMPLE, "{\"requestHeader\":{\"Content-Type\":\"" + type + "\"},\"requestBody\":{\"field\":\"value\"}}");
        when(this.access.listObjects(EntityType.RELEASE, Map.of(STATUS, "1"))).thenReturn(List.of(row));
        Map<?, ?> operation = this.operation(this.service("/api").swagger2(), "/api", "post");
        assertEquals(List.of(type), operation.get("consumes"));
        assertEquals("formData", ((Map<?, ?>) ((List<?>) operation.get("parameters")).get(0)).get("in"));
    }

    @Test
    void traceIsSupportedOnlyByOpenapiAndUnknownMethodsFail() {
        Map<FieldDef, String> row = this.release(this.info("api", "1", 1), "release", "1", 1);
        row.put(METHOD, "TRACE");
        when(this.access.listObjects(EntityType.RELEASE, Map.of(STATUS, "1"))).thenReturn(List.of(row));
        assertNotNull(this.operation(this.service("/api").openapi(), "/api", "trace"));
        assertEquals(422, assertThrows(DatawayException.class, () -> this.service("/api").swagger2()).status());
        row.put(METHOD, "CUSTOM");
        assertEquals(422, assertThrows(DatawayException.class, () -> this.service("/api").openapi()).status());
    }
}

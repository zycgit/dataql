/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.document;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.service.DatawayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DocumentSchemaTest {
    private DocumentSchema metadata(Map<String, Object> schema, Map<String, Object> sample) {
        return new DocumentSchema(JsonUtils.writeValueAsString(schema), JsonUtils.writeValueAsString(sample));
    }

    @Test
    void absentMetadataAndSamplesKeepTheirDocumentMeaning() {
        DocumentSchema empty = new DocumentSchema(" ", null);
        assertFalse(empty.has("requestBody"));
        assertFalse(empty.hasSample("requestBody"));
        assertEquals(Map.of(), empty.schema("requestBody", false));
        assertEquals(Map.of("schema", Map.of()), empty.content("requestBody"));
        DocumentSchema values = new DocumentSchema("{}", """
                {"requestBody":"{\\"id\\":1}","responseBody":"plain text","responseHeader":"","requestHeader":null}
                """);
        assertTrue(values.has("requestBody"));
        assertEquals(Map.of("id", 1), values.sample("requestBody"));
        assertEquals("plain text", values.sample("responseBody"));
        assertEquals("", values.sample("responseHeader"));
        assertNull(values.sample("requestHeader"));
        assertEquals("application/json", values.contentType("responseHeader"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "{invalid", "[]", "1", "true" })
    void invalidTopLevelMetadataFailsWithAnUnprocessableDocument(String value) {
        assertEquals(422, assertThrows(DatawayException.class, () -> new DocumentSchema(value, "{}")).status());
        assertEquals(422, assertThrows(DatawayException.class, () -> new DocumentSchema("{}", value)).status());
    }

    @Test
    void referencesAreRebasedRecursivelyButExampleDataAndExternalSchemaIdsArePreserved() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$ref", "#");
        schema.put("properties", Map.of("child", Map.of("$ref", "#/$defs/item"), "flag", true));
        schema.put("$defs", Map.of("item", Map.of("type", "string")));
        schema.put("items", Map.of("$dynamicRef", "#/properties/child"));
        schema.put("additionalProperties", false);
        schema.put("allOf", List.of(Map.of("$ref", "#"), Map.of("$id", "https://example.test/nested", "$ref", "#local")));
        schema.put("example", Map.of("$ref", "#untouched"));
        schema.put("oneOf", List.of(Map.of("$ref", "https://example.test/schema")));
        DocumentSchema metadata = this.metadata(Map.of("responseBody", schema), Map.of());
        Map<String, Object> target = new LinkedHashMap<>();
        metadata.registerSchemas("api/id", target, false);
        String ref = metadata.schema("responseBody", false).get("$ref").toString();
        assertTrue(ref.startsWith("#/components/schemas/api_"));
        Map<?, ?> stored = (Map<?, ?>) target.values().iterator().next();
        assertEquals(ref, stored.get("$ref"));
        assertEquals(ref + "/$defs/item", ((Map<?, ?>) ((Map<?, ?>) stored.get("properties")).get("child")).get("$ref"));
        assertEquals(Map.of("$ref", "#untouched"), stored.get("example"));
        assertEquals("#local", ((Map<?, ?>) ((List<?>) stored.get("allOf")).get(1)).get("$ref"));
        assertEquals("#", schema.get("$ref"));
    }

    @Test
    void namedAnchorsWithoutTheirOwnSchemaIdAreRejectedInsteadOfProducingBrokenReferences() {
        DocumentSchema metadata = this.metadata(Map.of("requestBody", Map.of("$ref", "#named")), Map.of());
        assertEquals(422, assertThrows(DatawayException.class, () -> metadata.registerSchemas("api", new LinkedHashMap<>(), false)).status());
        DocumentSchema selfContained = this.metadata(Map.of("requestBody", Map.of("$id", "https://example.test/schema", "$ref", "#named")), Map.of());
        Map<String, Object> target = new LinkedHashMap<>();
        selfContained.registerSchemas("api", target, false);
        assertEquals("#named", ((Map<?, ?>) target.values().iterator().next()).get("$ref"));
    }

    @Test
    void swaggerSchemasRemoveTheDialectAndRetainSupportedNestedStructures() {
        Map<String, Object> schema = Map.of("$schema", "https://json-schema.org/draft-04/schema", "type", "object", "properties", Map.of("list", Map.of("type", "array", "items", Map.of("type", "integer"))), "additionalProperties", Map.of("type", "boolean"), "allOf", List.of(Map.of("type", "object")), "x-custom", true);
        DocumentSchema metadata = this.metadata(Map.of("responseBody", schema), Map.of());
        Map<String, Object> converted = metadata.schema("responseBody", true);
        assertFalse(converted.containsKey("$schema"));
        assertEquals(schema.get("properties"), converted.get("properties"));
        assertEquals(true, converted.get("x-custom"));
        Map<String, Object> target = new LinkedHashMap<>();
        metadata.registerSchemas("api", target, true);
        assertTrue(metadata.schema("responseBody", true).get("$ref").toString().startsWith("#/definitions/"));
    }

    @ParameterizedTest
    @MethodSource("unsupportedSwaggerSchemas")
    void swaggerRejectsSchemaFeaturesItCannotRepresent(Map<String, Object> schema) {
        DocumentSchema metadata = this.metadata(Map.of("responseBody", schema), Map.of());
        assertEquals(422, assertThrows(DatawayException.class, () -> metadata.schema("responseBody", true)).status());
        assertEquals(schema, metadata.schema("responseBody", false));
    }

    private static Stream<Map<String, Object>> unsupportedSwaggerSchemas() {
        return Stream.of(Map.of("oneOf", List.of(Map.of("type", "string"))), Map.of("type", List.of("string", "null")), Map.of("type", "null"), Map.of("exclusiveMinimum", 2), Map.of("exclusiveMaximum", 3), Map.of("const", 1), Map.of("items", List.of(Map.of("type", "string"))));
    }

    @Test
    void headersMergeCaseInsensitivelyAndOmitReservedAndUncheckedEntries() {
        Map<String, Object> headerSchema = Map.of("properties", Map.of("X-Trace", Map.of("type", "string")), "required", List.of("X-Trace"));
        List<Map<String, Object>> headers = List.of(Map.of("name", "x-trace", "value", "example"), Map.of("name", "Disabled", "value", "hidden", "checked", false), Map.of("name", "Content-Type", "value", " Text/Plain; charset=utf-8 "), Map.of("name", "Authorization", "value", "secret"), Map.of("name", "Accept", "value", "*/*"), Map.of("value", "no-name"));
        DocumentSchema metadata = this.metadata(Map.of("requestHeader", headerSchema), Map.of("requestHeader", headers));
        List<Map<String, Object>> parameters = metadata.parameters("requestHeader", "header", false);
        assertEquals(1, parameters.size());
        assertEquals("X-Trace", parameters.get(0).get("name"));
        assertEquals(true, parameters.get(0).get("required"));
        assertEquals("example", parameters.get(0).get("example"));
        assertEquals("text/plain", metadata.contentType("requestHeader"));
    }

    @Test
    void arraysUseMultiValueQueryParametersButNotMultiValueHeaders() {
        Map<String, Object> schema = Map.of("properties", Map.of("tag", Map.of("type", "array", "items", Map.of("type", "string"), "title", "ignored")));
        DocumentSchema metadata = this.metadata(Map.of("requestBody", schema), Map.of("requestBody", Map.of("tag", List.of("a", "b"))));
        Map<String, Object> query = metadata.parameters("requestBody", "query", true).get(0);
        assertEquals("multi", query.get("collectionFormat"));
        assertEquals(List.of("a", "b"), query.get("x-example"));
        assertFalse(query.containsKey("title"));
        assertFalse(metadata.parameters("requestBody", "header", true).get(0).containsKey("collectionFormat"));
    }

    @Test
    void swaggerConvertsBinaryFormFieldsToFiles() {
        DocumentSchema metadata = this.metadata(Map.of("requestBody", Map.of("properties", Map.of("file", Map.of("type", "string", "format", "binary")))), Map.of());
        Map<String, Object> field = metadata.parameters("requestBody", "formData", true).get(0);
        assertEquals("file", field.get("type"));
        assertFalse(field.containsKey("format"));
    }

    @ParameterizedTest
    @MethodSource("invalidParameterSchemas")
    void swaggerParametersRequireARepresentablePrimitiveType(Map<String, Object> property) {
        DocumentSchema metadata = this.metadata(Map.of("requestBody", Map.of("properties", Map.of("value", property))), Map.of());
        assertEquals(422, assertThrows(DatawayException.class, () -> metadata.parameters("requestBody", "query", true)).status());
    }

    private static Stream<Map<String, Object>> invalidParameterSchemas() {
        return Stream.of(Map.of(), Map.of("type", "object"), Map.of("$ref", "#/type"), Map.of("type", "file"), Map.of("type", "array", "items", Map.of("type", "object")));
    }

    @Test
    void responseHeadersExcludeContentTypeAndUseTheRequestedSchemaFormat() {
        Map<String, Object> properties = Map.of("Content-Type", Map.of("type", "string"), "X-Count", Map.of("type", "integer"));
        DocumentSchema metadata = this.metadata(Map.of("responseHeader", Map.of("properties", properties)), Map.of());
        assertEquals(Map.of("X-Count", Map.of("type", "integer")), metadata.responseHeaders(true));
        assertEquals(Map.of("X-Count", Map.of("schema", Map.of("type", "integer"))), metadata.responseHeaders(false));
    }

    @Test
    void legacyJsonStringsInSchemaSectionsAreReadAndEmptyContentTypesUseTheDefault() {
        DocumentSchema metadata = this.metadata(Map.of("requestBody", "{\"type\":\"string\"}"), Map.of("requestHeader", Map.of("Content-Type", " "), "responseHeader", Map.of("Content-Type", 2)));
        assertEquals(Map.of("type", "string"), metadata.schema("requestBody", false));
        assertEquals("application/json", metadata.contentType("requestHeader"));
        assertEquals("application/json", metadata.contentType("responseHeader"));
    }
}

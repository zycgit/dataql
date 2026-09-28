/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiRelease;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.model.vo.ApiDetailVO;
import net.hasor.dataway.service.ConvertUtils;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.config.MemoryRequest;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ConvertUtilsTest extends ServiceTestSupport {
    @Test
    void editorInputConvertsToOriginalScriptAndCompleteMetadataWithoutMutatingTheInput() {
        Map<String, Object> input = this.editor();
        Map<String, Object> originalSample = new LinkedHashMap<>(Map.of("extra", 1, "headerData", List.of()));
        input.put("sample", originalSample);
        input.put("requestBody", "{\"name\":\"value\"}");
        input.put("schema", Map.of("responseBody", Map.of("type", "string")));
        input.put("optionInfo", Map.of("resultStructure", false));
        input.put("headerData", List.of(Map.of("checked", true, "name", "X-Name", "value", "example")));
        ApiDefinition definition = ConvertUtils.convertToApiDefinition("api", input);
        assertEquals("api", definition.getId());
        assertEquals("return 'ok';", definition.getScript());
        assertEquals("", definition.getDescription());
        Map<?, ?> sample = JsonUtils.readValue(definition.getSample(), Map.class);
        assertEquals(Map.of("name", "value"), sample.get("requestBody"));
        assertEquals(1, sample.get("extra"));
        assertFalse(sample.containsKey("headerData"));
        assertTrue(originalSample.containsKey("headerData"));
        assertEquals(Map.of("resultStructure", false), JsonUtils.readValue(definition.getOptions(), Map.class));
    }

    private Map<String, Object> editor() {
        return new LinkedHashMap<>(Map.of("select", "POST", "apiPath", "/test", "codeType", "DataQL", "codeValue", "return 'ok';"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "responseFormat", "resultStructure", "wrapAllParameters", "wrapParameterName" })
    void editorOptionsPreserveExplicitNullForExecutionValidation(String option) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put(option, null);
        Map<String, Object> input = this.editor();
        input.put("optionInfo", options);
        ApiDefinition definition = ConvertUtils.convertToApiDefinition("api", input);
        assertEquals(options, JsonUtils.readValue(definition.getOptions(), Map.class));
        assertSame(options, input.get("optionInfo"));
        assertTrue(options.containsKey(option));
        assertNull(options.get(option));

        input.put("optionInfo", "{\"" + option + "\":null}");
        ApiDefinition fromText = ConvertUtils.convertToApiDefinition("api", input);
        assertEquals(options, JsonUtils.readValue(fromText.getOptions(), Map.class));
    }

    @Test
    void anAbsentOrEmptyOptionsDocumentKeepsTheEngineDefaults() {
        Map<String, Object> input = this.editor();
        assertEquals("{}", ConvertUtils.convertToApiDefinition("api", input).getOptions());
        input.put("optionInfo", Map.of());
        assertEquals("{}", ConvertUtils.convertToApiDefinition("api", input).getOptions());
        input.put("optionInfo", null);
        assertEquals("{}", ConvertUtils.convertToApiDefinition("api", input).getOptions());
    }

    @ParameterizedTest
    @ValueSource(strings = { "select", "apiPath", "codeType", "codeValue" })
    void requiredEditorValuesRejectAbsentBlankOrWrongTypes(String field) {
        Map<String, Object> input = this.editor();
        input.remove(field);
        assertEquals(400, assertThrows(DatawayException.class, () -> ConvertUtils.convertToApiDefinition("api", input)).status());
        input.put(field, " ");
        assertEquals(400, assertThrows(DatawayException.class, () -> ConvertUtils.convertToApiDefinition("api", input)).status());
        input.put(field, 5);
        assertEquals(400, assertThrows(DatawayException.class, () -> ConvertUtils.convertToApiDefinition("api", input)).status());
    }

    @ParameterizedTest
    @MethodSource("invalidHeaders")
    void invalidEditorHeaderRowsFailAsClientErrors(Object headers) {
        Map<String, Object> input = this.editor();
        input.put("headerData", headers);
        assertEquals(400, assertThrows(DatawayException.class, () -> ConvertUtils.convertToApiDefinition("api", input)).status());
    }

    private static Stream<Object> invalidHeaders() {
        return Stream.of("not an array", List.of("not an object"), List.of(Map.of("name", "x", "value", "v")), List.of(Map.of("checked", true, "name", 1, "value", "v")), List.of(Map.of("checked", true, "name", "x", "value", 2)));
    }

    @Test
    void parameterConversionAcceptsObjectsAndEmptyInputButRejectsNonObjectDocuments() {
        Map<String, Object> source = new LinkedHashMap<>(Map.of("id", 1));
        assertSame(source, ConvertUtils.convertToApiParameters(source));
        assertEquals(source, ConvertUtils.convertToApiParameters("{\"id\":1}"));
        for (Object empty : new Object[] { null, "", " ", "null" }) {
            Map<String, Object> result = ConvertUtils.convertToApiParameters(empty);
            assertTrue(result.isEmpty());
            result.put("mutable", true);
        }
        for (Object invalid : new Object[] { "[1]", "true", "{invalid", List.of(1), 1 }) {
            assertEquals(400, assertThrows(DatawayException.class, () -> ConvertUtils.convertToApiParameters(invalid)).status());
        }
    }

    @ParameterizedTest
    @CsvSource({ "false,false,true,0", "true,true,false,1", "true,true,true,2", "true,false,false,3" })
    void consoleViewsUseThePublicationFlagsAndKeepDetailDataSeparate(boolean published, boolean enabled, boolean draft, int status) {
        ApiDefinition definition = this.definition("api", "return 1;");
        definition.setSample("{\"requestBody\":\"{}\",\"requestHeader\":\"[]\"}");
        definition.setSchema("{}");
        definition.setOptions("{}");
        ApiState state = new ApiState();
        state.setRevision(4);
        state.setPublished(published);
        state.setEnabled(enabled);
        state.setHasDraft(draft);

        assertEquals(status, ConvertUtils.convertToApiSummaryVO(definition, state).getStatus());
        ApiDetailVO detail = ConvertUtils.convertToApiDetailVO(definition, state);
        assertEquals("api", detail.getId());
        assertEquals(4, detail.getVersion());
        assertEquals(status, detail.getStatus());
        assertEquals("return 1;", detail.getCodeInfo().getCodeValue());
        assertEquals("{}", detail.getRequestBody());
        assertEquals(List.of(), detail.getHeaderData());
        assertEquals(Map.of(), detail.getSchema());
    }

    @Test
    void codeAndHistoryViewsPreserveBodyFormsAndUseUtcTime() {
        ApiDefinition definition = this.definition("api", "return 1;");
        var code = ConvertUtils.convertToApiCodeVO(definition, Map.of("requestBody", Map.of("id", 1), "headerData", List.of("new")));
        assertEquals("{\"id\":1}", code.getRequestBody());
        assertEquals(List.of("new"), code.getHeaderData());
        ApiRelease release = new ApiRelease();
        release.setId("history");
        release.setPublishedAt(Instant.EPOCH);
        var history = ConvertUtils.convertToApiHistoryVO(release, 3);
        assertEquals("history", history.getHistoryId());
        assertEquals("1970-01-01 00:00:00", history.getTime());
        assertEquals(3, history.getStatus());
    }

    @Test
    void webContextHidesCredentialHeadersButRetainsParsedCookiesAndBusinessBody() {
        MemoryRequest request = this.request("POST", "/test");
        request.setHeaders(Map.of("Authorization", "secret", "Cookie", "session=token", "X-Trace", "trace"));
        Map<String, Object> body = Map.of("body", "value");
        Map<String, Object> parameters = Map.of("query", "value");
        Map<String, ?> context = ConvertUtils.convertToWebContext(request, parameters, body);
        assertEquals(Map.of("x-trace", "trace"), context.get("headers"));
        assertEquals(Map.of("x-trace", List.of("trace")), context.get("headerValues"));
        assertEquals(Map.of("session", List.of("token")), context.get("cookies"));
        assertSame(body, context.get("body"));
        assertSame(parameters, context.get("parameters"));
        assertEquals("secret", request.getHeaders().get("authorization"));
    }
}

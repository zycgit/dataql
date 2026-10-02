/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.config.MemoryRequest;
import net.hasor.dataway.service.config.MemoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminWriteHttpTest extends AdminHttpSupport {
    @Test
    void creatingAnApiGeneratesAnIdAndStoresTheOriginalScriptAndEditorData() throws Exception {
        Map<String, Object> body = this.editor("-1", 0);
        body.put("codeType", "SQL");
        body.put("codeValue", "select :name");
        when(this.service.save(any(ApiDefinition.class), eq(0L))).thenReturn(this.state(1));
        Map<?, ?> result = this.success(this.post("/save-api", body));
        String id = assertInstanceOf(String.class, result.get("result"));
        assertEquals(id, UUID.fromString(id).toString());
        assertEquals(1, result.get("version"));
        ArgumentCaptor<ApiDefinition> saved = ArgumentCaptor.forClass(ApiDefinition.class);
        verify(this.service).save(saved.capture(), eq(0L));
        ApiDefinition definition = saved.getValue();
        assertEquals(id, definition.getId());
        assertEquals(ApiScriptType.SQL, definition.getType());
        assertEquals("select :name", definition.getScript());
        assertEquals("POST", definition.getMethod());
        assertEquals("/saved", definition.getPath());
        assertEquals("description", definition.getDescription());
        assertEquals(Map.of("resultHandler", "raw"), JsonUtils.readValue(definition.getOptions(), Map.class));
        assertEquals(Map.of("name", "demo"), JsonUtils.readValue(definition.getSample(), Map.class).get("requestBody"));
    }

    private Map<String, Object> editor(String id, Object version) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("version", version);
        body.put("select", "POST");
        body.put("apiPath", "/saved");
        body.put("codeType", "DataQL");
        body.put("codeValue", "return ${name};");
        body.put("comment", "description");
        body.put("requestBody", Map.of("name", "demo"));
        body.put("optionInfo", Map.of("resultHandler", "raw"));
        return body;
    }

    @Test
    void savingAnExistingDraftUsesItsIdAndOptimisticVersion() throws Exception {
        when(this.service.save(any(ApiDefinition.class), eq(4L))).thenReturn(this.state(5));
        assertEquals(Map.of("success", true, "code", 200, "message", "OK", "result", "api", "version", 5), this.success(this.post("/save-api?id=api", this.editor("api", 4))));
        ArgumentCaptor<ApiDefinition> saved = ArgumentCaptor.forClass(ApiDefinition.class);
        verify(this.service).save(saved.capture(), eq(4L));
        assertEquals("api", saved.getValue().getId());
        assertEquals("return ${name};", saved.getValue().getScript());
    }

    @Test
    void savingPreservesApplicationHandlerOptions() throws Exception {
        when(this.service.save(any(ApiDefinition.class), eq(4L))).thenReturn(this.state(5));
        Map<String, Object> body = this.editor("api", 4);
        Map<String, Object> options = Map.of("resultHandler", "created", "status", 202, "labels", List.of("api"));
        body.put("optionInfo", options);
        this.success(this.post("/save-api", body));
        ArgumentCaptor<ApiDefinition> saved = ArgumentCaptor.forClass(ApiDefinition.class);
        verify(this.service).save(saved.capture(), eq(4L));
        assertEquals(options, JsonUtils.readValue(saved.getValue().getOptions(), Map.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "responseFormat", "resultHandler", "wrapAllParameters", "wrapParameterName" })
    void savingADraftRejectsExplicitNullOptionsBeforeCallingTheService(String option) throws Exception {
        String body = """
                {"id":"api","version":4,"select":"POST","apiPath":"/saved","codeType":"DataQL",
                 "codeValue":"return 1;","optionInfo":{"%s":null}}
                """.formatted(option);
        HttpResponse<String> response = this.postJson("/save-api", body);
        assertEquals(400, response.statusCode(), response.body());
        assertTrue(response.body().contains(option + " must not be null"), response.body());
        verifyNoInteractions(this.service);
    }

    @Test
    void publishingAndDisablingReturnTheNewVersionAndDeletingReturnsSuccess() throws Exception {
        when(this.service.publish("api", 4)).thenReturn(this.state(5));
        when(this.service.disableApi("api", 5)).thenReturn(this.state(6));
        assertEquals(5, this.success(this.post("/publish", Map.of("id", "api", "version", 4))).get("version"));
        assertEquals(6, this.success(this.post("/disable?id=api", Map.of("version", 5))).get("version"));
        assertEquals(true, this.success(this.post("/delete", Map.of("id", "api", "version", 6))).get("result"));
        verify(this.service).publish("api", 4);
        verify(this.service).disableApi("api", 5);
        verify(this.service).deleteApi("api", 6);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "\"4\"", "-1", "1.5", "true", "{}", "[]" })
    void writeRequestsRequireANonNegativeNumericIntegerVersion(String json) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", "api");
        if (json != null) {
            body.put("version", JsonUtils.readValue(json, Object.class));
        }
        HttpResponse<String> response = this.post("/publish", body);
        assertEquals(400, response.statusCode(), response.body());
        verifyNoInteractions(this.service);
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "4", "\"other\"", "\"\"", "\" \"" })
    void aBodyIdCannotConflictWithTheQueryId(String json) throws Exception {
        String body = "{\"id\":" + json + ",\"version\":4}";
        assertEquals(400, this.postJson("/delete?id=api", body).statusCode());
        verifyNoInteractions(this.service);
    }

    @Test
    void aNumericIdIsRejectedRatherThanSilentlyConvertedToText() throws Exception {
        assertEquals(400, this.post("/delete", Map.of("id", 4, "version", 4)).statusCode());
        verifyNoInteractions(this.service);
    }

    @ParameterizedTest
    @ValueSource(strings = { "select", "apiPath", "codeType", "codeValue" })
    void incompleteEditorDocumentsNeverReachTheService(String missingField) throws Exception {
        Map<String, Object> body = this.editor("api", 4);
        body.remove(missingField);
        assertEquals(400, this.post("/save-api", body).statusCode());
        verifyNoInteractions(this.service);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/publish", "/disable", "/delete", "/save-api" })
    void serviceVersionConflictsRemainHttp409AndDoNotReturnSuccess(String path) throws Exception {
        DatawayException conflict = new DatawayException(409, "stale version");
        when(this.service.publish("api", 4)).thenThrow(conflict);
        when(this.service.disableApi("api", 4)).thenThrow(conflict);
        doThrow(conflict).when(this.service).deleteApi("api", 4);
        when(this.service.save(any(ApiDefinition.class), eq(4L))).thenThrow(conflict);
        HttpResponse<String> response = this.post(path, this.editor("api", 4));
        assertEquals(409, response.statusCode());
        assertFalse(response.body().contains("\"success\":true"));
    }

    @Test
    void controllerParameterErrorsKeepTheOriginalFailureForTheHost() throws Exception {
        MemoryRequest request = new MemoryRequest();
        request.setQuery("id=%GG");
        request.json(Map.of("version", 4));
        DeleteController controller = new DeleteController(this.service);
        try (request) {
            DatawayException failure = assertThrows(DatawayException.class, () -> controller.handle(request, new MemoryResponse()));
            assertEquals(400, failure.status());
            assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        }
        verifyNoInteractions(this.service);
    }
}

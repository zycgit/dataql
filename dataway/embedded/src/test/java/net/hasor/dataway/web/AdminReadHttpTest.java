/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiRelease;
import net.hasor.dataway.service.DatawayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminReadHttpTest extends AdminHttpSupport {
    @ParameterizedTest
    @ValueSource(strings = { "/api-info", "/api-detail" })
    void detailsExposeTheConsoleContractFromAnAtomicDraftAndState(String path) throws Exception {
        Map<?, ?> detail = assertInstanceOf(Map.class, this.success(this.get(path + "?id=api")).get("result"));
        assertEquals("api", detail.get("id"));
        assertEquals(4, detail.get("version"));
        assertEquals(2, detail.get("status"));
        assertEquals("POST", detail.get("select"));
        assertEquals("/example", detail.get("path"));
        assertEquals("DataQL", detail.get("codeType"));
        assertEquals("Example description", detail.get("apiComment"));
        assertEquals(Map.of("codeValue", "return 'draft';", "requestBody", "{\"name\":\"example\"}", "headerData", List.of()), detail.get("codeInfo"));
        assertEquals(Map.of("resultStructure", false), detail.get("optionData"));
        assertEquals(Map.of("type", "object"), detail.get("schema"));
        verify(this.service).getVersionById("api");
        verify(this.service, never()).getHistoryByApi(anyString());
    }

    @Test
    void listReturnsSummariesWithoutScriptOrRequestExamples() throws Exception {
        when(this.service.list()).thenReturn(List.of(this.draft));
        List<?> result = assertInstanceOf(List.class, this.success(this.get("/api-list")).get("result"));
        assertEquals(List.of(Map.of("id", "api", "version", 4, "checked", false, "select", "POST", "path", "/example", "status", 2, "comment", "Example description")), result);
    }

    @Test
    void anEmptyCatalogReturnsAnEmptyList() throws Exception {
        assertEquals(List.of(), this.success(this.get("/api-list")).get("result"));
        verify(this.service, never()).getDraftByApi(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "?id=", "?id=%20", "?flag", "?id=api&id=api", "?id=api&%69d=other" })
    void invalidOrAmbiguousIdsAreRejectedBeforeAccessingStorage(String query) throws Exception {
        assertEquals(400, this.get("/api-detail" + query).statusCode());
        verifyNoInteractions(this.service);
    }

    @Test
    void queryValuesAreDecodedExactlyOnceAndPreserveEqualsAndPlus() throws Exception {
        String id = "中文+a b=c&d%GG";
        ApiDefinition definition = this.definition(id, "return 1;");
        when(this.service.getApiById(id)).thenReturn(this.state);
        when(this.service.getDraftByApi(id)).thenReturn(definition);
        when(this.service.getVersionById(id)).thenReturn(4L);
        Map<?, ?> detail = assertInstanceOf(Map.class, this.success(this.get("/api-detail?id=%E4%B8%AD%E6%96%87%2Ba+b%3Dc%26d%25GG&flag")).get("result"));
        assertEquals(id, detail.get("id"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api-list", "/api-info?id=api", "/api-detail?id=api", "/api-history?id=api", "/get-history?id=api&historyId=release" })
    void concurrentModificationNeverReturnsAMixedRevision(String path) throws Exception {
        when(this.service.list()).thenReturn(List.of(this.draft));
        when(this.service.getHistoryById("release")).thenReturn(this.release("release", this.draft));
        when(this.service.getVersionById("api")).thenReturn(5L);
        HttpResponse<String> response = this.get(path);
        assertEquals(409, response.statusCode(), response.body());
        assertFalse(response.body().contains("return 'draft'"));
    }

    @ParameterizedTest
    @CsvSource({ "true,true,1", "false,true,3", "true,false,3" })
    void historyIsNewestFirstAndOnlyTheEnabledCurrentReleaseIsPublished(boolean enabled, boolean hasRelease, int expectedStatus) throws Exception {
        this.state.setEnabled(enabled);
        ApiRelease old = this.release("old", this.draft);
        ApiRelease latest = this.release("latest", this.draft);
        List<ApiRelease> history = new ArrayList<>(List.of(old, latest));
        when(this.service.getHistoryByApi("api")).thenReturn(history);
        when(this.service.getReleaseByApi("api")).thenReturn(hasRelease ? latest : null);
        assertEquals(List.of(Map.of("historyId", "latest", "status", expectedStatus, "time", "2026-01-02 03:04:05"), Map.of("historyId", "old", "status", 3, "time", "2026-01-02 03:04:05")), this.success(this.get("/api-history?id=api")).get("result"));
        assertEquals(List.of(old, latest), history);
    }

    @Test
    void getHistoryReturnsTheReleaseScriptRatherThanTheCurrentDraft() throws Exception {
        ApiDefinition old = this.definition("api", "return 'released';");
        when(this.service.getHistoryById("old")).thenReturn(this.release("old", old));
        Map<?, ?> detail = assertInstanceOf(Map.class, this.success(this.get("/get-history?id=api&historyId=old")).get("result"));
        assertEquals("return 'released';", ((Map<?, ?>) detail.get("codeInfo")).get("codeValue"));
        verify(this.service, never()).getDraftByApi(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "&historyId=", "&historyId=%20" })
    void missingHistoryIdsReturnNotFound(String query) throws Exception {
        assertEquals(404, this.get("/get-history?id=api" + query).statusCode());
        verify(this.service, never()).getHistoryById(anyString());
    }

    @Test
    void historyFromAnotherApiIsNotExposed() throws Exception {
        when(this.service.getHistoryById("other-release")).thenReturn(this.release("other-release", this.definition("other", "return 'private';")));
        HttpResponse<String> response = this.get("/get-history?id=api&historyId=other-release");
        assertEquals(404, response.statusCode());
        assertFalse(response.body().contains("private"));
        verify(this.service, never()).getVersionById(anyString());
    }

    @Test
    void missingApisAndStorageFailuresAreTranslatedByTheHost() throws Exception {
        when(this.service.getApiById("api")).thenThrow(new DatawayException(404, "API not found"));
        assertEquals(404, this.get("/api-info?id=api").statusCode());
        when(this.service.list()).thenThrow(new IllegalStateException("storage unavailable"));
        assertEquals(500, this.get("/api-list").statusCode());
    }
}

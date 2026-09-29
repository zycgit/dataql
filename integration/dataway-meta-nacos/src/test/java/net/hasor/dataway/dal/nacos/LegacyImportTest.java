/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import java.util.LinkedHashMap;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.dal.DataAccessException;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LegacyImportTest {
    @Test
    void pagedLegacyImportKeepsOriginalScriptsAndCombinesSchemaSamplesAndHints() throws Exception {
        NacosFixture fixture = new NacosFixture();
        fixture.documents.put("legacy/INDEX_MONITOR", "stable");
        fixture.documents.put("legacy/INDEX_DIRECTORY_0", "\n i_draft ,GET,/legacy\n");
        fixture.documents.put("legacy/INDEX_DIRECTORY_1", "r_release,GET,/legacy\n end ");
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("id", "i_draft");
        source.put("method", "GET");
        source.put("path", "/legacy");
        source.put("script", "compiled fragment wrapper");
        source.put("script_ori", "select :id");
        source.put("API_ID", "old controller noise");
        source.put("RELEASE_TIME", "1");
        source.put("SCHEMA", "{\"extra\":true}");
        source.put("SAMPLE", "{}");
        source.put("OPTION", "{\"resultStructure\":false}");
        source.put("REQ_BODY_SCHEMA", "{\"type\":\"object\"}");
        source.put("REQ_BODY_SAMPLE", "{\"id\":1}");
        source.put("REQ_HEADER_SCHEMA", "{}");
        source.put("RES_BODY_SAMPLE", "{}");
        source.put("PREPARE_HINT", "hint FRAGMENT_SQL_CONNECTION = 'main'");
        fixture.documents.put("legacy/i_draft", JsonUtils.writeValueAsString(source));
        source.put("id", "r_release");
        source.put("API_ID", "i_draft");
        source.put("CREATE_TIME", "1");
        source.put("GMT_TIME", "2");
        fixture.documents.put("legacy/r_release", JsonUtils.writeValueAsString(source));
        Map<String, String> original = new LinkedHashMap<>(fixture.documents);
        fixture.access.importLegacy("legacy");
        var draft = fixture.access.getObject(EntityType.INFO, "i_draft").orElseThrow();
        var release = fixture.access.getObject(EntityType.RELEASE, "r_release").orElseThrow();
        assertEquals("select :id", draft.get(SCRIPT));
        assertEquals("1", draft.get(REVISION));
        assertFalse(draft.containsKey(API_ID));
        assertFalse(release.containsKey(CREATE_TIME));
        assertEquals("i_draft", release.get(API_ID));
        assertEquals("object", JsonUtils.readTree(draft.get(SCHEMA)).path("requestBody").path("type").asText());
        assertTrue(JsonUtils.readTree(draft.get(SCHEMA)).path("extra").asBoolean());
        assertEquals("{\"id\":1}", JsonUtils.readTree(draft.get(SAMPLE)).path("requestBody").asText());
        assertTrue(JsonUtils.readTree(draft.get(OPTION)).has("PREPARE_HINT"));
        original.forEach((key, value) -> {
            if (key.startsWith("legacy/")) {
                assertEquals(value, fixture.documents.get(key));
            }
        });
        assertThrows(DataConflictException.class, () -> fixture.access.importLegacy("legacy"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "bad-entry", "unknown,GET,/x\nEND", "i_a,GET,/a\ni_a,GET,/a\nEND", "i_a,GET,/a\nEND" })
    void incompleteDuplicateAndMissingRecordsNeverPublish(String directory) throws Exception {
        NacosFixture fixture = new NacosFixture();
        if (directory != null) {
            fixture.documents.put("legacy/INDEX_DIRECTORY_0", directory);
        }
        if (directory != null && directory.contains("i_a,GET,/a\ni_a")) {
            fixture.documents.put("legacy/i_a", "{\"ID\":\"i_a\",\"METHOD\":\"GET\",\"PATH\":\"/a\"}");
        }
        String before = fixture.content();
        assertThrows(DataAccessException.class, () -> fixture.access.importLegacy("legacy"));
        assertEquals(before, fixture.content());
        verify(fixture.client, never()).publishConfigCas(anyString(), anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "broken", "null", "{\"ID\":\"different\"}", "{\"ID\":\"i_a\",\"SCHEMA\":\"broken\"}" })
    void malformedLegacyDocumentsAreReportedWithTheRecordId(String document) throws Exception {
        NacosFixture fixture = new NacosFixture();
        fixture.documents.put("legacy/INDEX_DIRECTORY_0", "i_a,GET,/a\nEND");
        fixture.documents.put("legacy/i_a", document);
        DataAccessException failure = assertThrows(DataAccessException.class, () -> fixture.access.importLegacy("legacy"));
        assertTrue(failure.getMessage().contains("i_a"));
        verify(fixture.client, never()).publishConfigCas(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void legacyMonitorChangeAbortsTheImport() throws Exception {
        NacosFixture fixture = new NacosFixture();
        fixture.documents.put("legacy/INDEX_DIRECTORY_0", "END");
        when(fixture.client.getConfig("INDEX_MONITOR", "legacy", 2000)).thenReturn("before", "after");
        assertThrows(DataConflictException.class, () -> fixture.access.importLegacy("legacy"));
        verify(fixture.client, never()).publishConfigCas(anyString(), anyString(), anyString(), anyString());
    }
}

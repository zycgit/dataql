/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;
import net.hasor.dataway.dal.DataAccessException;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.OperationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SnapshotTest {
    @Test
    void snapshotRoundTripPreservesSchemaSamplesOptionsAndOriginalScript() throws Exception {
        NacosFixture fixture = new NacosFixture();
        NacosSnapshot snapshot = NacosSnapshot.empty();
        Map<FieldDef, String> fields = new EnumMap<>(NacosFixture.route("POST", "/roundtrip"));
        fields.put(SCHEMA, "{\"requestBody\":{\"type\":\"object\"}}");
        fields.put(SAMPLE, "{\"requestBody\":\"{\\\"value\\\":1}\"}");
        fields.put(OPTION, "{\"resultStructure\":false}");
        snapshot.apply(fixture.access.create(EntityType.INFO, OperationType.CREATE, "draft", 0, fields));
        NacosSnapshot parsed = NacosSnapshot.parse(snapshot.serialize());
        assertEquals(snapshot.getRecords(), parsed.getRecords());
        assertEquals(1, parsed.getFormat());
        assertEquals("initial", parsed.getGeneration());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @MethodSource("invalidSnapshots")
    void invalidSnapshotsAreRejectedWithoutPublishingOverThem(String document) throws Exception {
        NacosFixture fixture = new NacosFixture();
        fixture.content(document);
        assertThrows(DataAccessException.class, () -> NacosSnapshot.parse(document));
        assertThrows(DataAccessException.class, () -> fixture.access.createObject(EntityType.INFO, "draft", NacosFixture.route("GET", "/draft")));
        assertEquals(document, fixture.content());
        verify(fixture.client, never()).publishConfigCas(anyString(), anyString(), anyString(), anyString());
    }

    static Stream<String> invalidSnapshots() {
        String empty = NacosSnapshot.empty().serialize();
        return Stream.of(" ", "broken", "null", "[]", "{}", empty.replace("initial", ""), empty.replace("\"format\":1", "\"format\":2"), empty.replace("\"INFO\":{}", "\"INFO\":null"), empty.replace("RELEASE", "UNKNOWN"), empty.replace("\"INFO\":{}", "\"INFO\":{\"a\":{\"ID\":\"b\",\"REVISION\":\"1\"}}"), empty.replace("\"INFO\":{}", "\"INFO\":{\"a\":{\"ID\":\"a\",\"REVISION\":\"0\"}}"), empty.replace("\"INFO\":{}", "\"INFO\":{\"a\":{\"ID\":\"a\",\"REVISION\":\"1\"}}"), empty.replace("\"INFO\":{}", "\"INFO\":{\"a\":{\"ID\":\"a\",\"REVISION\":\"1\",\"unknown\":\"x\"}}"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " " })
    void blankClientCoordinatesAndStorageNamesAreRejected(String name) throws Exception {
        NacosFixture fixture = new NacosFixture();
        assertThrows(IllegalArgumentException.class, () -> new NacosDataAccessLayer(fixture.client, name, "group", 1));
        assertThrows(IllegalArgumentException.class, () -> new NacosDataAccessLayer(fixture.client, "data", name, 1));
        assertThrows(IllegalArgumentException.class, () -> fixture.access.importLegacy(name));
        Map<EntityType, String> tables = new EnumMap<>(EntityType.class);
        tables.put(EntityType.INFO, name);
        assertThrows(IllegalArgumentException.class, () -> fixture.access.configureMapping(tables, Map.of()));
        Map<FieldDef, String> fields = new EnumMap<>(FieldDef.class);
        fields.put(SCRIPT, name);
        assertThrows(IllegalArgumentException.class, () -> fixture.access.configureMapping(Map.of(), Map.of(EntityType.INFO, fields)));
        verifyNoInteractions(fixture.client);
    }

    @Test
    void clientAndTimeoutAreRequired() throws Exception {
        NacosFixture fixture = new NacosFixture();
        assertThrows(NullPointerException.class, () -> new NacosDataAccessLayer(null, "data", "group", 1));
        assertThrows(IllegalArgumentException.class, () -> new NacosDataAccessLayer(fixture.client, "data", "group", 0));
    }
}

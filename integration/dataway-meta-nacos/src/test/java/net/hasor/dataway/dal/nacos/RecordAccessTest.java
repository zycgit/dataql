/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.OperationType;
import org.junit.jupiter.api.Test;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RecordAccessTest {
    @Test
    void recordsRoundTripAcrossIndependentClientsAndNullUpdatesRemoveFields() throws Exception {
        NacosFixture fixture = new NacosFixture();
        var access = fixture.access;
        access.createObject(EntityType.INFO, "draft", NacosFixture.route("GET", "/api"));
        String initial = fixture.content();
        var fresh = fixture.reconnect();
        var row = fresh.getObject(EntityType.INFO, "draft").orElseThrow();
        assertEquals("return '中文';", row.get(SCRIPT));
        assertEquals("1", row.get(REVISION));
        row.put(SCRIPT, "caller mutation");
        assertEquals(initial, fixture.content());
        Map<FieldDef, String> update = new EnumMap<>(FieldDef.class);
        update.put(COMMENT, null);
        fresh.updateObject(EntityType.INFO, "draft", 1, update);
        assertNull(access.getObject(EntityType.INFO, "draft").orElseThrow().get(COMMENT));
        assertEquals(1, access.listObjects(EntityType.INFO, update).size());
        assertEquals("return '中文';", access.getObject(EntityType.INFO, "draft").orElseThrow().get(SCRIPT));
        assertNotEquals(initial, fixture.content());
        assertThrows(DataConflictException.class, () -> access.updateObject(EntityType.INFO, "draft", 1, Map.of(SCRIPT, "stale")));
        assertThrows(DataConflictException.class, () -> access.deleteObject(EntityType.INFO, "draft", 1));
        access.deleteObject(EntityType.INFO, "draft", 2);
        assertTrue(fresh.getObject(EntityType.INFO, "draft").isEmpty());
        assertThrows(DataConflictException.class, () -> access.deleteObject(EntityType.INFO, "draft", 2));
        verify(fixture.client, never()).shutDown();
    }

    @Test
    void routeUniquenessUsesMethodAndPathButAllowsReleaseHistory() throws Exception {
        NacosFixture fixture = new NacosFixture();
        var access = fixture.access;
        access.createObject(EntityType.INFO, "c", NacosFixture.route("GET", "/Case"));
        access.createObject(EntityType.INFO, "b", NacosFixture.route("POST", "/same"));
        access.createObject(EntityType.INFO, "a", NacosFixture.route("GET", "/same"));
        assertEquals(List.of("a", "b", "c"), access.listObjects(EntityType.INFO, Map.of()).stream().map(row -> row.get(ID)).toList());
        assertEquals(1, access.listObjects(EntityType.INFO, Map.of(METHOD, "GET", PATH, "/same")).size());
        assertTrue(access.listObjects(EntityType.INFO, Map.of(PATH, "/case")).isEmpty());
        String before = fixture.content();
        assertThrows(DataConflictException.class, () -> access.createObject(EntityType.INFO, "duplicate", NacosFixture.route("GET", "/same")));
        assertThrows(DataConflictException.class, () -> access.createObject(EntityType.INFO, "a", NacosFixture.route("GET", "/new")));
        assertEquals(before, fixture.content());
        access.createObject(EntityType.RELEASE, "r1", NacosFixture.route("GET", "/same"));
        access.createObject(EntityType.RELEASE, "r2", NacosFixture.route("GET", "/same"));
        assertEquals(2, access.listObjects(EntityType.RELEASE, Map.of(PATH, "/same")).size());
    }

    @Test
    void failedBatchDoesNotPublishPartialChangesAndSuccessfulBatchUsesOneCas() throws Exception {
        NacosFixture fixture = new NacosFixture();
        var access = fixture.access;
        access.createObject(EntityType.INFO, "draft", NacosFixture.route("GET", "/draft"));
        String before = fixture.content();
        clearInvocations(fixture.client);
        var create = access.create(EntityType.RELEASE, OperationType.CREATE, "release", 0, Map.of(API_ID, "draft", RELEASE_TIME, "1"));
        var stale = access.create(EntityType.INFO, OperationType.UPDATE, "draft", 99, Map.of(STATUS, "1"));
        assertThrows(DataConflictException.class, () -> access.write(List.of(create, stale)));
        assertEquals(before, fixture.content());
        verify(fixture.client, never()).publishConfigCas(anyString(), anyString(), anyString(), anyString());
        access.write(List.of(create, access.create(EntityType.INFO, OperationType.UPDATE, "draft", 1, Map.of(STATUS, "1"))));
        verify(fixture.client).publishConfigCas(eq("metadata"), eq("group"), anyString(), eq(NacosFixture.md5(before)));
        assertTrue(fixture.reconnect().getObject(EntityType.RELEASE, "release").isPresent());
        assertEquals("2", access.getObject(EntityType.INFO, "draft").orElseThrow().get(REVISION));
    }

    @Test
    void noOpAndInvalidEntityFieldsDoNotUseTheClient() throws Exception {
        NacosFixture fixture = new NacosFixture();
        fixture.access.write(List.of());
        assertThrows(IllegalArgumentException.class, () -> fixture.access.createObject(EntityType.INFO, "bad", Map.of(API_ID, "unsupported")));
        assertThrows(IllegalArgumentException.class, () -> fixture.access.listObjects(EntityType.RELEASE, Map.of(GMT_TIME, "unsupported")));
        verifyNoInteractions(fixture.client);
    }
}

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.OperationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

class RecordAccessTest {
    @ParameterizedTest
    @EnumSource(EntityType.class)
    void recordsRoundTripAndVersionsProtectUpdateAndDelete(EntityType type) throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            var access = fixture.access;
            var fields = type == EntityType.INFO ? JdbcFixture.info("GET", "/records") : JdbcFixture.release("api");
            fields.put(SCRIPT, "select :name; -- original SQL, ' and 中文");
            access.createObject(type, "record", fields);
            var initial = access.getObject(type, "record").orElseThrow();
            assertEquals("1", initial.get(REVISION));
            fields.forEach((key, value) -> assertEquals(value, initial.get(key)));
            initial.put(SCRIPT, "caller mutation");
            assertEquals(fields.get(SCRIPT), access.getObject(type, "record").orElseThrow().get(SCRIPT));
            access.updateObject(type, "record", 1, Map.of(COMMENT, "edited"));
            assertEquals("2", access.getObject(type, "record").orElseThrow().get(REVISION));
            assertEquals(fields.get(SCRIPT), access.getObject(type, "record").orElseThrow().get(SCRIPT));
            assertThrows(DataConflictException.class, () -> access.updateObject(type, "record", 1, Map.of(COMMENT, "stale")));
            assertThrows(DataConflictException.class, () -> access.deleteObject(type, "record", 1));
            assertThrows(DataConflictException.class, () -> access.createObject(type, "record", fields));
            access.deleteObject(type, "record", 2);
            assertTrue(access.getObject(type, "record").isEmpty());
            assertThrows(DataConflictException.class, () -> access.deleteObject(type, "record", 2));
        }
    }

    @Test
    void queriesCombineExactConditionsAndReturnRecordsInIdOrder() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            var access = fixture.access;
            access.createObject(EntityType.INFO, "c", JdbcFixture.info("GET", "/B"));
            access.createObject(EntityType.INFO, "b", JdbcFixture.info("POST", "/a"));
            access.createObject(EntityType.INFO, "a", JdbcFixture.info("GET", "/a"));
            assertEquals(List.of("a", "b", "c"), access.listObjects(EntityType.INFO, Map.of()).stream().map(row -> row.get(ID)).toList());
            assertEquals(1, access.listObjects(EntityType.INFO, Map.of(METHOD, "GET", PATH, "/a")).size());
            assertTrue(access.listObjects(EntityType.INFO, Map.of(PATH, "/b")).isEmpty());
            assertTrue(access.listObjects(EntityType.INFO, Map.of(PATH, "' OR 1=1 --")).isEmpty());
            assertThrows(DataConflictException.class, () -> access.createObject(EntityType.INFO, "duplicate", JdbcFixture.info("GET", "/a")));
            access.createObject(EntityType.RELEASE, "history-1", JdbcFixture.release("a"));
            access.createObject(EntityType.RELEASE, "history-2", JdbcFixture.release("a"));
            assertEquals(2, access.listObjects(EntityType.RELEASE, Map.of(API_ID, "a")).size());
        }
    }

    @Test
    void explicitNullClearsNullableLegacyColumnsAndNullConditionsUseIsNull() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            fixture.sql("ALTER TABLE interface_info ALTER COLUMN api_comment DROP NOT NULL");
            fixture.access.createObject(EntityType.INFO, "record", JdbcFixture.info("GET", "/null"));
            Map<FieldDef, String> clear = new EnumMap<>(FieldDef.class);
            clear.put(COMMENT, null);
            fixture.access.updateObject(EntityType.INFO, "record", 1, clear);
            var rows = fixture.access.listObjects(EntityType.INFO, clear);
            assertEquals(1, rows.size());
            assertNull(rows.get(0).get(COMMENT));
            assertEquals("2", rows.get(0).get(REVISION));
        }
    }

    @Test
    void batchRollbackCoversBothSqlAndOptimisticConflicts() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            var access = fixture.access;
            access.createObject(EntityType.INFO, "draft", JdbcFixture.info("GET", "/draft"));
            for (boolean duplicate : new boolean[] { false, true }) {
                var first = access.create(EntityType.RELEASE, OperationType.CREATE, "snapshot", 0, JdbcFixture.release("draft"));
                var second = duplicate ? access.create(EntityType.INFO, OperationType.CREATE, "other", 0, JdbcFixture.info("GET", "/draft")) : access.create(EntityType.INFO, OperationType.UPDATE, "draft", 99, Map.of(COMMENT, "stale"));
                assertThrows(DataConflictException.class, () -> access.write(List.of(first, second)));
                assertTrue(access.listObjects(EntityType.RELEASE, Map.of()).isEmpty());
                assertEquals("1", access.getObject(EntityType.INFO, "draft").orElseThrow().get(REVISION));
            }
            access.write(List.of(access.create(EntityType.RELEASE, OperationType.CREATE, "snapshot", 0, JdbcFixture.release("draft")), access.create(EntityType.INFO, OperationType.UPDATE, "draft", 1, Map.of(STATUS, "1"))));
            assertEquals(1, access.listObjects(EntityType.RELEASE, Map.of()).size());
            assertEquals("2", access.getObject(EntityType.INFO, "draft").orElseThrow().get(REVISION));
        }
    }
}

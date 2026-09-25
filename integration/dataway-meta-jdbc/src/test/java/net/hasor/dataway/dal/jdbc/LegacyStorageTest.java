/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import net.hasor.dataway.TestDatabase;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.dal.*;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.CallContext;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.FxRuntime;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

class LegacyStorageTest {
    private void script(JdbcDataSource source, String name) throws Exception {
        try (var connection = source.getConnection();           //
             var input = getClass().getResourceAsStream(name);  //
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
    }

    private Map<FieldDef, String> fields(String method, String path) {
        Map<FieldDef, String> data = new EnumMap<>(FieldDef.class);
        data.put(METHOD, method);
        data.put(PATH, path);
        data.put(STATUS, "0");
        data.put(COMMENT, "legacy");
        data.put(TYPE, "SQL");
        data.put(SCRIPT, "SELECT :value");
        data.put(SCHEMA, "{\"requestSchema\":{\"custom\":true},\"unknown\":42}");
        data.put(SAMPLE, "{\"requestBody\":{\"value\":7},\"headerData\":[],\"custom\":1}");
        data.put(OPTION, "{\"custom\":{\"a\":1},\"resultStructure\":false}");
        data.put(CREATE_TIME, "1600000000000");
        data.put(GMT_TIME, "1600000000000");
        return data;
    }

    @Test
    void oldSchemaUpgradePreservesFieldsAndExecutesStoredReleaseScript() throws Exception {
        var source = TestDatabase.empty();
        script(source, "/legacy-h2.sql");
        // Seed before adding revisions: these rows have precisely the legacy layout.
        try (var connection = source.getConnection()) {
            var fields = fields("GET", "/legacy");
            try (var statement = connection.prepareStatement("INSERT INTO interface_info VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")) {
                Object[] values = { "i_old", "GET", "/legacy", "1", "legacy", "SQL", fields.get(SCRIPT), fields.get(SCHEMA), fields.get(SAMPLE), fields.get(OPTION), "1600000000000", "1600000000000" };
                for (int i = 0; i < values.length; i++) {
                    statement.setObject(i + 1, values[i]);
                }
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("INSERT INTO interface_release VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                Object[] values = { "r_old", "i_old", "GET", "/legacy", "1", "legacy", "SQL", "return 42;", fields.get(SCRIPT), fields.get(SCHEMA), fields.get(SAMPLE), fields.get(OPTION), "1600000000000" };
                for (int i = 0; i < values.length; i++) {
                    statement.setObject(i + 1, values[i]);
                }
                statement.executeUpdate();
            }
        }
        script(source, "/META-INF/dataway/schema/upgrade/h2.sql");
        var access = new JdbcDataAccessLayer(source, "");
        var service = DatawayService.builder(FxRuntime.builder().dataSource(source).build(), access).build();
        assertEquals(42, ((Number) service.invokeApi("/legacy", Map.of())).intValue());
        var before = access.getObject(EntityType.INFO, "i_old").orElseThrow();
        assertEquals("1", before.get(REVISION));
        assertEquals("SELECT :value", service.getApiById("i_old", CallContext.local(Operation.READ)).getDraft().getScript());
        ApiDefinition i_oldApi = new ApiDefinition();
        i_oldApi.setId("i_old");
        i_oldApi.setMethod("GET");
        i_oldApi.setPath("/legacy");
        i_oldApi.setType(ApiScriptType.SQL);
        i_oldApi.setScript("SELECT :value + 1");
        i_oldApi.setDescription("edited");
        service.save(i_oldApi, 1, CallContext.local(Operation.SAVE));
        var after = access.getObject(EntityType.INFO, "i_old").orElseThrow();
        for (FieldDef field : List.of(SCHEMA, SAMPLE, OPTION, CREATE_TIME)) {
            assertEquals(before.get(field), after.get(field));
        }
        assertEquals(42, ((Number) service.invokeApi("/legacy", Map.of())).intValue());
        service.publish("i_old", 2, CallContext.local(Operation.PUBLISH));
        assertEquals(10, ((Number) service.invokeApi("/legacy", Map.of("value", 9))).intValue());
        var active = access.listObjects(EntityType.RELEASE, Map.of(API_ID, "i_old", STATUS, "1")).getFirst();
        assertEquals("SELECT :value + 1", active.get(SCRIPT_ORI));
        assertTrue(active.get(SCRIPT).startsWith("var tempCall = @@sql(`value`)"));
        assertEquals(before.get(OPTION), active.get(OPTION));
        assertEquals(2, service.history("i_old", CallContext.local(Operation.HISTORY)).size());
        assertEquals("3", access.getObject(EntityType.RELEASE, "r_old").orElseThrow().get(STATUS));
        // The upgraded unique index permits a second method at the same path.
        access.createObject(EntityType.INFO, "i_post", fields("POST", "/legacy"));
        assertEquals(2, access.listObjects(EntityType.INFO, Map.of(PATH, "/legacy")).size());
    }

    @Test
    void batchesRollbackEarlierChangesWhenLaterMutationConflicts() {
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        access.createObject(EntityType.INFO, "one", fields("GET", "/one"));
        access.createObject(EntityType.INFO, "two", fields("POST", "/one"));
        assertThrows(DataConflictException.class, () -> access.write(List.of(access.create(EntityType.INFO, OperationType.UPDATE, "one", 1, Map.of(COMMENT, "changed")), access.create(EntityType.INFO, OperationType.UPDATE, "two", 9, Map.of(COMMENT, "stale")))));
        assertEquals("legacy", access.getObject(EntityType.INFO, "one").orElseThrow().get(COMMENT));
        assertEquals("1", access.getObject(EntityType.INFO, "one").orElseThrow().get(REVISION));
        assertThrows(DataConflictException.class, () -> access.write(List.of(access.create(EntityType.INFO, OperationType.DELETE, "one", 1, Map.of()), access.create(EntityType.INFO, OperationType.CREATE, "duplicate", 0, fields("POST", "/one")))));
        assertTrue(access.getObject(EntityType.INFO, "one").isPresent());
        assertEquals(2, access.listObjects(EntityType.INFO, Map.of()).size());
    }

    @Test
    void partialUpdatesPreserveDocumentsAndNullIsNotTreatedAsAbsent() {
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        access.createObject(EntityType.INFO, "one", fields("GET", "/one"));
        String original = access.getObject(EntityType.INFO, "one").orElseThrow().get(SAMPLE);
        access.updateObject(EntityType.INFO, "one", 1, Map.of(COMMENT, "changed"));
        assertEquals(original, access.getObject(EntityType.INFO, "one").orElseThrow().get(SAMPLE));
        Map<FieldDef, String> clear = new EnumMap<>(FieldDef.class);
        clear.put(SAMPLE, null);
        // Legacy NOT NULL constraint rejects an explicit clear, rather than silently ignoring it.
        assertThrows(DataAccessException.class, () -> access.updateObject(EntityType.INFO, "one", 2, clear));
        assertEquals("2", access.getObject(EntityType.INFO, "one").orElseThrow().get(REVISION));
        assertThrows(IllegalArgumentException.class, () -> access.updateObject(EntityType.INFO, "one", 2, Map.of(API_ID, "bad")));
        assertEquals("2", access.getObject(EntityType.INFO, "one").orElseThrow().get(REVISION));
    }

    @Test
    void tablePrefixIsValidatedAndUsedForBothEntities() throws Exception {
        var source = TestDatabase.create();
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE interface_info RENAME TO tenant_interface_info");
            statement.execute("ALTER TABLE interface_release RENAME TO tenant_interface_release");
        }
        var access = new JdbcDataAccessLayer(source, "tenant_");
        var service = DatawayService.builder(FxRuntime.builder().build(), access).build();
        ApiDefinition oneApi = new ApiDefinition();
        oneApi.setId("one");
        oneApi.setMethod("GET");
        oneApi.setPath("/one");
        oneApi.setType(ApiScriptType.DATAQL);
        oneApi.setScript("return true;");
        oneApi.setDescription("");
        service.save(oneApi, 0, CallContext.local(Operation.SAVE));
        service.publish("one", 1, CallContext.local(Operation.PUBLISH));
        assertEquals(true, service.invokeApi("/one", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new JdbcDataAccessLayer(source, "x;DROP TABLE "));
    }

    @Test
    void newSqlApiPublishesSampleParametersAndKeepsMetadataOnOrdinaryEdits() throws Exception {
        var source = TestDatabase.create();
        var access = new JdbcDataAccessLayer(source, "");
        var service = DatawayService.builder(FxRuntime.builder().dataSource(source).build(), access).build();
        String sample = "{\"requestBody\":{\"value\":0},\"responseBody\":{},\"unknown\":true}";
        var sql = new ApiDefinition();
        sql.setId("sql");
        sql.setMethod("POST");
        sql.setPath("/same");
        sql.setType(ApiScriptType.SQL);
        sql.setScript("SELECT :value + 1");
        sql.setDescription("");
        sql.setSchema("{}");
        sql.setSample(sample);
        sql.setOptions("{\"hostOption\":true}");
        service.save(sql, 0, CallContext.local(Operation.SAVE));
        service.publish("sql", 1, CallContext.local(Operation.PUBLISH));
        assertEquals(9, ((Number) service.invokeApi("POST", "/same", Map.of("value", 8))).intValue());
        ApiDefinition getApi = new ApiDefinition();
        getApi.setId("get");
        getApi.setMethod("GET");
        getApi.setPath("/same");
        getApi.setType(ApiScriptType.DATAQL);
        getApi.setScript("return 'GET';");
        getApi.setDescription("");
        service.save(getApi, 0, CallContext.local(Operation.SAVE));
        service.publish("get", 1, CallContext.local(Operation.PUBLISH));
        assertEquals("GET", service.invokeApi("GET", "/same", Map.of()));
        ApiDefinition sqlApi = new ApiDefinition();
        sqlApi.setId("sql");
        sqlApi.setMethod("POST");
        sqlApi.setPath("/same");
        sqlApi.setType(ApiScriptType.SQL);
        sqlApi.setScript("SELECT :value + 2");
        sqlApi.setDescription("edit");
        service.save(sqlApi, 2, CallContext.local(Operation.SAVE));
        assertEquals(sample, service.getApiById("sql", CallContext.local(Operation.READ)).getDraft().getSample());
        assertEquals("{\"hostOption\":true}", service.getApiById("sql", CallContext.local(Operation.READ)).getDraft().getOptions());
    }

    @Test
    void failedReleaseInsertionRollsBackInfoAndPreviousReleaseTogether() {
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        var service = DatawayService.builder(FxRuntime.builder().build(), access).build();
        ApiDefinition oneApi = new ApiDefinition();
        oneApi.setId("one");
        oneApi.setMethod("GET");
        oneApi.setPath("/one");
        oneApi.setType(ApiScriptType.DATAQL);
        oneApi.setScript("return 1;");
        oneApi.setDescription("");
        service.save(oneApi, 0, CallContext.local(Operation.SAVE));
        var state = service.publish("one", 1, CallContext.local(Operation.PUBLISH));
        var old = access.getObject(EntityType.RELEASE, state.getPublished().getId()).orElseThrow();
        Map<FieldDef, String> duplicate = new EnumMap<>(old);
        duplicate.remove(ID);
        duplicate.remove(REVISION);
        assertThrows(DataConflictException.class, () -> access.write(List.of(access.create(EntityType.INFO, OperationType.UPDATE, "one", 2, Map.of(COMMENT, "not committed")), access.create(EntityType.RELEASE, OperationType.UPDATE, old.get(ID), 1, Map.of(STATUS, "3")), access.create(EntityType.RELEASE, OperationType.CREATE, old.get(ID), 0, duplicate))));
        assertEquals("2", access.getObject(EntityType.INFO, "one").orElseThrow().get(REVISION));
        assertEquals("1", access.getObject(EntityType.RELEASE, old.get(ID)).orElseThrow().get(REVISION));
        assertEquals("1", access.getObject(EntityType.RELEASE, old.get(ID)).orElseThrow().get(STATUS));
    }

    @Test
    void publicationsRemainOrderedWhenClockDoesNotAdvance() {
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        var service = DatawayService.builder(FxRuntime.builder().build(), access).clock(Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC)).build();
        ApiDefinition oneApi = new ApiDefinition();
        oneApi.setId("one");
        oneApi.setMethod("GET");
        oneApi.setPath("/one");
        oneApi.setType(ApiScriptType.DATAQL);
        oneApi.setScript("return 1;");
        oneApi.setDescription("");
        service.save(oneApi, 0, CallContext.local(Operation.SAVE));
        service.publish("one", 1, CallContext.local(Operation.PUBLISH));
        service.publish("one", 2, CallContext.local(Operation.PUBLISH));
        service.disableApi("one", 3, CallContext.local(Operation.DISABLE));
        var history = service.history("one", CallContext.local(Operation.HISTORY));
        assertEquals(2, history.size());
        assertTrue(history.get(1).getPublishedAt().isAfter(history.get(0).getPublishedAt()));
        assertEquals(history.get(1), service.getApiById("one", CallContext.local(Operation.READ)).getPublished());
    }

    @Test
    void factoryEntriesUseFieldsAndInvalidBatchesDoNotChangeData() {
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        DataMutation create = access.create();
        create.setEntityType(EntityType.INFO);
        create.setOperationType(OperationType.CREATE);
        create.setId("one");
        create.setFields(fields("GET", "/one"));
        access.write(List.of(create));
        assertEquals("1", access.getObject(EntityType.INFO, "one").orElseThrow().get(REVISION));

        var invalid = access.create(EntityType.INFO, OperationType.UPDATE, "one", 1, Map.of());
        invalid.setVersion(-1);
        var update = access.create(EntityType.INFO, OperationType.UPDATE, "one", 1, Map.of(COMMENT, "changed"));
        assertThrows(IllegalArgumentException.class, () -> access.write(List.of(update, invalid)));
        assertEquals("legacy", access.getObject(EntityType.INFO, "one").orElseThrow().get(COMMENT));
        assertEquals("1", access.getObject(EntityType.INFO, "one").orElseThrow().get(REVISION));

        invalid.setVersion(1);
        invalid.setFields(Map.of(REVISION, "100"));
        assertThrows(IllegalArgumentException.class, () -> access.write(List.of(invalid)));
    }

    @Test
    void databaseProviderCanOverrideTheMutationFactory() throws Exception {
        var created = new AtomicInteger();
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "") {
            @Override
            public DataMutation create() {
                created.incrementAndGet();
                return new HostMutation();
            }
        };
        assertInstanceOf(HostMutation.class, access.create(EntityType.INFO, OperationType.UPDATE, "one", 1, Map.of()));
        created.set(0);
        var service = DatawayService.builder(FxRuntime.builder().build(), access).build();
        ApiDefinition oneApi = new ApiDefinition();
        oneApi.setId("one");
        oneApi.setMethod("GET");
        oneApi.setPath("/one");
        oneApi.setType(ApiScriptType.DATAQL);
        oneApi.setScript("return true;");
        oneApi.setDescription("");
        service.save(oneApi, 0, CallContext.local(Operation.SAVE));
        service.publish("one", 1, CallContext.local(Operation.PUBLISH));
        assertEquals(3, created.get());
        assertEquals(true, service.invokeApi("/one", Map.of()));
    }

    private static final class HostMutation extends DataMutation {
    }

    @Test
    void unsupportedFieldsAreRejectedBeforeObtainingAConnection() {
        var source = (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { DataSource.class }, (proxy, method, args) -> {
            throw new AssertionError("Datasource must not be accessed: " + method.getName());
        });
        var access = new JdbcDataAccessLayer(source, "");
        var valid = access.create(EntityType.INFO, OperationType.UPDATE, "one", 1, Map.of(COMMENT, "changed"));
        var invalid = access.create(EntityType.INFO, OperationType.UPDATE, "two", 1, Map.of(API_ID, "unsupported"));
        assertThrows(IllegalArgumentException.class, () -> access.write(List.of(valid, invalid)));
        assertThrows(IllegalArgumentException.class, () -> access.listObjects(EntityType.INFO, Map.of(API_ID, "unsupported")));
        var invalidRelease = access.create(EntityType.RELEASE, OperationType.UPDATE, "release", 1, Map.of(CREATE_TIME, "0"));
        assertThrows(IllegalArgumentException.class, () -> access.write(List.of(invalidRelease)));
    }

    @Test
    void standaloneContextJoinsItsOuterScopeAndRecoversAfterRollback() throws Exception {
        var source = TestDatabase.create();
        var database = new LocalJdbcExecutor(source);
        var access = new JdbcDataAccessLayer(database, "");
        assertThrows(IllegalStateException.class, () -> database.execute(connection -> {
            access.createObject(EntityType.INFO, "one", fields("GET", "/one"));
            assertTrue(access.getObject(EntityType.INFO, "one").isPresent());
            throw new IllegalStateException("outer failure");
        }));
        assertTrue(access.getObject(EntityType.INFO, "one").isEmpty());
        access.createObject(EntityType.INFO, "one", fields("GET", "/one"));
        assertThrows(DataConflictException.class, () -> access.updateObject(EntityType.INFO, "one", 99, Map.of(COMMENT, "stale")));
        access.updateObject(EntityType.INFO, "one", 1, Map.of(COMMENT, "next batch"));
        assertEquals("next batch", access.getObject(EntityType.INFO, "one").orElseThrow().get(COMMENT));
    }

    @Test
    void executorOwnsConnectionAndWrapsEachQueryOrWholeBatch() throws Exception {
        var source = TestDatabase.create();
        var host = new LocalJdbcExecutor(source);
        var calls = new AtomicInteger();
        JdbcExecutor executor = new JdbcExecutor() {
            @Override
            public <T> T execute(JdbcCallback<T> callback) throws SQLException {
                calls.incrementAndGet();
                return host.execute(connection -> {
                    var borrowed = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Connection.class }, (proxy, method, args) -> {
                        assertFalse(Set.of("close", "commit", "rollback", "setAutoCommit").contains(method.getName()), "DAL must not control the supplied connection: " + method.getName());
                        try {
                            return method.invoke(connection, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
                    T result = callback.execute(borrowed);
                    assertFalse(connection.isClosed());
                    return result;
                });
            }
        };
        var access = new JdbcDataAccessLayer(executor, "");
        assertEquals(0, calls.get());
        access.write(List.of(access.create(EntityType.INFO, OperationType.CREATE, "one", 0, fields("GET", "/one")), access.create(EntityType.INFO, OperationType.CREATE, "two", 0, fields("GET", "/two"))));
        assertEquals(1, calls.get());
        assertEquals(2, access.listObjects(EntityType.INFO, Map.of()).size());
        assertEquals(2, calls.get());
    }

}

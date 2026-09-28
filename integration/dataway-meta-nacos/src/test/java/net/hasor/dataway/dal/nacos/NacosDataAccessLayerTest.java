/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.TestWebRequest;
import net.hasor.dataway.TestWebResponse;
import net.hasor.dataway.dal.*;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

class NacosDataAccessLayerTest {
    static final String EMPTY = "{\"format\":1,\"generation\":\"initial\",\"records\":{\"INFO\":{},\"RELEASE\":{}}}";

    @ParameterizedTest
    @ValueSource(strings = { "/swagger2.json", "/openapi.json" })
    void documentsUseMappedNacosPublicationsWithoutWritingOrExecuting(String path) throws Exception {
        Server server = new Server();
        server.configs.put("test/store", "{\"format\":1,\"generation\":\"initial\",\"records\":{\"drafts.v1\":{},\"release history\":{}}}");
        Dataway dataway = this.mappedConfig(server.access()).createDataway();
        ApiDefinition definition = new ApiDefinition();
        definition.setId("docs");
        definition.setMethod("GET");
        definition.setPath("/docs-test");
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript("invalid script that must never execute");
        definition.setDescription("Published document");
        dataway.getAdminService().save(definition, 0);
        dataway.getAdminService().publish("docs", 1);
        String original = server.configs.get("test/store");
        TestWebResponse response = new TestWebResponse();
        dataway.getDocumentHandler().handle(new TestWebRequest("GET", path, Map.of()), response);
        var document = JsonUtils.readTree(JsonUtils.writeValueAsString(response.getResult()));
        assertEquals("Published document", document.path("paths").path("/docs-test").path("get").path("summary").asText());
        assertEquals(original, server.configs.get("test/store"));
        dataway.getAdminService().disableApi("docs", 2);
        response = new TestWebResponse();
        dataway.getDocumentHandler().handle(new TestWebRequest("GET", path, Map.of()), response);
        assertTrue(JsonUtils.readTree(JsonUtils.writeValueAsString(response.getResult())).path("paths").isEmpty());
    }

    @Test
    void allEntityAndFieldMappingsSupportPublicationExecutionAndReconnect() throws Exception {
        Server server = new Server();
        server.configs.put("test/store", "{\"format\":1,\"generation\":\"initial\",\"records\":{\"drafts.v1\":{},\"release history\":{}}}");
        var access = server.access();
        var config = this.mappedConfig(access);
        var dataway = config.createDataway();
        var admin = dataway.getAdminService();
        var definition = new ApiDefinition();
        definition.setId("one");
        definition.setMethod("GET");
        definition.setPath("/mapped");
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript("return ${value};");
        definition.setDescription("Mapped API");
        definition.setSchema("{\"type\":\"object\"}");
        definition.setSample("{\"requestBody\":{\"value\":0}}");
        definition.setOptions("{\"resultStructure\":false}");
        admin.save(definition, 0);
        admin.publish("one", 1);
        String firstRelease = admin.getReleaseByApi("one").getId();
        definition.setScript("return ${value} + 1;");
        admin.save(definition, 2);
        admin.publish("one", 3);
        assertEquals(2, admin.getHistoryByApi("one").size());
        assertEquals("return ${value};", admin.getHistoryById(firstRelease).getDefinition().getScript());
        assertEquals(definition, admin.getDraftByApi("one"));

        var document = JsonUtils.readTree(server.configs.get("test/store"));
        assertFalse(document.get("records").has("INFO"));
        assertFalse(document.get("records").has("RELEASE"));
        var stored = document.get("records").get("drafts.v1").get("one");
        assertEquals("4", stored.get("stored_REVISION").asText());
        assertEquals(definition.getScript(), stored.get("source.code").asText());
        assertFalse(stored.has("SCRIPT"));
        assertFalse(stored.has("ID"));

        // Configuration edits do not alter the storage mapping already in use.
        config.tableMapping(EntityType.INFO, "other").fieldMapping(EntityType.INFO, SCRIPT, "other_script");
        assertEquals(1, access.listObjects(EntityType.INFO, Map.of(METHOD, "GET", PATH, "/mapped")).size());
        var restarted = this.mappedConfig(server.access()).createDataway();
        var response = new TestWebResponse();
        restarted.getApiHandler().handle(new TestWebRequest("GET", "/mapped", Map.of("value", 7)), response);
        assertEquals(8, ((Number) response.getResult()).intValue());
        restarted.getAdminService().disableApi("one", 4);
        assertFalse(admin.getApiById("one").isEnabled());
        assertThrows(DataConflictException.class, () -> access.deleteObject(EntityType.INFO, "one", 4));
        restarted.getAdminService().deleteApi("one", 5);
        assertTrue(admin.list().isEmpty());
        assertTrue(access.listObjects(EntityType.RELEASE, Map.of()).isEmpty());
    }

    private DatawayConfig mappedConfig(NacosDataAccessLayer access) {
        var config = new DatawayConfig().dataAccessLayer(access).tableMapping(EntityType.INFO, "drafts.v1").tableMapping(EntityType.RELEASE, "release history");
        for (FieldDef field : EnumSet.complementOf(EnumSet.of(API_ID, RELEASE_TIME))) {
            config.fieldMapping(EntityType.INFO, field, "stored_" + field.name());
        }
        for (FieldDef field : EnumSet.complementOf(EnumSet.of(CREATE_TIME, GMT_TIME))) {
            config.fieldMapping(EntityType.RELEASE, field, "release_" + field.name());
        }
        return config.fieldMapping(EntityType.INFO, SCRIPT, "source.code").fieldMapping(EntityType.RELEASE, SCRIPT, "SCRIPT_ORI");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "return 'compiled';" })
    void mappedOriginalScriptsPreserveTheUnusedOldField(String oldScript) {
        Server server = new Server();
        Object oldValue = oldScript == null ? JsonUtils.readTree("null") : oldScript;
        Map<String, Object> row = Map.of("ID", "sql", "REVISION", "1", "SCRIPT", oldValue, "SCRIPT_ORI", "SELECT :value");
        server.configs.put("test/store", JsonUtils.writeValueAsString(Map.of("format", 1, "generation", "old", "records", Map.of("INFO", Map.of(), "RELEASE", Map.of("sql", row)))));
        var access = server.access();
        new DatawayConfig().dataAccessLayer(access).fieldMapping(EntityType.RELEASE, SCRIPT, "SCRIPT_ORI").createDataway();
        assertEquals("SELECT :value", access.getObject(EntityType.RELEASE, "sql").orElseThrow().get(SCRIPT));
        access.updateObject(EntityType.RELEASE, "sql", 1, Map.of(COMMENT, "updated"));
        access.updateObject(EntityType.RELEASE, "sql", 2, Map.of(SCRIPT, "SELECT :value + 1"));
        var stored = JsonUtils.readTree(server.configs.get("test/store")).get("records").get("RELEASE").get("sql");
        assertEquals(JsonUtils.readTree(JsonUtils.writeValueAsString(oldValue)), stored.get("SCRIPT"));
        assertEquals("SELECT :value + 1", stored.get("SCRIPT_ORI").asText());
        Map<FieldDef, String> clear = new EnumMap<>(FieldDef.class);
        clear.put(SCRIPT, null);
        access.updateObject(EntityType.RELEASE, "sql", 3, clear);
        assertNull(access.getObject(EntityType.RELEASE, "sql").orElseThrow().get(SCRIPT));
        stored = JsonUtils.readTree(server.configs.get("test/store")).get("records").get("RELEASE").get("sql");
        assertFalse(stored.has("SCRIPT_ORI"));
        assertTrue(stored.has("SCRIPT"));

        access.write(List.of(access.create(EntityType.RELEASE, OperationType.DELETE, "sql", 4, Map.of()), access.create(EntityType.RELEASE, OperationType.CREATE, "sql", 0, Map.of(SCRIPT, "SELECT 1"))));
        stored = JsonUtils.readTree(server.configs.get("test/store")).get("records").get("RELEASE").get("sql");
        assertFalse(stored.has("SCRIPT"));
        assertEquals("SELECT 1", stored.get("SCRIPT_ORI").asText());
        access.deleteObject(EntityType.RELEASE, "sql", 1);
        assertTrue(access.getObject(EntityType.RELEASE, "sql").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " " })
    void blankStorageNamesFailBeforeReadingNacos(String name) {
        Server server = new Server();
        server.failRead = true;
        var table = new DatawayConfig().dataAccessLayer(server.access()).tableMapping(EntityType.INFO, name);
        var field = new DatawayConfig().dataAccessLayer(server.access()).fieldMapping(EntityType.RELEASE, SCRIPT, name);
        assertThrows(IllegalArgumentException.class, table::createDataway);
        assertThrows(IllegalArgumentException.class, field::createDataway);
        assertEquals(0, server.publications.get());
    }

    @Test
    void ambiguousMappingsAndUnsupportedFieldsLeaveThePreviousMappingIntact() {
        Server server = new Server();
        var access = server.access();
        new DatawayConfig().dataAccessLayer(access).createDataway();
        var duplicateEntity = new DatawayConfig().dataAccessLayer(access).tableMapping(EntityType.INFO, "RELEASE");
        var duplicateField = new DatawayConfig().dataAccessLayer(access).fieldMapping(EntityType.INFO, SCRIPT, "ID");
        var unsupportedField = new DatawayConfig().dataAccessLayer(access).fieldMapping(EntityType.INFO, API_ID, "source_id");
        assertThrows(IllegalArgumentException.class, duplicateEntity::createDataway);
        assertThrows(IllegalArgumentException.class, duplicateField::createDataway);
        assertThrows(IllegalArgumentException.class, unsupportedField::createDataway);
        access.createObject(EntityType.INFO, "one", this.route("GET", "/one"));
        assertEquals("return 1;", access.getObject(EntityType.INFO, "one").orElseThrow().get(SCRIPT));
    }

    @Test
    void renamedEntitiesMustExistAndUnknownFieldsStillPreventWrites() {
        Server server = new Server();
        var access = server.access();
        this.mappedConfig(access).createDataway();
        assertThrows(DataAccessException.class, () -> access.createObject(EntityType.INFO, "one", this.route("GET", "/one")));
        server.configs.put("test/store", "{\"format\":1,\"generation\":\"old\",\"records\":{\"drafts.v1\":{},\"release history\":{\"one\":{\"release_ID\":\"one\",\"release_REVISION\":\"1\",\"unknown\":\"value\"}}}}");
        String before = server.configs.get("test/store");
        assertThrows(DataAccessException.class, () -> access.updateObject(EntityType.RELEASE, "one", 1, Map.of(COMMENT, "updated")));
        assertEquals(before, server.configs.get("test/store"));
        assertEquals(0, server.publications.get());
    }

    @Test
    void snapshotObjectsRoundTripAndReadTheExistingWireFormat() {
        var snapshot = NacosSnapshot.empty();
        assertEquals(NacosSnapshot.parse(EMPTY).getRecords(), snapshot.getRecords());
        var access = new Server().access();
        var fields = new EnumMap<FieldDef, String>(FieldDef.class);
        fields.put(METHOD, "GET");
        fields.put(PATH, "/object");
        fields.put(SCRIPT, "return '对象';");
        fields.put(COMMENT, "对象说明");
        snapshot.apply(access.create(EntityType.INFO, OperationType.CREATE, "object", 0, fields));
        String json = snapshot.serialize();
        var restored = NacosSnapshot.parse(json);
        assertEquals(snapshot.getRecords(), restored.getRecords());
        assertEquals(snapshot.getGeneration(), restored.getGeneration());
        assertTrue(restored.getRecords().get(EntityType.INFO).get("object").containsKey(COMMENT));
        assertEquals(json, snapshot.serialize());
    }

    @Test
    void objectDeserializationStillRejectsIncompleteAndUnknownEntities() {
        for (String invalid : List.of("{\"format\":1,\"records\":{\"INFO\":{},\"RELEASE\":{}}}", "{\"generation\":\"initial\",\"records\":{\"INFO\":{},\"RELEASE\":{}}}", EMPTY.replace("\"INFO\":{}", "\"INFO\":null"), EMPTY.replace("RELEASE", "UNKNOWN"))) {
            assertThrows(DataAccessException.class, () -> NacosSnapshot.parse(invalid));
        }
    }

    @Test
    void legacyScriptFieldsRequireExplicitMappingOrMigration() {
        Server server = new Server();
        Map<String, Object> releases = Map.of("sql", Map.of("ID", "sql", "REVISION", "1", "SCRIPT", "return 'compiled';", "SCRIPT_ORI", "SELECT :value"));
        String original = JsonUtils.writeValueAsString(Map.of("format", 1, "generation", "old", "records", Map.of("INFO", Map.of(), "RELEASE", releases)));
        server.configs.put("test/store", original);
        var access = server.access();
        assertThrows(DataAccessException.class, () -> access.getObject(EntityType.RELEASE, "sql"));
        assertThrows(DataAccessException.class, () -> access.updateObject(EntityType.RELEASE, "sql", 1, Map.of(COMMENT, "updated")));
        assertEquals(original, server.configs.get("test/store"));
        assertEquals(0, server.publications.get());
    }

    private static class Server {
        final    Map<String, String> configs      = new ConcurrentHashMap<>();
        final    AtomicInteger       publications = new AtomicInteger();
        volatile CyclicBarrier       reads;
        volatile boolean             failRead;
        volatile boolean             rejectWrite;
        volatile boolean             failAfterCommit;
        final    ConfigService       client       = (ConfigService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ConfigService.class }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getConfig" -> {
                    if (failRead) {
                        throw new NacosException(500, "unavailable");
                    }
                    String result = configs.get(args[1] + "/" + args[0]);
                    CyclicBarrier barrier = reads;
                    if (barrier != null) {
                        barrier.await(5, TimeUnit.SECONDS);
                    }
                    return result;
                }
                case "publishConfigCas" -> {
                    publications.incrementAndGet();
                    synchronized (configs) {
                        String key = args[1] + "/" + args[0];
                        String old = configs.get(key);
                        String md5 = old == null ? null : HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(old.getBytes(StandardCharsets.UTF_8)));
                        if (rejectWrite || !Objects.equals(md5, args[3])) {
                            return false;
                        }
                        configs.put(key, (String) args[2]);
                        if (failAfterCommit) {
                            throw new NacosException(500, "response lost after commit");
                        }
                        return true;
                    }
                }
                default -> throw new AssertionError("Unexpected client operation: " + method.getName());
            }
        });

        Server() {
            configs.put("test/store", EMPTY);
        }

        NacosDataAccessLayer access() {
            return new NacosDataAccessLayer(client, "store", "test", 3000);
        }
    }

    private Map<FieldDef, String> route(String method, String path) {
        return Map.of(METHOD, method, PATH, path, SCRIPT, "return 1;", COMMENT, "原始说明");
    }

    @Test
    void queriesAndPartialUpdatesPreserveFieldsAndClearExplicitNulls() {
        Server server = new Server();
        var access = server.access();
        access.createObject(EntityType.INFO, "one", route("GET", "/接口"));
        access.createObject(EntityType.INFO, "two", route("POST", "/接口"));
        assertEquals(1, access.listObjects(EntityType.INFO, Map.of(METHOD, "GET", PATH, "/接口")).size());
        assertTrue(access.listObjects(EntityType.INFO, Map.of(METHOD, "get")).isEmpty());
        Map<FieldDef, String> update = new EnumMap<>(FieldDef.class);
        update.put(COMMENT, null);
        access.updateObject(EntityType.INFO, "one", 1, update);
        var row = access.getObject(EntityType.INFO, "one").orElseThrow();
        assertEquals("2", row.get(REVISION));
        assertEquals("return 1;", row.get(SCRIPT));
        assertNull(row.get(COMMENT));
        // An explicit null clears the stored field before the full snapshot is serialized.
        assertFalse(row.containsKey(COMMENT));
        assertFalse(server.configs.get("test/store").contains("\"COMMENT\":null"));
        var cleared = new EnumMap<FieldDef, String>(FieldDef.class);
        cleared.put(COMMENT, null);
        assertEquals(1, access.listObjects(EntityType.INFO, cleared).size());
        row.put(PATH, "/changed");
        assertEquals("/接口", access.getObject(EntityType.INFO, "one").orElseThrow().get(PATH));
        assertThrows(DataConflictException.class, () -> access.deleteObject(EntityType.INFO, "one", 1));
        access.deleteObject(EntityType.INFO, "one", 2);
        assertTrue(access.getObject(EntityType.INFO, "one").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void batchAndRouteConflictsLeaveNoPartialChanges(boolean mapped) {
        Server server = new Server();
        var access = this.access(server, mapped);
        access.createObject(EntityType.INFO, "one", route("GET", "/one"));
        String before = server.configs.get("test/store");
        assertThrows(DataConflictException.class, () -> access.write(List.of(access.create(EntityType.RELEASE, OperationType.CREATE, "release", 0, route("GET", "/one")), access.create(EntityType.INFO, OperationType.UPDATE, "one", 99, Map.of(COMMENT, "stale")))));
        assertEquals(before, server.configs.get("test/store"));
        assertThrows(DataConflictException.class, () -> access.createObject(EntityType.INFO, "duplicate", route("GET", "/one")));
        access.write(List.of(access.create(EntityType.INFO, OperationType.UPDATE, "one", 1, Map.of(COMMENT, "published")), access.create(EntityType.RELEASE, OperationType.CREATE, "r1", 0, route("GET", "/one")), access.create(EntityType.RELEASE, OperationType.CREATE, "r2", 0, route("GET", "/one"))));
        assertEquals(2, server.publications.get());
        assertEquals(2, access.listObjects(EntityType.RELEASE, Map.of()).size());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void separateInstancesCannotOverwriteEachOther(boolean mapped) throws Exception {
        Server server = new Server();
        var first = this.access(server, mapped);
        var second = this.access(server, mapped);
        server.reads = new CyclicBarrier(2);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> one = () -> create(first, "one");
            Callable<Boolean> two = () -> create(second, "two");
            var results = workers.invokeAll(List.of(one, two));
            assertNotEquals(results.get(0).get(), results.get(1).get());
        } finally {
            server.reads = null;
        }
        assertEquals(1, first.listObjects(EntityType.INFO, Map.of()).size());
    }

    private NacosDataAccessLayer access(Server server, boolean mapped) {
        var access = server.access();
        var config = new DatawayConfig().dataAccessLayer(access);
        if (mapped) {
            for (EntityType type : EntityType.values()) {
                config.fieldMapping(type, ID, "record_id").fieldMapping(type, REVISION, "version").fieldMapping(type, SCRIPT, "SCRIPT_ORI");
            }
        }
        config.createDataway();
        return access;
    }

    private boolean create(NacosDataAccessLayer access, String id) {
        try {
            access.createObject(EntityType.INFO, id, route("GET", "/" + id));
            return true;
        } catch (DataConflictException e) {
            return false;
        }
    }

    @Test
    void validatesBeforeRemoteAccessAndRefusesMissingOrCorruptSnapshots() {
        Server server = new Server();
        var access = server.access();
        server.failRead = true;
        assertThrows(IllegalArgumentException.class, () -> access.createObject(EntityType.INFO, "one", Map.of(API_ID, "bad")));
        assertThrows(DataAccessException.class, () -> access.listObjects(EntityType.INFO, Map.of()));
        server.failRead = false;
        for (String bad : List.of("", "{}", "null", "{broken", EMPTY.replace("initial", ""), EMPTY.replace("format\":1", "format\":2"))) {
            server.configs.put("test/store", bad);
            assertThrows(DataAccessException.class, () -> access.createObject(EntityType.INFO, "one", route("GET", "/one")));
        }
        server.configs.remove("test/store");
        assertThrows(DataAccessException.class, () -> access.listObjects(EntityType.INFO, Map.of()));
        assertEquals(0, server.publications.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rejectsCasFailureAndDoesNotRetryAnAmbiguousPublish(boolean mapped) {
        Server server = new Server();
        var access = this.access(server, mapped);
        server.rejectWrite = true;
        assertThrows(DataConflictException.class, () -> access.createObject(EntityType.INFO, "one", route("GET", "/one")));
        assertTrue(access.getObject(EntityType.INFO, "one").isEmpty());
        server.rejectWrite = false;
        server.failAfterCommit = true;
        assertThrows(DataAccessException.class, () -> access.createObject(EntityType.INFO, "one", route("GET", "/one")));
        assertEquals(2, server.publications.get());
        assertTrue(access.getObject(EntityType.INFO, "one").isPresent());
    }

    @Test
    void fullCoreLifecycleWorksWithoutJdbcOrFrameworks() throws Exception {
        Server server = new Server();
        var service = new Dataway(new DatawayConfig().dataAccessLayer(server.access()));
        ApiDefinition oneApi = new ApiDefinition();
        oneApi.setId("one");
        oneApi.setMethod("GET");
        oneApi.setPath("/one");
        oneApi.setType(ApiScriptType.DATA_QL);
        oneApi.setScript("return 42;");
        oneApi.setDescription("说明");
        service.getAdminService().save(oneApi, 0);
        assertNull(service.getAdminService().list().getFirst().getScript());
        assertTrue(service.getAdminService().getApiById("one").isHasDraft());
        service.getAdminService().publish("one", 1);
        var publication = service.getAdminService().getReleaseByApi("one");
        assertEquals(publication, service.getAdminService().getHistoryById(publication.getId()));
        assertEquals(publication, service.getAdminService().getReleaseById(publication.getId()));
        assertFalse(service.getAdminService().getApiById("one").isHasDraft());
        var restarted = new DatawayConfig().dataAccessLayer(server.access()).createDataway();
        var response = new TestWebResponse();
        restarted.getApiHandler().handle(new TestWebRequest("GET", "/one", Map.of()), response);
        Map<?, ?> apiResult = (Map<?, ?>) response.getResult();
        assertEquals(42, ((Number) apiResult.get("value")).intValue());
        assertEquals("return 42;", server.access().listObjects(EntityType.RELEASE, Map.of()).getFirst().get(SCRIPT));
        assertFalse(server.configs.get("test/store").contains("SCRIPT_ORI"));
        var disabled = restarted.getAdminService().disableApi("one", 2);
        assertFalse(disabled.isEnabled());
        assertTrue(disabled.isPublished());
        assertEquals(publication, restarted.getAdminService().getReleaseByApi("one"));
        assertTrue(server.access().listObjects(EntityType.RELEASE, Map.of(STATUS, "1")).isEmpty());
        restarted.getAdminService().deleteApi("one", disabled.getRevision());
        assertTrue(server.access().listObjects(EntityType.INFO, Map.of()).isEmpty());
        assertTrue(server.access().listObjects(EntityType.RELEASE, Map.of()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void importsLegacyDirectoryAndSplitDocumentsWithoutTouchingSource(boolean mapped) {
        Server server = new Server();
        var access = server.access();
        if (mapped) {
            server.configs.put("test/store", "{\"format\":1,\"generation\":\"initial\",\"records\":{\"drafts.v1\":{},\"release history\":{}}}");
            this.mappedConfig(access).createDataway();
        }
        server.configs.put("old/INDEX_MONITOR", "1234");
        server.configs.put("old/INDEX_DIRECTORY_0", "i_one,1234,/one\nr_one,1234,/one\nEND");
        Map<String, Object> info = new LinkedHashMap<>();
        route("GET", "/one").forEach((key, value) -> info.put(key.name(), value));
        info.put("ID", "i_one");
        info.put("API_ID", "i_one");
        info.put("SCRIPT", "compiled draft");
        info.put("SCRIPT_ORI", "original draft");
        info.put("REQ_BODY_SCHEMA", "{\"type\":\"object\"}");
        info.put("REQ_BODY_SAMPLE", "plain body");
        info.put("OPTION", "{\"custom\":true}");
        info.put("PREPARE_HINT", "legacy hint");
        server.configs.put("old/i_one", JsonUtils.writeValueAsString(info));
        server.configs.put("old/r_one", JsonUtils.writeValueAsString(Map.of("ID", "r_one", "API_ID", "i_one", "SCRIPT", "compiled", "SCRIPT_ORI", "original", "CREATE_TIME", "1000", "GMT_TIME", "1001")));
        var original = new HashMap<>(server.configs);
        access.importLegacy("old");
        var row = access.getObject(EntityType.INFO, "i_one").orElseThrow();
        assertEquals("1", row.get(REVISION));
        assertEquals("original draft", row.get(SCRIPT));
        assertFalse(row.containsKey(API_ID));
        assertEquals("plain body", JsonUtils.readValue(row.get(SAMPLE), Map.class).get("requestBody"));
        assertTrue(row.get(SCHEMA).contains("requestBody"));
        assertTrue(row.get(OPTION).contains("legacy hint"));
        assertEquals("original", access.getObject(EntityType.RELEASE, "r_one").orElseThrow().get(SCRIPT));
        assertEquals(mapped, server.configs.get("test/store").contains("SCRIPT_ORI"));
        original.forEach((key, value) -> {
            if (key.startsWith("old/")) {
                assertEquals(value, server.configs.get(key));
            }
        });
        assertEquals(1, server.publications.get());
        assertThrows(DataConflictException.class, () -> access.importLegacy("old"));
    }

    @Test
    void incompleteLegacyImportNeverPublishesPartialData() {
        Server server = new Server();
        server.configs.put("old/INDEX_DIRECTORY_0", "i_missing,1,/missing\nEND");
        assertThrows(DataAccessException.class, () -> server.access().importLegacy("old"));
        assertEquals(EMPTY, server.configs.get("test/store"));
        assertEquals(0, server.publications.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void sqlPublicationStoresOriginalScriptAndCompilesItOnInvocation(boolean mapped) throws Exception {
        Server server = new Server();
        var access = server.access();
        var config = new DatawayConfig().dataAccessLayer(access).resultStructure(false).fragment("sql", () -> (hints, parameters, script) -> {
            assertEquals("SELECT :value + 1", script);
            return ((Number) parameters.get("value")).intValue() + 1;
        });
        if (mapped) {
            config.fieldMapping(EntityType.INFO, SCRIPT, "source_code").fieldMapping(EntityType.RELEASE, SCRIPT, "SCRIPT_ORI");
        }
        var dataway = config.createDataway();
        var definition = new ApiDefinition();
        definition.setId("sql");
        definition.setMethod("GET");
        definition.setPath("/sql");
        definition.setType(ApiScriptType.SQL);
        definition.setScript("SELECT :value + 1");
        definition.setDescription("");
        definition.setSample("{\"requestBody\":{\"value\":0}}");
        dataway.getAdminService().save(definition, 0);
        dataway.getAdminService().publish("sql", 1);
        var release = dataway.getAdminService().getReleaseByApi("sql");
        assertEquals(definition.getScript(), access.getObject(EntityType.INFO, "sql").orElseThrow().get(SCRIPT));
        assertEquals(definition.getScript(), access.getObject(EntityType.RELEASE, release.getId()).orElseThrow().get(SCRIPT));
        assertEquals(mapped, server.configs.get("test/store").contains("SCRIPT_ORI"));
        definition.setScript("SELECT :value + 2");
        dataway.getAdminService().save(definition, 2);
        var restarted = config.createDataway();
        assertEquals("SELECT :value + 1", restarted.getAdminService().getHistoryById(release.getId()).getDefinition().getScript());
        assertEquals("SELECT :value + 2", restarted.getAdminService().getDraftByApi("sql").getScript());
        var response = new TestWebResponse();
        restarted.getApiHandler().handle(new TestWebRequest("GET", "/sql", Map.of("value", 7)), response);
        assertEquals(8, ((Number) response.getResult()).intValue());
    }
}

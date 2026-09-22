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
import com.alibaba.nacos.common.utils.JacksonUtils;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.dal.*;
import net.hasor.dataway.service.model.ApiDefinition;
import net.hasor.dataway.service.model.ScriptType;
import net.hasor.dataway.spi.CallContext;
import org.junit.jupiter.api.Test;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

class NacosDataAccessLayerTest {
    static final String EMPTY = "{\"format\":1,\"generation\":\"initial\",\"records\":{\"INFO\":{},\"RELEASE\":{}}}";

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
        // Nacos omits null fields; replacing the full snapshot must remove the old value.
        assertFalse(row.containsKey(COMMENT));
        assertFalse(server.configs.get("test/store").contains("\"COMMENT\":null"));
        var cleared = new EnumMap<FieldDef, String>(FieldDef.class);
        cleared.put(COMMENT, null);
        assertEquals(1, access.listObjects(EntityType.INFO, cleared).size());
        assertThrows(UnsupportedOperationException.class, () -> row.put(PATH, "/changed"));
        assertThrows(DataConflictException.class, () -> access.deleteObject(EntityType.INFO, "one", 1));
        access.deleteObject(EntityType.INFO, "one", 2);
        assertTrue(access.getObject(EntityType.INFO, "one").isEmpty());
    }

    @Test
    void batchAndRouteConflictsLeaveNoPartialChanges() {
        Server server = new Server();
        var access = server.access();
        access.createObject(EntityType.INFO, "one", route("GET", "/one"));
        String before = server.configs.get("test/store");
        assertThrows(DataConflictException.class, () -> access.write(List.of(access.create(EntityType.RELEASE, OperationType.CREATE, "release", 0, route("GET", "/one")), access.create(EntityType.INFO, OperationType.UPDATE, "one", 99, Map.of(COMMENT, "stale")))));
        assertEquals(before, server.configs.get("test/store"));
        assertThrows(DataConflictException.class, () -> access.createObject(EntityType.INFO, "duplicate", route("GET", "/one")));
        access.write(List.of(access.create(EntityType.INFO, OperationType.UPDATE, "one", 1, Map.of(COMMENT, "published")), access.create(EntityType.RELEASE, OperationType.CREATE, "r1", 0, route("GET", "/one")), access.create(EntityType.RELEASE, OperationType.CREATE, "r2", 0, route("GET", "/one"))));
        assertEquals(2, server.publications.get());
        assertEquals(2, access.listObjects(EntityType.RELEASE, Map.of()).size());
    }

    @Test
    void separateInstancesCannotOverwriteEachOther() throws Exception {
        Server server = new Server();
        var first = server.access();
        var second = server.access();
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

    @Test
    void rejectsCasFailureAndDoesNotRetryAnAmbiguousPublish() {
        Server server = new Server();
        var access = server.access();
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
    void fullServiceLifecycleWorksWithoutJdbcOrFrameworks() throws Exception {
        Server server = new Server();
        var service = Dataway.builder().dataAccessLayer(server.access()).build().getService();
        service.save(new ApiDefinition("one", "GET", "/one", ScriptType.DATAQL, "return 42;", "说明"), 0, CallContext.LOCAL);
        service.publish("one", 1, CallContext.LOCAL);
        var restarted = Dataway.builder().dataAccessLayer(server.access()).build().getService();
        assertEquals(42, ((Number) restarted.invokeApi("/one", Map.of())).intValue());
        assertEquals("return 42;", server.access().listObjects(EntityType.RELEASE, Map.of()).getFirst().get(SCRIPT_ORI));
        var disabled = restarted.disableApi("one", 2, CallContext.LOCAL);
        assertFalse(disabled.enabled());
        assertThrows(net.hasor.dataway.spi.DatawayException.class, () -> restarted.invokeApi("/one", Map.of()));
        restarted.deleteApi("one", disabled.revision(), CallContext.LOCAL);
        assertTrue(server.access().listObjects(EntityType.INFO, Map.of()).isEmpty());
        assertTrue(server.access().listObjects(EntityType.RELEASE, Map.of()).isEmpty());
    }

    @Test
    void importsLegacyDirectoryAndSplitDocumentsWithoutTouchingSource() {
        Server server = new Server();
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
        server.configs.put("old/i_one", JacksonUtils.toJson(info));
        server.configs.put("old/r_one", JacksonUtils.toJson(Map.of("ID", "r_one", "API_ID", "i_one", "SCRIPT", "compiled", "SCRIPT_ORI", "original", "CREATE_TIME", "1000", "GMT_TIME", "1001")));
        var original = new HashMap<>(server.configs);
        server.access().importLegacy("old");
        var row = server.access().getObject(EntityType.INFO, "i_one").orElseThrow();
        assertEquals("1", row.get(REVISION));
        assertEquals("original draft", row.get(SCRIPT));
        assertFalse(row.containsKey(API_ID));
        assertEquals("plain body", JacksonUtils.toObj(row.get(SAMPLE), Map.class).get("requestBody"));
        assertTrue(row.get(SCHEMA).contains("requestBody"));
        assertTrue(row.get(OPTION).contains("legacy hint"));
        assertEquals("original", server.access().getObject(EntityType.RELEASE, "r_one").orElseThrow().get(SCRIPT_ORI));
        original.forEach((key, value) -> {
            if (key.startsWith("old/")) {
                assertEquals(value, server.configs.get(key));
            }
        });
        assertEquals(1, server.publications.get());
        assertThrows(DataConflictException.class, () -> server.access().importLegacy("old"));
    }

    @Test
    void incompleteLegacyImportNeverPublishesPartialData() {
        Server server = new Server();
        server.configs.put("old/INDEX_DIRECTORY_0", "i_missing,1,/missing\nEND");
        assertThrows(DataAccessException.class, () -> server.access().importLegacy("old"));
        assertEquals(EMPTY, server.configs.get("test/store"));
        assertEquals(0, server.publications.get());
    }
}

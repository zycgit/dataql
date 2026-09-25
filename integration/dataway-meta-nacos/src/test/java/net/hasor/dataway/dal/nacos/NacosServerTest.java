/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.OperationType;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.CallContext;
import org.junit.jupiter.api.Test;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

class NacosServerTest {
    private ConfigService client() throws Exception {
        String address = System.getenv("DATAWAY_NACOS_SERVER");
        assertNotNull(address, "Set DATAWAY_NACOS_SERVER to an isolated Nacos server");
        Properties properties = new Properties();
        properties.setProperty("serverAddr", address);
        return NacosFactory.createConfigService(properties);
    }

    @Test
    void publishInvokeReconnectAndVersionChecks() throws Exception {
        String dataId = "dataway-test-" + UUID.randomUUID();
        String group = "DATAWAY_TEST";
        ConfigService client = client();
        try {
            assertTrue(client.publishConfig(dataId, group, NacosDataAccessLayerTest.EMPTY));
            var access = new NacosDataAccessLayer(client, dataId, group, 5000);
            var service = Dataway.builder().dataAccessLayer(access).build().getService();
            ApiDefinition oneApi = new ApiDefinition();
            oneApi.setId("one");
            oneApi.setMethod("GET");
            oneApi.setPath("/one");
            oneApi.setType(ApiScriptType.DATAQL);
            oneApi.setScript("return 42;");
            oneApi.setDescription("说明");
            service.save(oneApi, 0, CallContext.local(Operation.SAVE));
            service.publish("one", 1, CallContext.local(Operation.PUBLISH));
            ConfigService other = client();
            try {
                var restarted = new NacosDataAccessLayer(other, dataId, group, 5000);
                assertEquals(42, ((Number) Dataway.builder().dataAccessLayer(restarted).build().getService().invokeApi("/one", Map.of())).intValue());
                String before = other.getConfig(dataId, group, 5000);
                assertThrows(DataConflictException.class, () -> restarted.write(List.of(restarted.create(EntityType.RELEASE, OperationType.CREATE, "extra", 0, Map.of(SCRIPT, "return 9;")), restarted.create(EntityType.INFO, OperationType.UPDATE, "one", 999, Map.of(COMMENT, "stale")))));
                assertEquals(before, other.getConfig(dataId, group, 5000));
            } finally {
                other.shutDown();
            }
        } finally {
            try {
                client.removeConfig(dataId, group);
            } finally {
                client.shutDown();
            }
        }
    }

    @Test
    void realServerCasRejectsOneOfTwoCompetingSnapshots() throws Exception {
        String dataId = "dataway-cas-" + UUID.randomUUID();
        String group = "DATAWAY_TEST";
        ConfigService first = client();
        ConfigService second = client();
        try {
            assertTrue(first.publishConfig(dataId, group, NacosDataAccessLayerTest.EMPTY));
            CyclicBarrier barrier = new CyclicBarrier(2);
            var a = new NacosDataAccessLayer(synchronizeRead(first, barrier), dataId, group, 5000);
            var b = new NacosDataAccessLayer(synchronizeRead(second, barrier), dataId, group, 5000);
            try (var workers = Executors.newFixedThreadPool(2)) {
                var results = workers.invokeAll(List.of(write(a, "one"), write(b, "two")));
                assertNotEquals(results.get(0).get(), results.get(1).get());
            }
            assertEquals(1, new NacosDataAccessLayer(first, dataId, group, 5000).listObjects(EntityType.INFO, Map.of()).size());
        } finally {
            try {
                first.removeConfig(dataId, group);
            } finally {
                first.shutDown();
                second.shutDown();
            }
        }
    }

    private Callable<Boolean> write(NacosDataAccessLayer access, String id) {
        return () -> {
            try {
                access.createObject(EntityType.INFO, id, Map.of(METHOD, "GET", PATH, "/" + id));
                return true;
            } catch (DataConflictException e) {
                return false;
            }
        };
    }

    private ConfigService synchronizeRead(ConfigService target, CyclicBarrier barrier) {
        return (ConfigService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ConfigService.class }, (proxy, method, args) -> {
            try {
                Object result = method.invoke(target, args);
                if (method.getName().equals("getConfig")) {
                    barrier.await(10, TimeUnit.SECONDS);
                }
                return result;
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}

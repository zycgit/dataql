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
import net.hasor.dataway.TestWebRequest;
import net.hasor.dataway.TestWebResponse;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.OperationType;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DATAWAY_NACOS_SERVER", matches = ".*\\S.*")
class NacosServerTest {
    private ConfigService client() throws Exception {
        String address = System.getenv("DATAWAY_NACOS_SERVER");
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
            var service = new Dataway(new DatawayConfig().dataAccessLayer(access));
            ApiDefinition oneApi = new ApiDefinition();
            oneApi.setId("one");
            oneApi.setMethod("GET");
            oneApi.setPath("/one");
            oneApi.setType(ApiScriptType.DATA_QL);
            oneApi.setScript("return 42;");
            oneApi.setDescription("说明");
            service.getAdminService().save(oneApi, 0);
            service.getAdminService().publish("one", 1);
            ConfigService other = client();
            try {
                var restarted = new NacosDataAccessLayer(other, dataId, group, 5000);
                var dataway = new DatawayConfig().dataAccessLayer(restarted).createDataway();
                var response = new TestWebResponse();
                dataway.getApiHandler().handle(new TestWebRequest("GET", "/one", Map.of()), response);
                Map<?, ?> apiResult = (Map<?, ?>) response.getResult();
                assertEquals(42, ((Number) apiResult.get("value")).intValue());
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
            var workers = Executors.newFixedThreadPool(2);
            try {
                var results = workers.invokeAll(List.of(write(a, "one"), write(b, "two")));
                assertNotEquals(results.get(0).get(), results.get(1).get());
            } finally {
                workers.shutdownNow();
                assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
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

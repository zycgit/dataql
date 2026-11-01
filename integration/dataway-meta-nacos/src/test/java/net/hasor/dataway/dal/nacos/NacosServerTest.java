/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.OperationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

/** Optional transport verification; the unit suite needs no Nacos server. */
@EnabledIfEnvironmentVariable(named = "DATAWAY_NACOS_SERVER", matches = ".*\\S.*")
class NacosServerTest {
    @Test
    void independentClientsReloadCommittedBatchesAndServerCasRejectsStaleDigests() throws Exception {
        // 两个独立客户端共享本场景专用配置，Nacos 持久确认与查询缓存可见分开等待。
        Properties properties = new Properties();
        properties.setProperty("serverAddr", System.getenv("DATAWAY_NACOS_SERVER"));
        String dataId = "dataway-test-" + UUID.randomUUID();
        String group = "DATAWAY_TEST";
        ConfigService first = NacosFactory.createConfigService(properties);
        try {
            assertTrue(first.publishConfig(dataId, group, NacosSnapshot.empty().serialize()));
            awaitSnapshot(first, dataId, group, snapshot -> "initial".equals(snapshot.getGeneration()));
            var writer = new NacosDataAccessLayer(first, dataId, group, 5000);
            writer.createObject(EntityType.INFO, "draft", NacosFixture.route("GET", "/draft"));
            awaitSnapshot(first, dataId, group, snapshot -> hasRevision(snapshot, "1"));

            // 新客户端观察到首版记录后，再执行原子批次并等待该批次整体可见。
            ConfigService second = NacosFactory.createConfigService(properties);
            try {
                var reader = new NacosDataAccessLayer(second, dataId, group, 5000);
                String before = awaitSnapshot(second, dataId, group, snapshot -> hasRevision(snapshot, "1"));
                assertEquals("1", reader.getObject(EntityType.INFO, "draft").orElseThrow().get(REVISION));
                var createRelease = writer.create(EntityType.RELEASE, OperationType.CREATE, "release", 0, Map.of(API_ID, "draft", SCRIPT, "return 1;"));
                var updateDraft = writer.create(EntityType.INFO, OperationType.UPDATE, "draft", 1, Map.of(STATUS, "1"));
                writer.write(List.of(createRelease, updateDraft));
                awaitSnapshot(second, dataId, group, snapshot -> hasRevision(snapshot, "2") && snapshot.getRecords().get(EntityType.RELEASE).containsKey("release"));
                assertTrue(reader.getObject(EntityType.RELEASE, "release").isPresent());
                assertEquals("2", reader.getObject(EntityType.INFO, "draft").orElseThrow().get(REVISION));

                // 陈旧业务版本和陈旧正文摘要分别被拒绝，不重试写入或 CAS。
                assertThrows(DataConflictException.class, () -> reader.updateObject(EntityType.INFO, "draft", 1, Map.of(COMMENT, "stale")));
                assertFalse(second.publishConfigCas(dataId, group, before, NacosFixture.md5(before)));
                assertEquals("2", reader.getObject(EntityType.INFO, "draft").orElseThrow().get(REVISION));
            } finally {
                second.shutDown();
            }
        } finally {
            try {
                first.removeConfig(dataId, group);
            } finally {
                first.shutDown();
            }
        }
    }

    private static boolean hasRevision(NacosSnapshot snapshot, String revision) {
        Map<FieldDef, String> row = snapshot.getRecords().get(EntityType.INFO).get("draft");
        return row != null && revision.equals(row.get(REVISION));
    }

    private static String awaitSnapshot(ConfigService client, String dataId, String group, Predicate<NacosSnapshot> expected) throws Exception {
        // 仅轮询正式 SDK 的查询结果；服务端错误和非法正文立即向测试报告传播。
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        String content = null;
        while (System.nanoTime() < deadline) {
            content = client.getConfig(dataId, group, 5000);
            if (content != null && expected.test(NacosSnapshot.parse(content))) {
                return content;
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        return fail("Nacos snapshot did not become visible: " + dataId + "; last content: " + content);
    }
}

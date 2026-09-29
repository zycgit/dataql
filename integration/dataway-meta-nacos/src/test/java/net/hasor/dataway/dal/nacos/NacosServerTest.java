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
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
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
        Properties properties = new Properties();
        properties.setProperty("serverAddr", System.getenv("DATAWAY_NACOS_SERVER"));
        String dataId = "dataway-test-" + UUID.randomUUID();
        String group = "DATAWAY_TEST";
        ConfigService first = NacosFactory.createConfigService(properties);
        try {
            assertTrue(first.publishConfig(dataId, group, NacosSnapshot.empty().serialize()));
            var writer = new NacosDataAccessLayer(first, dataId, group, 5000);
            writer.createObject(EntityType.INFO, "draft", NacosFixture.route("GET", "/draft"));
            ConfigService second = NacosFactory.createConfigService(properties);
            try {
                var reader = new NacosDataAccessLayer(second, dataId, group, 5000);
                assertEquals("1", reader.getObject(EntityType.INFO, "draft").orElseThrow().get(REVISION));
                String before = second.getConfig(dataId, group, 5000);
                writer.write(List.of(writer.create(EntityType.RELEASE, OperationType.CREATE, "release", 0, Map.of(API_ID, "draft", SCRIPT, "return 1;")), writer.create(EntityType.INFO, OperationType.UPDATE, "draft", 1, Map.of(STATUS, "1"))));
                assertTrue(reader.getObject(EntityType.RELEASE, "release").isPresent());
                assertEquals("2", reader.getObject(EntityType.INFO, "draft").orElseThrow().get(REVISION));
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
}

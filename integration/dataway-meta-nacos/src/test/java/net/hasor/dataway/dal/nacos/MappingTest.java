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
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.OperationType;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.api.Test;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

class MappingTest {
    @Test
    void arbitraryStorageNamesSupportPublicationHistoryAndReconnect() throws Exception {
        NacosFixture fixture = new NacosFixture();
        fixture.content("{\"format\":1,\"generation\":\"initial\",\"records\":{\"drafts.v1\":{},\"release history\":{}}}");
        var config = this.config(fixture.access);
        var admin = config.createDataway().getAdminService();
        ApiDefinition definition = new ApiDefinition();
        definition.setId("mapped");
        definition.setMethod("GET");
        definition.setPath("/mapped");
        definition.setType(ApiScriptType.SQL);
        definition.setScript("select :id");
        definition.setDescription("Mapped metadata");
        assertEquals(1, admin.save(definition, 0).getRevision());
        admin.publish("mapped", 1);
        String firstID = admin.getReleaseByApi("mapped").getId();
        definition.setScript("select :id + 1");
        admin.save(definition, 2);
        admin.publish("mapped", 3);
        var fresh = this.config(fixture.reconnect()).createDataway().getAdminService();
        assertEquals(2, fresh.getHistoryByApi("mapped").size());
        assertEquals("select :id", fresh.getReleaseById(firstID).getDefinition().getScript());
        assertEquals("select :id + 1", fresh.getDraftByApi("mapped").getScript());
        var records = JsonUtils.readTree(fixture.content()).path("records");
        assertFalse(records.has("INFO"));
        assertEquals("select :id + 1", records.path("drafts.v1").path("mapped").path("stored_SCRIPT").asText());
        config.tableMapping(EntityType.INFO, "changed after creation");
        assertEquals(1, admin.list().size());
        fresh.disableApi("mapped", 4);
        assertFalse(admin.getApiById("mapped").isEnabled());
        fresh.deleteApi("mapped", 5);
        assertTrue(admin.list().isEmpty());
        assertTrue(fixture.access.listObjects(EntityType.RELEASE, Map.of()).isEmpty());
    }

    private DatawayConfig config(NacosDataAccessLayer access) {
        var config = new DatawayConfig().dataAccessLayer(access).tableMapping(EntityType.INFO, "drafts.v1").tableMapping(EntityType.RELEASE, "release history");
        for (EntityType type : EntityType.values()) {
            for (FieldDef field : FieldDef.values()) {
                if ((type == EntityType.INFO && (field == API_ID || field == RELEASE_TIME)) || (type == EntityType.RELEASE && (field == CREATE_TIME || field == GMT_TIME))) {
                    continue;
                }
                config.fieldMapping(type, field, "stored_" + field.name());
            }
        }
        return config;
    }

    @Test
    void originalScriptMappingPreservesTheUnusedLegacyScriptUntilRecordReplacement() throws Exception {
        NacosFixture fixture = new NacosFixture();
        fixture.content("{\"format\":1,\"generation\":\"old\",\"records\":{\"INFO\":{},\"RELEASE\":{\"sql\":{\"ID\":\"sql\",\"REVISION\":\"1\",\"SCRIPT\":\"compiled\",\"SCRIPT_ORI\":\"select :id\"}}}}");
        var access = fixture.access;
        access.configureMapping(Map.of(), Map.of(EntityType.RELEASE, Map.of(SCRIPT, "SCRIPT_ORI")));
        assertEquals("select :id", access.getObject(EntityType.RELEASE, "sql").orElseThrow().get(SCRIPT));
        access.updateObject(EntityType.RELEASE, "sql", 1, Map.of(SCRIPT, "select :value"));
        var row = JsonUtils.readTree(fixture.content()).path("records").path("RELEASE").path("sql");
        assertEquals("compiled", row.path("SCRIPT").asText());
        assertEquals("select :value", row.path("SCRIPT_ORI").asText());
        Map<FieldDef, String> clear = new EnumMap<>(FieldDef.class);
        clear.put(SCRIPT, null);
        access.updateObject(EntityType.RELEASE, "sql", 2, clear);
        row = JsonUtils.readTree(fixture.content()).path("records").path("RELEASE").path("sql");
        assertTrue(row.has("SCRIPT"));
        assertFalse(row.has("SCRIPT_ORI"));
        access.write(List.of(access.create(EntityType.RELEASE, OperationType.DELETE, "sql", 3, Map.of()), access.create(EntityType.RELEASE, OperationType.CREATE, "sql", 0, Map.of(SCRIPT, "select 1"))));
        row = JsonUtils.readTree(fixture.content()).path("records").path("RELEASE").path("sql");
        assertFalse(row.has("SCRIPT"));
        assertEquals("select 1", row.path("SCRIPT_ORI").asText());
    }

    @Test
    void ambiguousAndUnsupportedMappingsDoNotReplaceThePreviousMapping() throws Exception {
        NacosFixture fixture = new NacosFixture();
        assertThrows(IllegalArgumentException.class, () -> fixture.access.configureMapping(Map.of(EntityType.INFO, "RELEASE"), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> fixture.access.configureMapping(Map.of(), Map.of(EntityType.INFO, Map.of(SCRIPT, "ID"))));
        assertThrows(IllegalArgumentException.class, () -> fixture.access.configureMapping(Map.of(), Map.of(EntityType.INFO, Map.of(API_ID, "foreign"))));
        fixture.access.createObject(EntityType.INFO, "good", NacosFixture.route("GET", "/good"));
        assertTrue(fixture.access.getObject(EntityType.INFO, "good").isPresent());
    }
}

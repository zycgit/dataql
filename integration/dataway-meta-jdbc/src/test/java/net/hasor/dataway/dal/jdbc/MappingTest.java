/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.util.EnumMap;
import java.util.Map;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

class MappingTest {
    @Test
    void configuredTablesAndAllFieldsSupportTheCompleteAdminLifecycle() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            fixture.load("/mapped-h2.sql");
            Map<FieldDef, String> info = this.fields(new FieldDef[] { ID, METHOD, PATH, STATUS, COMMENT, TYPE, SCRIPT, SCHEMA, SAMPLE, OPTION, CREATE_TIME, GMT_TIME, REVISION }, "definition_id request_method route_path lifecycle_status description script_type source_text schema_document sample_document option_document created_at updated_at row_version");
            Map<FieldDef, String> release = this.fields(new FieldDef[] { ID, API_ID, METHOD, PATH, STATUS, COMMENT, TYPE, SCRIPT, SCHEMA, SAMPLE, OPTION, RELEASE_TIME, REVISION }, "release_id definition_ref http_method http_path release_status release_description release_type original_script release_schema release_sample release_options published_at release_version");
            var config = new DatawayConfig().dataAccessLayer(fixture.access).tableMapping(EntityType.INFO, "metadata.drafts").tableMapping(EntityType.RELEASE, "metadata.releases");
            info.forEach((field, name) -> config.fieldMapping(EntityType.INFO, field, name));
            release.forEach((field, name) -> config.fieldMapping(EntityType.RELEASE, field, name));
            var admin = config.createDataway().getAdminService();
            ApiDefinition definition = new ApiDefinition();
            definition.setId("mapped");
            definition.setMethod("GET");
            definition.setPath("/mapped");
            definition.setType(ApiScriptType.SQL);
            definition.setDescription("Mapped SQL API");
            definition.setScript("select :value");
            assertEquals(1, admin.save(definition, 0).getRevision());
            assertTrue(admin.publish("mapped", 1).isEnabled());
            String releaseID = admin.getReleaseByApi("mapped").getId();
            definition.setScript("select :value + 1");
            admin.save(definition, 2);
            assertEquals("select :value", admin.getReleaseById(releaseID).getDefinition().getScript());
            assertTrue(admin.getApiById("mapped").isHasDraft());
            admin.publish("mapped", 3);
            assertEquals(2, admin.getHistoryByApi("mapped").size());
            admin.disableApi("mapped", 4);
            assertFalse(admin.getApiById("mapped").isEnabled());
            admin.deleteApi("mapped", 5);
            assertTrue(admin.list().isEmpty());
            assertTrue(fixture.access.listObjects(EntityType.RELEASE, Map.of()).isEmpty());
        }
    }

    private Map<FieldDef, String> fields(FieldDef[] fields, String names) {
        String[] columns = names.split(" ");
        Map<FieldDef, String> result = new EnumMap<>(FieldDef.class);
        for (int index = 0; index < fields.length; index++) {
            result.put(fields[index], columns[index]);
        }
        return result;
    }

    @Test
    void originalScriptColumnMappingLeavesLegacyCompiledColumnUntouched() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            fixture.sql("ALTER TABLE interface_info ADD api_script_ori CLOB");
            fixture.sql("ALTER TABLE interface_info ALTER COLUMN api_script SET DEFAULT 'legacy compiled code'");
            fixture.access.configureMapping(Map.of(), Map.of(EntityType.INFO, Map.of(SCRIPT, "api_script_ori")));
            fixture.access.createObject(EntityType.INFO, "sql", JdbcFixture.info("POST", "/sql"));
            fixture.access.updateObject(EntityType.INFO, "sql", 1, Map.of(SCRIPT, "select :id"));
            assertEquals("select :id", fixture.access.getObject(EntityType.INFO, "sql").orElseThrow().get(SCRIPT));
            try (var connection = fixture.source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT api_script, api_script_ori FROM interface_info")) {
                assertTrue(rows.next());
                assertEquals("legacy compiled code", rows.getString(1));
                assertEquals("select :id", rows.getString(2));
            }
        }
    }

    @Test
    void catalogSchemaPrefixAndDefensiveMappingCopiesAreSupported() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            fixture.sql("ALTER TABLE interface_info RENAME TO dw_interface_info");
            var prefixed = new JdbcDataAccessLayer(fixture.source, "dw_");
            prefixed.createObject(EntityType.INFO, "prefix", JdbcFixture.info("GET", "/prefix"));
            String catalog;
            try (var connection = fixture.source.getConnection()) {
                catalog = connection.getCatalog();
            }
            Map<EntityType, String> tables = new EnumMap<>(EntityType.class);
            tables.put(EntityType.INFO, catalog + ".PUBLIC.dw_interface_info");
            prefixed.configureMapping(tables, Map.of());
            tables.put(EntityType.INFO, "missing");
            assertTrue(prefixed.getObject(EntityType.INFO, "prefix").isPresent());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "bad-name", "schema.table.extra.part", "table;DROP TABLE x" })
    void invalidNamesFailBeforeDatabaseAccess(String name) throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            Map<EntityType, String> table = new EnumMap<>(EntityType.class);
            table.put(EntityType.INFO, name);
            assertThrows(IllegalArgumentException.class, () -> fixture.access.configureMapping(table, Map.of()));
            Map<FieldDef, String> column = new EnumMap<>(FieldDef.class);
            column.put(SCRIPT, name);
            assertThrows(IllegalArgumentException.class, () -> fixture.access.configureMapping(Map.of(), Map.of(EntityType.INFO, column)));
            assertTrue(fixture.access.listObjects(EntityType.INFO, Map.of()).isEmpty());
        }
    }

    @Test
    void duplicateUnsupportedColumnsAndInvalidPrefixesAreRejected() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            assertThrows(IllegalArgumentException.class, () -> fixture.access.configureMapping(Map.of(), Map.of(EntityType.INFO, Map.of(SCRIPT, "API_ID"))));
            assertThrows(IllegalArgumentException.class, () -> fixture.access.configureMapping(Map.of(), Map.of(EntityType.INFO, Map.of(API_ID, "source"))));
            assertThrows(IllegalArgumentException.class, () -> new JdbcDataAccessLayer(fixture.source, "bad-prefix"));
            assertThrows(IllegalArgumentException.class, () -> new JdbcDataAccessLayer(fixture.source, null));
        }
    }
}

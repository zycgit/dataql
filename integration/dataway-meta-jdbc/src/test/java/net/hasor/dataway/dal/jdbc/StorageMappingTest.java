/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.hasor.dataway.TestDatabase;
import net.hasor.dataway.TestWebRequest;
import net.hasor.dataway.TestWebResponse;
import net.hasor.dataway.dal.DataAccessException;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.DatawayException;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.EntityType.INFO;
import static net.hasor.dataway.dal.EntityType.RELEASE;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;

class StorageMappingTest {
    @Test
    void allMappedTablesAndFieldsSupportPublicationExecutionAndVersionChecks() throws Exception {
        var source = TestDatabase.empty();
        try (var connection = source.getConnection(); var stream = this.getClass().getResourceAsStream("/mapped-h2.sql"); var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
        var access = new JdbcDataAccessLayer(source, "unused_");
        var config = this.mappedConfig(access);
        var dataway = config.createDataway();
        var admin = dataway.getAdminService();
        var definition = this.definition("return 'first';");
        admin.save(definition, 0);
        admin.publish("one", 1);
        String firstRelease = admin.getReleaseByApi("one").getId();
        definition.setScript("return 'second';");
        admin.save(definition, 2);
        assertEquals(409, assertThrows(DatawayException.class, () -> admin.publish("one", 2)).status());
        admin.publish("one", 3);
        assertEquals(4, admin.getVersionById("one"));
        assertEquals(2, admin.getHistoryByApi("one").size());
        assertEquals("return 'first';", admin.getHistoryById(firstRelease).getDefinition().getScript());
        assertEquals("return 'second';", admin.getDraftByApi("one").getScript());
        assertEquals("{\"requestBody\":{\"value\":0}}", admin.getReleaseByApi("one").getDefinition().getSample());
        assertEquals("{\"type\":\"object\"}", admin.getReleaseByApi("one").getDefinition().getSchema());
        assertEquals("{\"resultStructure\":false}", admin.getReleaseByApi("one").getDefinition().getOptions());
        assertEquals(1, access.listObjects(INFO, Map.of(METHOD, "GET", PATH, "/mapped")).size());
        assertEquals(1, access.listObjects(RELEASE, Map.of(API_ID, "one", STATUS, "1")).size());
        assertThrows(DataConflictException.class, () -> access.deleteObject(RELEASE, firstRelease, 1));
        access.deleteObject(RELEASE, firstRelease, 2);
        assertTrue(access.getObject(RELEASE, firstRelease).isEmpty());

        // Later edits to the configuration do not change an initialized access layer.
        config.tableMapping(INFO, "missing_table").fieldMapping(RELEASE, SCRIPT, "missing_column");
        var response = new TestWebResponse();
        dataway.getApiHandler().handle(new TestWebRequest("GET", "/mapped", Map.of()), response);
        assertEquals("second", response.getResult());
        admin.disableApi("one", 4);
        assertFalse(admin.getApiById("one").isEnabled());
        assertThrows(DataConflictException.class, () -> access.deleteObject(INFO, "one", 4));
        admin.deleteApi("one", 5);
        assertTrue(admin.list().isEmpty());
    }

    private DatawayConfig mappedConfig(JdbcDataAccessLayer access) {
        return new DatawayConfig().dataAccessLayer(access).resultStructure(false).tableMapping(INFO, "metadata.drafts").fieldMapping(INFO, ID, "definition_id").fieldMapping(INFO, METHOD, "request_method").fieldMapping(INFO, PATH, "route_path").fieldMapping(INFO, STATUS, "lifecycle_status").fieldMapping(INFO, COMMENT, "description").fieldMapping(INFO, TYPE, "script_type").fieldMapping(INFO, SCRIPT, "source_text").fieldMapping(INFO, SCHEMA, "schema_document").fieldMapping(INFO, SAMPLE, "sample_document").fieldMapping(INFO, OPTION, "option_document").fieldMapping(INFO, CREATE_TIME, "created_at")
                .fieldMapping(INFO, GMT_TIME, "updated_at").fieldMapping(INFO, REVISION, "row_version").tableMapping(RELEASE, "metadata.releases").fieldMapping(RELEASE, ID, "release_id").fieldMapping(RELEASE, API_ID, "definition_ref").fieldMapping(RELEASE, METHOD, "http_method").fieldMapping(RELEASE, PATH, "http_path").fieldMapping(RELEASE, STATUS, "release_status").fieldMapping(RELEASE, COMMENT, "release_description").fieldMapping(RELEASE, TYPE, "release_type").fieldMapping(RELEASE, SCRIPT, "original_script").fieldMapping(RELEASE, SCHEMA, "release_schema")
                .fieldMapping(RELEASE, SAMPLE, "release_sample").fieldMapping(RELEASE, OPTION, "release_options").fieldMapping(RELEASE, RELEASE_TIME, "published_at").fieldMapping(RELEASE, REVISION, "release_version");
    }

    private ApiDefinition definition(String script) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId("one");
        definition.setMethod("GET");
        definition.setPath("/mapped");
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript(script);
        definition.setDescription("Mapping example");
        definition.setSchema("{\"type\":\"object\"}");
        definition.setSample("{\"requestBody\":{\"value\":0}}");
        definition.setOptions("{\"resultStructure\":false}");
        return definition;
    }

    @Test
    void originalLegacySqlUsesTheMappedColumnWithoutChangingTheExecutionColumn() throws Exception {
        var source = TestDatabase.create();
        var original = new DatawayConfig().dataAccessLayer(new JdbcDataAccessLayer(source, "")).createDataway().getAdminService();
        var definition = this.definition("SELECT :value");
        definition.setType(ApiScriptType.SQL);
        original.save(definition, 0);
        original.publish("one", 1);
        String releaseID = original.getReleaseByApi("one").getId();
        String oldExecution = "var tempCall = @@sql(`value`)<%SELECT :value%>; return tempCall(${value});";
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE interface_release RENAME COLUMN pub_script TO pub_script_ori");
            statement.execute("ALTER TABLE interface_release ADD COLUMN pub_script CLOB");
            try (var update = connection.prepareStatement("UPDATE interface_release SET pub_script = ?")) {
                update.setString(1, oldExecution);
                update.executeUpdate();
            }
            statement.execute("ALTER TABLE interface_release ALTER COLUMN pub_script SET NOT NULL");
        }

        var access = new JdbcDataAccessLayer(source, "");
        var dataway = new DatawayConfig().dataAccessLayer(access).resultStructure(false).fieldMapping(RELEASE, SCRIPT, "pub_script_ori").fragment("sql", () -> (hints, parameters, script) -> {
            assertEquals("SELECT :value", script);
            return parameters.get("value");
        }).createDataway();
        var admin = dataway.getAdminService();
        assertEquals("SELECT :value", admin.getHistoryById(releaseID).getDefinition().getScript());
        var response = new TestWebResponse();
        dataway.getApiHandler().handle(new TestWebRequest("GET", "/mapped", Map.of("value", 7)), response);
        assertEquals(7, response.getResult());
        assertThrows(DataAccessException.class, () -> admin.publish("one", 2));
        assertEquals(2, admin.getVersionById("one"));

        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE interface_release ALTER COLUMN pub_script DROP NOT NULL");
        }
        admin.publish("one", 2);
        assertEquals(2, admin.getHistoryByApi("one").size());
        assertEquals("SELECT :value", access.getObject(RELEASE, admin.getReleaseByApi("one").getId()).orElseThrow().get(SCRIPT));
        try (var connection = source.getConnection(); var statement = connection.prepareStatement("SELECT pub_script, pub_script_ori FROM interface_release WHERE pub_id = ?")) {
            statement.setString(1, releaseID);
            try (var row = statement.executeQuery()) {
                assertTrue(row.next());
                assertEquals(oldExecution, row.getString("pub_script"));
                assertEquals("SELECT :value", row.getString("pub_script_ori"));
            }
        }
    }

    @Test
    void partialOverridesKeepTheOtherEntityPrefixAndFieldDefaults() throws Exception {
        var source = TestDatabase.create();
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE interface_info RENAME TO application_apis");
            statement.execute("ALTER TABLE application_apis RENAME COLUMN api_script TO source_code");
            statement.execute("ALTER TABLE interface_release RENAME TO tenant_interface_release");
        }
        var dataway = new DatawayConfig().dataAccessLayer(new JdbcDataAccessLayer(source, "tenant_")).tableMapping(INFO, "application_apis").fieldMapping(INFO, SCRIPT, "source_code").createDataway();
        var admin = dataway.getAdminService();
        admin.save(this.definition("return true;"), 0);
        admin.publish("one", 1);
        assertEquals("return true;", admin.getDraftByApi("one").getScript());
        assertEquals("return true;", admin.getReleaseByApi("one").getDefinition().getScript());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "table; DROP TABLE other", "schema..table", "table -- comment" })
    void invalidTableNamesFailAtInitialization(String name) {
        var config = new DatawayConfig().dataAccessLayer(new JdbcDataAccessLayer(TestDatabase.empty(), "")).tableMapping(INFO, name);
        assertThrows(IllegalArgumentException.class, config::createDataway);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "script; DROP TABLE other", "table.column", "source,api_id" })
    void invalidColumnNamesFailAtInitialization(String name) {
        var config = new DatawayConfig().dataAccessLayer(new JdbcDataAccessLayer(TestDatabase.empty(), "")).fieldMapping(RELEASE, SCRIPT, name);
        assertThrows(IllegalArgumentException.class, config::createDataway);
    }

    @ParameterizedTest
    @CsvSource({ "INFO,API_ID", "INFO,RELEASE_TIME", "RELEASE,CREATE_TIME", "RELEASE,GMT_TIME" })
    void fieldsMustBelongToTheMappedEntity(EntityType entityType, FieldDef field) {
        var config = new DatawayConfig().dataAccessLayer(new JdbcDataAccessLayer(TestDatabase.empty(), "")).fieldMapping(entityType, field, "custom_field");
        assertThrows(IllegalArgumentException.class, config::createDataway);
    }

    @Test
    void duplicateColumnNamesFailWithoutReplacingExistingMappings() {
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        var dataway = new DatawayConfig().dataAccessLayer(access).createDataway();
        dataway.getAdminService().save(this.definition("return true;"), 0);
        var invalid = new DatawayConfig().dataAccessLayer(access).fieldMapping(RELEASE, SCRIPT, "PUB_ID");
        assertThrows(IllegalArgumentException.class, invalid::createDataway);
        dataway.getAdminService().publish("one", 1);
        assertEquals("return true;", dataway.getAdminService().getReleaseByApi("one").getDefinition().getScript());
    }
}

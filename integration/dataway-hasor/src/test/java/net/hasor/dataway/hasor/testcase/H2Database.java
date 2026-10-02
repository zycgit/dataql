/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;

/** Isolated, explicitly initialized metadata storage for each test. */
public final class H2Database implements AutoCloseable {
    public final JdbcDataSource      source = new JdbcDataSource();
    public final JdbcDataAccessLayer access;

    public H2Database() throws Exception {
        this.source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        try (var connection = this.source.getConnection(); var stream = this.getClass().getResourceAsStream("/META-INF/dataway/schema/h2.sql"); var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
        this.access = new JdbcDataAccessLayer(this.source, "");
    }

    public void publish(Dataway dataway, String method, String path, String script) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(UUID.randomUUID().toString());
        definition.setMethod(method);
        definition.setPath(path);
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript(script);
        definition.setDescription("Integration test API");
        definition.setSample("{}");
        definition.setOptions("{\"resultHandler\":\"raw\"}");
        dataway.getAdminService().save(definition, 0);
        dataway.getAdminService().publish(definition.getId(), 1);
    }

    public int count(String table) throws SQLException {
        try (var connection = this.source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    @Override
    public void close() throws SQLException {
        try (var connection = this.source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }
}

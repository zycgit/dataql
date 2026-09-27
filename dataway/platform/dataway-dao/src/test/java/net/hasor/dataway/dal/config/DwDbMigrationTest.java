/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import net.hasor.dbvisitor.hasor.autoconfig.DefaultDataSource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DwDbMigrationTest {
    @Test
    public void shouldBaselineExistingDatabaseAtInitialVersion() throws Exception {
        DefaultDataSource dataSource = new DefaultDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setJdbcUrl("jdbc:h2:mem:dataway_baseline;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("create table legacy_dataway_table (id bigint primary key)");
        }

        new DwDbMigration(dataSource).migrate();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertTrue(this.tableExists(statement, "legacy_dataway_table"));
            assertFalse(this.tableExists(statement, "dw_auth_role"));
            try (ResultSet resultSet = statement.executeQuery(
                    "select \"version\", \"type\" from \"" + DwDbMigration.HISTORY_TABLE
                            + "\" where \"success\" = true and \"type\" = 'BASELINE'")) {
                assertTrue(resultSet.next());
                assertEquals(DwDbMigration.BASE_VERSION, resultSet.getString("version"));
                assertEquals("BASELINE", resultSet.getString("type"));
            }
        }
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "select count(*) from information_schema.tables where lower(table_name) = '" + tableName + "'")) {
            return resultSet.next() && resultSet.getInt(1) > 0;
        }
    }
}

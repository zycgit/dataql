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
import javax.sql.DataSource;
import net.hasor.dataway.hasor.DatawayModule;
import net.hasor.dataway.hasor.example.config.DatabaseConfiguration;
import org.h2.tools.RunScript;

/** Enables the example's database configuration for SQL integration scenarios. */
public final class SqlTestApplication implements AutoCloseable {
    private final TestApplication application;

    public SqlTestApplication(H2Database database) throws Exception {
        var settings = TestSettings.enabled();
        settings.setProperty("example.database.ds1.url", "jdbc:h2:mem:" + UUID.randomUUID());
        settings.setProperty("example.database.ds2.url", "jdbc:h2:mem:" + UUID.randomUUID());
        settings.setProperty("example.database.main.url", "jdbc:h2:mem:" + UUID.randomUUID());
        this.application = new TestApplication(new DatawayModule(), database.access, settings, DatabaseConfiguration.class, SqlTestConfiguration.class);
        DataSource source = this.application.context().getInstance(DataSource.class);
        try (var connection = source.getConnection(); var input = this.getClass().getResourceAsStream("/example/database/people.sql"); var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        } catch (Exception | Error failure) {
            this.application.close();
            throw failure;
        }
    }

    public String baseUrl() {
        return this.application.baseUrl();
    }

    public int count(String table) throws SQLException {
        DataSource source = this.application.context().getInstance(DataSource.class);
        try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    @Override
    public void close() throws Exception {
        this.application.close();
    }
}

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.h2.tools.RunScript;

/** Uses the runnable example, adding a business table to the primary source for transaction scenarios. */
public final class SqlTestApplication implements AutoCloseable {
    private final ExampleServer application;

    public SqlTestApplication() throws Throwable {
        this.application = new ExampleServer();
        DataSource source = this.application.context().getBean(DataSource.class);
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
        DataSource source = this.application.context().getBean(DataSource.class);
        try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    @Override
    public void close() {
        this.application.close();
    }
}

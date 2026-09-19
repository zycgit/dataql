/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import net.hasor.dataway.dal.providers.db.DbVisitorApiRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;

/** Database fixtures explicitly run the shipped DDL before constructing Dataway. */
public final class TestDatabase {
    private TestDatabase() {
    }

    public static JdbcDataSource empty() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:fixture_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        return source;
    }

    public static JdbcDataSource create() {
        JdbcDataSource source = empty();
        try (var connection = source.getConnection(); var stream = TestDatabase.class.getResourceAsStream("/META-INF/dataway/schema/h2.sql"); var reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        } catch (Exception e) {
            throw new AssertionError("Cannot prepare test database", e);
        }
        return source;
    }

    public static DbVisitorApiRepository repository() {
        return new DbVisitorApiRepository(create());
    }
}

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.sql.Connection;
import java.sql.SQLException;

/** Uses the supplied connection synchronously without committing, rolling back or closing it. */
@FunctionalInterface
public interface JdbcCallback<T> {
    T execute(Connection connection) throws SQLException;
}
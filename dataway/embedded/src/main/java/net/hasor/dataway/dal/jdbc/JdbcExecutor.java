/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.sql.SQLException;

/**
 * Owns the connection and transaction boundary of one synchronous DAL operation.
 * Implementations must invoke the callback once, propagate failures, and release their
 * connection after completion. A write batch must commit atomically or roll back;
 * when joining a host transaction, failures must mark that transaction rollback-only.
 * Implementations must support concurrent calls. Queries also pass through this hook.
 */
public interface JdbcExecutor {
    <T> T execute(JdbcCallback<T> callback) throws SQLException;
}

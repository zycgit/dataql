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
import java.util.Objects;
import javax.sql.DataSource;

/** Default local JDBC transactions for applications without a host transaction integration. */
public class LocalJdbcExecutor implements JdbcExecutor {
    private final DataSource              source;
    private final ThreadLocal<Connection> current      = new ThreadLocal<>();
    private final ThreadLocal<Boolean>    rollbackOnly = new ThreadLocal<>();

    public LocalJdbcExecutor(DataSource source) {
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public <T> T execute(JdbcCallback<T> callback) throws SQLException {
        if (this.current.get() != null) {
            try {
                return callback.execute(this.current.get());
            } catch (SQLException | RuntimeException | Error failure) {
                this.rollbackOnly.set(true);
                throw failure;
            }
        }

        try (Connection connection = this.source.getConnection()) {
            if (!connection.getAutoCommit()) {
                throw new SQLException("Standalone context requires an independent auto-commit connection");
            }

            connection.setAutoCommit(false);
            this.current.set(connection);

            try {
                T result = callback.execute(connection);
                if (Boolean.TRUE.equals(this.rollbackOnly.get())) {
                    throw new SQLException("Transaction marked rollback-only");
                }

                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | Error e) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            } finally {
                this.current.remove();
                this.rollbackOnly.remove();
            }
        }
    }
}
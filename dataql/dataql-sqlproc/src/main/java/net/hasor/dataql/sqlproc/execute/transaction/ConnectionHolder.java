/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;

/** Owns one JDBC connection until all transaction scopes and connection proxies release it. */
final class ConnectionHolder {
    private final String             sourceName;
    private final ConnectionProvider provider;
    private       Connection         connection;
    private       int                referenceCount;
    private       int                savepointCounter;

    ConnectionHolder(String sourceName, ConnectionProvider provider) {
        this.sourceName = sourceName;
        this.provider = provider;
    }

    synchronized void requested() {
        this.referenceCount++;
    }

    synchronized void released() throws SQLException {
        if (this.referenceCount > 0) {
            this.referenceCount--;
        }
        if (this.referenceCount == 0) {
            try {
                this.savepointCounter = 0;
                if (this.connection != null) {
                    this.connection.close();
                }
            } finally {
                this.connection = null;
            }
        }
    }

    synchronized Connection getConnection() throws SQLException {
        return this.getConnection(null);
    }

    synchronized Connection getConnection(Hints hints) throws SQLException {
        if (!this.isOpen()) {
            throw new SQLException("Connection holder is closed.");
        }
        if (this.connection == null) {
            this.connection = this.provider.findConnection(this.sourceName, hints);
        }
        return this.connection;
    }

    synchronized boolean isOpen() {
        return this.referenceCount > 0;
    }

    boolean hasTransaction() throws SQLException {
        return !this.getConnection().getAutoCommit();
    }

    void beginTransaction() throws SQLException {
        Connection conn = this.getConnection();
        if (conn.getAutoCommit()) {
            conn.setAutoCommit(false);
        }
    }

    void stopTransaction() throws SQLException {
        Connection conn = this.getConnection();
        if (!conn.getAutoCommit()) {
            conn.setAutoCommit(true);
        }
    }

    boolean supportsSavePoints() throws SQLException {
        return this.getConnection().getMetaData().supportsSavepoints();
    }

    Savepoint createSavepoint() throws SQLException {
        this.savepointCounter++;
        return this.getConnection().setSavepoint("DATAQL_SAVEPOINT_" + this.savepointCounter);
    }

    void releaseSavepoint(Savepoint savepoint) throws SQLException {
        this.getConnection().releaseSavepoint(savepoint);
    }

    void rollback(Savepoint savepoint) throws SQLException {
        this.getConnection().rollback(savepoint);
    }
}

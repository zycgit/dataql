/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.sql.SQLException;

/** JDBC transaction state and connection settings to restore on completion. */
final class TransactionObject {
    private final ConnectionHolder holder;
    private final Isolation        recoverIsolation;
    private       boolean          recoverAutoCommit;

    TransactionObject(ConnectionHolder holder, Isolation recoverIsolation) {
        this.holder = holder;
        this.recoverIsolation = recoverIsolation;
    }

    ConnectionHolder getHolder() {
        return this.holder;
    }

    Isolation getRecoverIsolation() {
        return this.recoverIsolation;
    }

    boolean hasTransaction() throws SQLException {
        return this.holder.hasTransaction();
    }

    void begin() throws SQLException {
        if (!this.holder.hasTransaction()) {
            this.recoverAutoCommit = true;
        }
        this.holder.beginTransaction();
    }

    void stop() throws SQLException {
        if (this.recoverAutoCommit) {
            this.recoverAutoCommit = false;
            this.holder.stopTransaction();
        }
    }

    void commit() throws SQLException {
        if (this.holder.hasTransaction()) {
            this.holder.getConnection().commit();
        }
    }

    void rollback() throws SQLException {
        if (this.holder.hasTransaction()) {
            this.holder.getConnection().rollback();
        }
    }
}

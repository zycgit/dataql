/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.sql.SQLException;
import java.sql.Savepoint;

/** State of one local transaction scope, including its savepoint or suspended transaction. */
final class TransactionStatus {
    private final Isolation         isolation;
    private       TransactionObject transactionObject;
    private       TransactionObject suspendedTransaction;
    private       Savepoint         savepoint;
    private       boolean           completed;
    private       boolean           rollbackOnly;
    private       boolean           newConnection;

    TransactionStatus(Isolation isolation) {
        this.isolation = isolation;
    }

    TransactionObject getTransactionObject() {
        return this.transactionObject;
    }

    void setTransactionObject(TransactionObject transactionObject) {
        this.transactionObject = transactionObject;
    }

    TransactionObject getSuspendedTransaction() {
        return this.suspendedTransaction;
    }

    void setSuspendedTransaction(TransactionObject suspendedTransaction) {
        this.suspendedTransaction = suspendedTransaction;
    }

    void markNewConnection() {
        this.newConnection = true;
    }

    void setCompleted() {
        this.completed = true;
    }

    void markSavepoint() throws SQLException {
        ConnectionHolder holder = this.transactionObject.getHolder();
        if (!holder.supportsSavePoints()) {
            throw new SQLException("Connection does not support savepoints.");
        }
        this.savepoint = holder.createSavepoint();
    }

    void releaseSavepoint() throws SQLException {
        this.transactionObject.getHolder().releaseSavepoint(this.savepoint);
        this.savepoint = null;
    }

    void rollbackToSavepoint() throws SQLException {
        this.transactionObject.getHolder().rollback(this.savepoint);
    }

    Isolation getIsolationLevel() {
        return this.isolation;
    }

    boolean isCompleted() {
        return this.completed;
    }

    boolean isRollbackOnly() {
        return this.rollbackOnly;
    }

    boolean isNewConnection() {
        return this.newConnection;
    }

    boolean isSuspend() {
        return this.suspendedTransaction != null;
    }

    boolean hasSavepoint() {
        return this.savepoint != null;
    }

    void setRollback() throws SQLException {
        if (this.completed) {
            throw new SQLException("Transaction is already completed.");
        }
        this.rollbackOnly = true;
    }
}

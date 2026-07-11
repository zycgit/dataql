/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import javax.sql.DataSource;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionConnectionManager.ConnectionHolder;

/** Local JDBC transaction manager migrated from dbVisitor's transaction core. */
public class LocalTransactionManager implements TransactionManager {
    private final Deque<LocalTransactionStatus> statusStack = new ArrayDeque<>();
    private final DataSource                    dataSource;

    public LocalTransactionManager(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource is null.");
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public boolean hasTransaction() {
        return !this.statusStack.isEmpty();
    }

    @Override
    public boolean isTopTransaction(TransactionStatus status) {
        return status != null && this.statusStack.peekFirst() == status;
    }

    @Override
    public void commit() throws SQLException {
        LocalTransactionStatus status = this.statusStack.peekFirst();
        if (status != null) {
            this.commit(status);
        }
    }

    @Override
    public void rollBack() throws SQLException {
        LocalTransactionStatus status = this.statusStack.peekFirst();
        if (status != null) {
            this.rollBack(status);
        }
    }

    @Override
    public TransactionStatus begin(Propagation propagation, Isolation isolation) throws SQLException {
        Objects.requireNonNull(propagation, "propagation is null.");
        LocalTransactionStatus status = new LocalTransactionStatus(propagation, isolation);
        status.setTransactionObject(this.getTransactionObject(status));
        this.statusStack.addFirst(status);

        if (status.getTransactionObject().hasTransaction()) {
            switch (propagation) {
                case REQUIRES_NEW -> {
                    this.suspend(status);
                    status.getTransactionObject().begin();
                }
                case NESTED -> status.markSavepoint();
                case NOT_SUPPORTED -> this.suspend(status);
                case NEVER -> {
                    this.cleanup(status);
                    throw new SQLException("Existing transaction found for propagation NEVER.");
                }
                default -> {
                }
            }
            return status;
        }

        if (propagation == Propagation.REQUIRED || propagation == Propagation.REQUIRES_NEW || propagation == Propagation.NESTED) {
            status.getTransactionObject().begin();
        } else if (propagation == Propagation.MANDATORY) {
            this.cleanup(status);
            throw new SQLException("No existing transaction found for propagation MANDATORY.");
        }
        return status;
    }

    @Override
    public void commit(TransactionStatus transactionStatus) throws SQLException {
        LocalTransactionStatus status = this.asLocalStatus(transactionStatus);
        this.checkNotCompleted(status);
        if (status.isReadOnly() || status.isRollbackOnly()) {
            this.rollBack(status);
            return;
        }

        try {
            this.completeNestedStatuses(status, true);
            if (status.hasSavepoint()) {
                status.releaseSavepoint();
            } else if (status.isNewConnection()) {
                status.getTransactionObject().commit();
            }
        } catch (SQLException e) {
            status.getTransactionObject().rollback();
            throw e;
        } finally {
            this.cleanup(status);
        }
    }

    @Override
    public void rollBack(TransactionStatus transactionStatus) throws SQLException {
        LocalTransactionStatus status = this.asLocalStatus(transactionStatus);
        this.checkNotCompleted(status);
        try {
            this.completeNestedStatuses(status, false);
            if (status.hasSavepoint()) {
                status.rollbackToSavepoint();
            } else if (status.isNewConnection()) {
                status.getTransactionObject().rollback();
            }
        } catch (SQLException e) {
            status.getTransactionObject().rollback();
            throw e;
        } finally {
            this.cleanup(status);
        }
    }

    private void completeNestedStatuses(LocalTransactionStatus target, boolean commit) throws SQLException {
        if (!this.statusStack.contains(target)) {
            throw new SQLException("Transaction is not managed by this manager.");
        }
        while (this.statusStack.peekFirst() != target) {
            if (commit) {
                this.commit(this.statusStack.peekFirst());
            } else {
                this.rollBack(this.statusStack.peekFirst());
            }
        }
    }

    private void suspend(LocalTransactionStatus status) throws SQLException {
        this.checkTop(status);
        TransactionObject suspended = status.getTransactionObject();
        status.setSuspendedTransaction(suspended);
        TransactionConnectionManager.clearHolder(this.dataSource);
        status.setTransactionObject(this.getTransactionObject(status));
    }

    private void resume(LocalTransactionStatus status) throws SQLException {
        if (!status.isCompleted() || !status.isSuspend()) {
            throw new SQLException("Suspended transaction cannot be resumed.");
        }
        this.checkTop(status);
        TransactionObject suspended = status.getSuspendedTransaction();
        TransactionConnectionManager.setHolder(this.dataSource, suspended.getHolder());
        status.setTransactionObject(suspended);
        status.setSuspendedTransaction(null);
        suspended.getHolder().released();
    }

    private void cleanup(LocalTransactionStatus status) throws SQLException {
        this.checkTop(status);
        status.setCompleted();
        TransactionObject transactionObject = status.getTransactionObject();
        if (transactionObject.getRecoverIsolation() != null) {
            transactionObject.getHolder().getConnection().setTransactionIsolation(transactionObject.getRecoverIsolation().getValue());
        }
        transactionObject.stop();
        transactionObject.getHolder().released();
        if (status.isSuspend()) {
            this.resume(status);
        }
        this.statusStack.removeFirst();
        status.setTransactionObject(null);
        status.setSuspendedTransaction(null);
    }

    private TransactionObject getTransactionObject(LocalTransactionStatus status) throws SQLException {
        ConnectionHolder holder = TransactionConnectionManager.getHolder(this.dataSource);
        boolean newConnection = !holder.isOpen();
        if (!newConnection) {
            newConnection = !holder.hasTransaction();
        }
        if (newConnection) {
            status.markNewConnection();
        }
        holder.requested();
        Connection connection;
        try {
            connection = holder.getConnection();
        } catch (SQLException e) {
            holder.released();
            throw e;
        }

        Isolation isolation = status.getIsolationLevel();
        if (isolation == null || isolation == Isolation.DEFAULT) {
            return new TransactionObject(holder, null);
        }
        Isolation recoverIsolation = Isolation.valueOf(connection.getTransactionIsolation());
        if (recoverIsolation != isolation) {
            connection.setTransactionIsolation(isolation.getValue());
        }
        return new TransactionObject(holder, recoverIsolation);
    }

    private LocalTransactionStatus asLocalStatus(TransactionStatus status) throws SQLException {
        if (!(status instanceof LocalTransactionStatus localStatus)) {
            throw new SQLException("Unsupported transaction status.");
        }
        return localStatus;
    }

    private void checkNotCompleted(LocalTransactionStatus status) throws SQLException {
        if (status.isCompleted()) {
            throw new SQLException("Transaction is already completed.");
        }
    }

    private void checkTop(LocalTransactionStatus status) throws SQLException {
        if (!this.isTopTransaction(status)) {
            throw new SQLException("Transaction status is not top in stack.");
        }
    }

    public TransactionStatus lastTransaction() {
        return this.statusStack.peekFirst();
    }

    @Override
    public void close() throws IOException {
        if (this.statusStack.isEmpty()) {
            return;
        }
        try {
            this.commit(this.statusStack.peekLast());
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    private static final class TransactionObject {
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

    private static final class LocalTransactionStatus implements TransactionStatus {
        private final Propagation       propagation;
        private final Isolation         isolation;
        private       TransactionObject transactionObject;
        private       TransactionObject suspendedTransaction;
        private       Savepoint         savepoint;
        private       boolean           completed;
        private       boolean           rollbackOnly;
        private       boolean           readOnly;
        private       boolean           newConnection;

        LocalTransactionStatus(Propagation propagation, Isolation isolation) {
            this.propagation = propagation;
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
            if (!holder.supportsSavepoints()) {
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

        @Override
        public Propagation getPropagation() {
            return this.propagation;
        }

        @Override
        public Isolation getIsolationLevel() {
            return this.isolation;
        }

        @Override
        public boolean isCompleted() {
            return this.completed;
        }

        @Override
        public boolean isRollbackOnly() {
            return this.rollbackOnly;
        }

        @Override
        public boolean isReadOnly() {
            return this.readOnly;
        }

        @Override
        public boolean isNewConnection() {
            return this.newConnection;
        }

        @Override
        public boolean isSuspend() {
            return this.suspendedTransaction != null;
        }

        @Override
        public boolean hasSavepoint() {
            return this.savepoint != null;
        }

        @Override
        public void setRollback() throws SQLException {
            if (this.completed) {
                throw new SQLException("Transaction is already completed.");
            }
            this.rollbackOnly = true;
        }

        @Override
        public void setReadOnly() throws SQLException {
            if (this.completed) {
                throw new SQLException("Transaction is already completed.");
            }
            this.readOnly = true;
        }
    }
}

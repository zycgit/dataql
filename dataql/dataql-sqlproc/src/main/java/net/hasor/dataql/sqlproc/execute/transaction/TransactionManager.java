/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;

/** Local JDBC transactions for one datasource on the current thread. */
final class TransactionManager {
    private final Deque<TransactionStatus> statusStack = new ArrayDeque<>();
    private final String                   sourceName;
    private final ConnectionProvider       provider;
    private       ConnectionHolder         currentHolder;

    TransactionManager(String sourceName, ConnectionProvider provider) {
        this.sourceName = sourceName;
        this.provider = Objects.requireNonNull(provider, "connectionProvider is null.");
    }

    Object execute(Hints hints, Propagation propagation, Isolation isolation, TransactionCallback callback) throws Throwable {
        TransactionStatus status = this.begin(hints, propagation, isolation);
        try {
            return callback.execute();
        } catch (Throwable failure) {
            status.setRollback();
            throw failure;
        } finally {
            if (!status.isCompleted()) {
                this.commit(status);
            }
        }
    }

    private TransactionStatus begin(Hints hints, Propagation propagation, Isolation isolation) throws SQLException {
        Objects.requireNonNull(propagation, "propagation is null.");
        TransactionStatus status = new TransactionStatus(isolation);
        status.setTransactionObject(this.getTransactionObject(status, hints));
        this.statusStack.addFirst(status);

        if (status.getTransactionObject().hasTransaction()) {
            switch (propagation) {
                case REQUIRES_NEW -> {
                    this.suspend(status, hints);
                    status.getTransactionObject().begin();
                }
                case NESTED -> {
                    status.markSavepoint();
                }
                case NOT_SUPPORTED -> {
                    this.suspend(status, hints);
                }
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

    private void commit(TransactionStatus status) throws SQLException {
        this.checkNotCompleted(status);
        if (status.isRollbackOnly()) {
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

    private void rollBack(TransactionStatus status) throws SQLException {
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

    private void completeNestedStatuses(TransactionStatus target, boolean commit) throws SQLException {
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

    private void suspend(TransactionStatus status, Hints hints) throws SQLException {
        this.checkTop(status);
        TransactionObject suspended = status.getTransactionObject();
        status.setSuspendedTransaction(suspended);
        this.currentHolder = null;
        status.setTransactionObject(this.getTransactionObject(status, hints));
    }

    private void resume(TransactionStatus status) throws SQLException {
        if (!status.isCompleted() || !status.isSuspend()) {
            throw new SQLException("Suspended transaction cannot be resumed.");
        }
        this.checkTop(status);
        TransactionObject suspended = status.getSuspendedTransaction();
        this.currentHolder = suspended.getHolder();
        status.setTransactionObject(suspended);
        status.setSuspendedTransaction(null);
        suspended.getHolder().released();
    }

    private void cleanup(TransactionStatus status) throws SQLException {
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

    Connection getConnection(Hints hints) throws SQLException {
        ConnectionHolder holder = this.getHolder();
        holder.requested();
        try {
            holder.getConnection(hints);
        } catch (SQLException e) {
            holder.released();
            throw e;
        }
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class }, new ConnectionInvocationHandler(holder));
    }

    private TransactionObject getTransactionObject(TransactionStatus status, Hints hints) throws SQLException {
        ConnectionHolder holder = this.getHolder();
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
            connection = holder.getConnection(hints);
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

    private ConnectionHolder getHolder() {
        if (this.currentHolder == null || !this.currentHolder.isOpen()) {
            this.currentHolder = new ConnectionHolder(this.sourceName, this.provider);
        }
        return this.currentHolder;
    }

    private void checkNotCompleted(TransactionStatus status) throws SQLException {
        if (status.isCompleted()) {
            throw new SQLException("Transaction is already completed.");
        }
    }

    private void checkTop(TransactionStatus status) throws SQLException {
        if (this.statusStack.peekFirst() != status) {
            throw new SQLException("Transaction status is not top in stack.");
        }
    }

    void close() throws IOException {
        if (this.statusStack.isEmpty()) {
            return;
        }
        try {
            this.commit(this.statusStack.peekLast());
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}

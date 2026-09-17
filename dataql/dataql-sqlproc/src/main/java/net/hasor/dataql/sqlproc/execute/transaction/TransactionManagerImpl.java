/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;

/** Local JDBC transaction manager migrated from dbVisitor's transaction core. */
class TransactionManagerImpl implements TransactionManager {
    private final Deque<LocalTransactionStatus> statusStack   = new ArrayDeque<>();
    private final String                        sourceName;
    private final ConnectionProvider            provider;
    private final ThreadLocal<ConnectionHolder> currentHolder = new ThreadLocal<>();

    TransactionManagerImpl(String sourceName, ConnectionProvider provider) {
        this.sourceName = sourceName;
        this.provider = Objects.requireNonNull(provider, "connectionProvider is null.");
    }

    @Override
    public boolean isTopTransaction(TransactionStatus status) {
        return status != null && this.statusStack.peekFirst() == status;
    }

    @Override
    public TransactionStatus begin(Hints hints, Propagation propagation, Isolation isolation) throws SQLException {
        Objects.requireNonNull(propagation, "propagation is null.");
        LocalTransactionStatus status = new LocalTransactionStatus(propagation, isolation);
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

    private void suspend(LocalTransactionStatus status, Hints hints) throws SQLException {
        this.checkTop(status);
        TransactionObject suspended = status.getTransactionObject();
        status.setSuspendedTransaction(suspended);
        this.clearHolder();
        status.setTransactionObject(this.getTransactionObject(status, hints));
    }

    private void resume(LocalTransactionStatus status) throws SQLException {
        if (!status.isCompleted() || !status.isSuspend()) {
            throw new SQLException("Suspended transaction cannot be resumed.");
        }
        this.checkTop(status);
        TransactionObject suspended = status.getSuspendedTransaction();
        this.setHolder(suspended.getHolder());
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

    // for object

    public Connection getConnection(Hints hints) throws SQLException {
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

    private TransactionObject getTransactionObject(LocalTransactionStatus status, Hints hints) throws SQLException {
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

    // for ConnectionHolder

    private ConnectionHolder getHolder() {
        ConnectionHolder holder = this.currentHolder.get();
        if (holder == null) {
            holder = new ConnectionHolder();
            this.currentHolder.set(holder);
        }
        return holder;
    }

    private void setHolder(ConnectionHolder holder) {
        this.currentHolder.set(holder);
    }

    private void clearHolder() {
        this.currentHolder.remove();
    }

    void removeHolder(ConnectionHolder holder) {
        if (this.currentHolder.get() == holder) {
            this.currentHolder.remove();
        }
    }

    // for Status

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

    // utils and helper methods.

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

    /** Holds the JDBC connection bound to one local transaction manager on the current thread. */
    private final class ConnectionHolder {
        private Connection connection;
        private int        referenceCount;
        private int        savepointCounter;

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
                    removeHolder(this);
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
                this.connection = provider.findConnection(sourceName, hints);
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

    /** Invocation handler for connection proxies returned by the local transaction manager. */
    private static final class ConnectionInvocationHandler implements InvocationHandler {
        private final ConnectionHolder holder;
        private final AtomicBoolean    closed = new AtomicBoolean();

        ConnectionInvocationHandler(ConnectionHolder holder) {
            this.holder = holder;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "toString" -> "DataQL transaction connection proxy";
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "isClosed" -> this.closed.get();
                case "close" -> {
                    if (this.closed.compareAndSet(false, true)) {
                        this.holder.released();
                    }
                    yield null;
                }
                default -> this.invokeConnection(method, args);
            };
        }

        private Object invokeConnection(Method method, Object[] args) throws Throwable {
            if (this.closed.get()) {
                throw new SQLException("Connection is closed.");
            }
            Connection conn = this.holder.getConnection();
            try {
                return method.invoke(conn, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
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

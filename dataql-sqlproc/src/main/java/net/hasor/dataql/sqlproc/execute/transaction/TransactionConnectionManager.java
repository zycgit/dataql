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
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.sql.DataSource;
import net.hasor.cobble.function.EFunction;

/**
 * Coordinates SQL fragment connections and transaction managers by datasource name.
 * Transaction state is bound to the current thread.
 */
public class TransactionConnectionManager {
    private static final ThreadLocal<Map<DataSource, ConnectionHolder>> HOLDERS = ThreadLocal.withInitial(HashMap::new);

    private final EFunction<String, Connection, SQLException>  connectionProvider;
    private final Map<String, ManagedDataSource>               dataSources         = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<String, TransactionContext>> transactionContexts = ThreadLocal.withInitial(HashMap::new);

    public TransactionConnectionManager(EFunction<String, Connection, SQLException> connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is null.");
    }

    public Connection getConnection(String sourceName) throws SQLException {
        ConnectionHolder holder = getHolder(this.dataSource(sourceName));
        holder.requested();
        try {
            holder.getConnection();
        } catch (SQLException e) {
            holder.released();
            throw e;
        }
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class }, new ConnectionInvocationHandler(holder));
    }

    public TransactionManager getTransactionManager(String sourceName) {
        return this.transactionContext(sourceName).transactionManager;
    }

    public TransactionTemplate getTransactionTemplate(String sourceName) {
        return this.transactionContext(sourceName).transactionTemplate;
    }

    private TransactionContext transactionContext(String sourceName) {
        return this.transactionContexts.get().computeIfAbsent(sourceKey(sourceName), key -> {
            LocalTransactionManager transactionManager = new LocalTransactionManager(this.dataSource(sourceName));
            return new TransactionContext(transactionManager, new TransactionTemplate(transactionManager));
        });
    }

    private ManagedDataSource dataSource(String sourceName) {
        return this.dataSources.computeIfAbsent(sourceKey(sourceName), key -> new ManagedDataSource(sourceName, this.connectionProvider));
    }

    private static String sourceKey(String sourceName) {
        return sourceName == null ? "" : sourceName;
    }

    private record TransactionContext(LocalTransactionManager transactionManager, TransactionTemplate transactionTemplate) {
    }

    static ConnectionHolder getHolder(DataSource dataSource) {
        return HOLDERS.get().computeIfAbsent(dataSource, ConnectionHolder::new);
    }

    static void setHolder(DataSource dataSource, ConnectionHolder holder) {
        HOLDERS.get().put(dataSource, holder);
    }

    static void clearHolder(DataSource dataSource) {
        HOLDERS.get().remove(dataSource);
    }

    private static void removeHolder(DataSource dataSource, ConnectionHolder holder) {
        Map<DataSource, ConnectionHolder> holderMap = HOLDERS.get();
        if (holderMap.get(dataSource) == holder) {
            holderMap.remove(dataSource);
        }
        if (holderMap.isEmpty()) {
            HOLDERS.remove();
        }
    }

    static final class ConnectionHolder {
        private static final String SAVEPOINT_PREFIX = "DATAQL_SAVEPOINT_";

        private final DataSource dataSource;
        private       Connection connection;
        private       int        referenceCount;
        private       int        savepointCounter;

        ConnectionHolder(DataSource dataSource) {
            this.dataSource = dataSource;
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
                    removeHolder(this.dataSource, this);
                }
            }
        }

        synchronized Connection getConnection() throws SQLException {
            if (!this.isOpen()) {
                throw new SQLException("Connection holder is closed.");
            }
            if (this.connection == null) {
                this.connection = this.dataSource.getConnection();
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

        boolean supportsSavepoints() throws SQLException {
            return this.getConnection().getMetaData().supportsSavepoints();
        }

        Savepoint createSavepoint() throws SQLException {
            this.savepointCounter++;
            return this.getConnection().setSavepoint(SAVEPOINT_PREFIX + this.savepointCounter);
        }

        void releaseSavepoint(Savepoint savepoint) throws SQLException {
            this.getConnection().releaseSavepoint(savepoint);
        }

        void rollback(Savepoint savepoint) throws SQLException {
            this.getConnection().rollback(savepoint);
        }
    }

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

    private record ManagedDataSource(String sourceName, EFunction<String, Connection, SQLException> connectionProvider) implements DataSource {

        @Override
            public Connection getConnection() throws SQLException {
                return this.connectionProvider.eApply(this.sourceName);
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return this.getConnection();
            }

            @Override
            public PrintWriter getLogWriter() {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) {
            }

            @Override
            public void setLoginTimeout(int seconds) {
            }

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                throw new SQLFeatureNotSupportedException();
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                if (iface.isInstance(this)) {
                    return iface.cast(this);
                }
                throw new SQLException("Not a wrapper for " + iface.getName() + ".");
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return iface.isInstance(this);
            }
        }
}

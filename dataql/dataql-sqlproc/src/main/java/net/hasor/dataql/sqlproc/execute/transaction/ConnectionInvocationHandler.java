/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Releases a connection reference when a SQL fragment closes its connection proxy. */
final class ConnectionInvocationHandler implements InvocationHandler {
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

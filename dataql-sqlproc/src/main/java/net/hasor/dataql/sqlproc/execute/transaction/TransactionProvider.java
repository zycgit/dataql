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
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;

/**
 * Coordinates SQL fragment connections and transaction managers by datasource name.
 * Transaction state is bound to the current thread.
 */
public class TransactionProvider implements ConnectionProvider, Closeable {
    private final ConnectionProvider                               delegate;
    private final ThreadLocal<Map<String, TransactionManagerImpl>> contexts = ThreadLocal.withInitial(HashMap::new);

    public TransactionProvider(ConnectionProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate, "connectionProvider is null.");
    }

    @Override
    public Connection findConnection(String sourceName, Hints hints) throws SQLException {
        return this.transactionContext(sourceName).getConnection(hints);
    }

    public TransactionManager findTransactionManager(String sourceName) {
        return this.transactionContext(sourceName);
    }

    private TransactionManagerImpl transactionContext(String sourceName) {
        String sourceKey = sourceName == null ? "" : sourceName;
        return this.contexts.get().computeIfAbsent(sourceKey, key -> {
            return new TransactionManagerImpl(sourceName, this.delegate);
        });
    }

    @Override
    public void close() throws IOException {
        IOException error = null;
        try {
            for (TransactionManagerImpl manager : this.contexts.get().values()) {
                try {
                    manager.close();
                } catch (IOException e) {
                    if (error == null) {
                        error = e;
                    } else {
                        error.addSuppressed(e);
                    }
                }
            }
        } finally {
            this.contexts.remove();
        }
        if (error != null) {
            throw error;
        }
    }

}

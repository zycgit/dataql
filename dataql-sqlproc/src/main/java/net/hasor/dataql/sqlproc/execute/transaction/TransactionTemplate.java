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
import java.util.Objects;

/** Callback-based transaction boundary. */
public class TransactionTemplate {
    private final TransactionManager transactionManager;

    public TransactionTemplate(TransactionManager transactionManager) {
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager is null.");
    }

    public <T> T execute(TransactionCallback<T> callback) throws Throwable {
        return this.execute(callback, Propagation.REQUIRED, Isolation.DEFAULT);
    }

    public <T> T execute(TransactionCallback<T> callback, Propagation propagation) throws Throwable {
        return this.execute(callback, propagation, Isolation.DEFAULT);
    }

    public <T> T execute(TransactionCallback<T> callback, Propagation propagation, Isolation isolation) throws Throwable {
        TransactionStatus status = null;
        try {
            status = this.transactionManager.begin(propagation, isolation);
            return callback.doTransaction(status);
        } catch (Throwable e) {
            if (status != null) {
                status.setRollback();
            }
            throw e;
        } finally {
            if (status != null && !status.isCompleted()) {
                this.transactionManager.commit(status);
            }
        }
    }
}

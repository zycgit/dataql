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
import java.sql.SQLException;

/** Programmatic transaction manager. */
public interface TransactionManager extends Closeable {
    default TransactionStatus begin() throws SQLException {
        return this.begin(Propagation.REQUIRED, Isolation.DEFAULT);
    }

    default TransactionStatus begin(Propagation propagation) throws SQLException {
        return this.begin(propagation, Isolation.DEFAULT);
    }

    TransactionStatus begin(Propagation propagation, Isolation isolation) throws SQLException;

    void commit(TransactionStatus status) throws SQLException;

    void commit() throws SQLException;

    void rollBack(TransactionStatus status) throws SQLException;

    void rollBack() throws SQLException;

    boolean hasTransaction();

    boolean isTopTransaction(TransactionStatus status);
}

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.io.Closeable;
import java.sql.SQLException;
import net.hasor.dataql.domain.Hints;

/** Programmatic transaction manager. */
public interface TransactionManager extends Closeable {
    TransactionStatus begin(Hints hints, Propagation propagation, Isolation isolation) throws SQLException;

    void commit(TransactionStatus status) throws SQLException;

    void rollBack(TransactionStatus status) throws SQLException;

    boolean isTopTransaction(TransactionStatus status);
}

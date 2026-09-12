/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.sql.SQLException;

/** State returned by a transaction manager. */
public interface TransactionStatus {
    Propagation getPropagation();

    Isolation getIsolationLevel();

    boolean isCompleted();

    boolean isRollbackOnly();

    boolean isReadOnly();

    boolean isNewConnection();

    boolean isSuspend();

    boolean hasSavepoint();

    void setRollback() throws SQLException;

    void setReadOnly() throws SQLException;
}

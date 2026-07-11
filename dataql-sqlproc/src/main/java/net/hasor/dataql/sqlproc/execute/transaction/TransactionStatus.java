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

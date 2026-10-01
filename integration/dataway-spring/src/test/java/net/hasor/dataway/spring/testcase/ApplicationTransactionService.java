/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionCallback;
import org.springframework.transaction.annotation.Transactional;

/** Represents an application service calling DataQL within a Spring-managed transaction. */
public class ApplicationTransactionService {
    @Transactional(transactionManager = "ds1TransactionManager", rollbackFor = Throwable.class)
    public Object execute(TransactionCallback callback) throws Throwable {
        return callback.execute();
    }
}
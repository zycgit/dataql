/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;

/** Work executed within the transaction selected by a connection provider. */
@FunctionalInterface
public interface TransactionCallback {
    Object execute() throws Throwable;
}
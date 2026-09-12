/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.interceptor;

/** An invocation in the final SQL execution chain. */
public interface SqlExecutionInvocation {
    /** Returns the final SQL and binding information for this execution. */
    SqlInfo getSqlInfo();

    /** Continues with the next interceptor, or performs JDBC execution at the end of the chain. */
    Object proceed() throws Throwable;
}

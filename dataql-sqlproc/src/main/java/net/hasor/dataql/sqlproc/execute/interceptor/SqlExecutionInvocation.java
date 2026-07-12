/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.interceptor;

/** An invocation in the final SQL execution chain. */
public interface SqlExecutionInvocation {
    /** Returns the final SQL and binding information for this execution. */
    SqlInfo getSqlInfo();

    /** Continues with the next interceptor, or performs JDBC execution at the end of the chain. */
    Object proceed() throws Throwable;
}

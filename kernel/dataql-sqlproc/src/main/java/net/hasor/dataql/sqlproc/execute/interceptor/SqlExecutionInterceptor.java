/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.interceptor;
import java.util.EventListener;

/**
 * Intercepts a final SQL execution. Implementations call {@link SqlExecutionInvocation#proceed()}
 * to continue the chain, or return a result directly to skip the remaining interceptors and JDBC execution.
 */
@FunctionalInterface
public interface SqlExecutionInterceptor extends EventListener {
    /** Execute around the current SQL invocation. */
    Object invoke(SqlExecutionInvocation invocation) throws Throwable;
}

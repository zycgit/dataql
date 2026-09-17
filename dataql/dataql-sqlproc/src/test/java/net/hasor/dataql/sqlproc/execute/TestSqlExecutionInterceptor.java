/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute;

import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInvocation;

public class TestSqlExecutionInterceptor implements SqlExecutionInterceptor {
    private static final ThreadLocal<Handler> HANDLER = new ThreadLocal<>();

    @Override
    public Object invoke(SqlExecutionInvocation invocation) throws Throwable {
        Handler handler = HANDLER.get();
        return handler != null ? handler.invoke(invocation) : invocation.proceed();
    }

    public static void use(Handler handler) {
        HANDLER.set(handler);
    }

    public static void clear() {
        HANDLER.remove();
    }

    @FunctionalInterface
    public interface Handler {
        Object invoke(SqlExecutionInvocation invocation) throws Throwable;
    }
}

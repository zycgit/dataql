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
import java.sql.SQLException;
import java.util.List;

public final class SqlExecutionChain implements SqlExecutionInvocation {
    private final List<SqlExecutionInterceptor> interceptors;
    private final SqlInfo                       sqlInfo;
    private final Terminal                      terminal;
    private       int                           index;

    private SqlExecutionChain(List<SqlExecutionInterceptor> interceptors, SqlInfo sqlInfo, Terminal terminal) {
        this.interceptors = interceptors;
        this.sqlInfo = sqlInfo;
        this.terminal = terminal;
    }

    public static Object execute(List<SqlExecutionInterceptor> interceptors, SqlInfo sqlInfo, Terminal terminal) throws SQLException {
        try {
            return new SqlExecutionChain(interceptors, sqlInfo, terminal).proceed();
        } catch (SQLException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new SQLException("SQL execution interceptor failed.", e);
        }
    }

    @Override
    public SqlInfo getSqlInfo() {
        return this.sqlInfo;
    }

    @Override
    public Object proceed() throws Throwable {
        if (this.index < this.interceptors.size()) {
            return this.interceptors.get(this.index++).invoke(this);
        }
        return this.terminal.invoke();
    }

    @FunctionalInterface
    public interface Terminal {
        Object invoke() throws Throwable;
    }
}

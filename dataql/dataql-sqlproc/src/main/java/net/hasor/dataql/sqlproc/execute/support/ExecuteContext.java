/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.support;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionPredicate;

public interface ExecuteContext extends QueryContext {

    Connection findConnection(String sourceName, Hints hints) throws SQLException;

    /** Register an interceptor. */
    void addInterceptor(SqlExecutionInterceptor interceptor);

    /** Register an interceptor with its matching condition. */
    void addInterceptor(SqlExecutionInterceptor interceptor, SqlExecutionPredicate predicate);

    /** Remove an interceptor by identity. */
    boolean removeInterceptor(SqlExecutionInterceptor interceptor);

    /**
     * Return interceptors whose registration predicate matches the execution context.
     */
    List<SqlExecutionInterceptor> filterInterceptors(QueryType type, String fragmentString, Hints hints);

    //Options options();
}

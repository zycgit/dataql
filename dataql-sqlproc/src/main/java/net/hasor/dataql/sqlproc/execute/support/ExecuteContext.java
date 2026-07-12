/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    void setConnectionProvider(ConnectionProvider provider);

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

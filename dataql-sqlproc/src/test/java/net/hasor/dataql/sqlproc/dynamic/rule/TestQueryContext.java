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
package net.hasor.dataql.sqlproc.dynamic.rule;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hasor.cobble.ClassUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.dynamic.MacroRegistry;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionPredicate;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

/**
 * 多个 SQL 节点组合成一个 SqlNode
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-05-24
 */
public class TestQueryContext implements ExecuteContext {
    private final TypeHandlerRegistry           typeHandlerRegistry = new TypeHandlerRegistry();
    private final RuleRegistry                  ruleRegistry        = new RuleRegistry();
    private final MacroRegistry                 macroRegistry       = new MacroRegistry();
    private final List<InterceptorRegistration> interceptors        = new CopyOnWriteArrayList<>();

    public void addMacro(String macroName, String sqlSegment) {
        this.macroRegistry.register(macroName, sqlSegment);
    }

    @Override
    public java.sql.Connection findConnection(String sourceName, Hints hints) throws SQLException {
        throw new SQLException("connection '" + sourceName + "' not configured");
    }

    @Override
    public SqlRule findRule(String ruleName) {
        return this.ruleRegistry.findRule(ruleName);
    }

    @Override
    public DynamicSql findMacro(String name) {
        return macroRegistry.findMacro(name);
    }

    @Override
    public TypeHandlerRegistry getTypeRegistry() {
        return this.typeHandlerRegistry;
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void addInterceptor(SqlExecutionInterceptor interceptor) {
        this.addInterceptor(interceptor, (type, fragmentString, hints) -> true);
    }

    @Override
    public void addInterceptor(SqlExecutionInterceptor interceptor, SqlExecutionPredicate predicate) {
        this.interceptors.add(new InterceptorRegistration(interceptor, predicate));
    }

    @Override
    public boolean removeInterceptor(SqlExecutionInterceptor interceptor) {
        return this.interceptors.removeIf(registration -> registration.interceptor() == interceptor);
    }

    @Override
    public List<SqlExecutionInterceptor> filterInterceptors(QueryType type, String fragmentString, Hints hints) {
        List<SqlExecutionInterceptor> result = new ArrayList<>(this.interceptors.size());
        for (InterceptorRegistration registration : this.interceptors) {
            if (registration.predicate().accept(type, fragmentString, hints)) {
                result.add(registration.interceptor());
            }
        }
        return result;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return ClassUtils.getClass(this.getClassLoader(), className);
    }

    private record InterceptorRegistration(SqlExecutionInterceptor interceptor, SqlExecutionPredicate predicate) {
    }
}

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hasor.cobble.ClassUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.dynamic.MacroRegistry;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.rule.RuleRegistry;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionPredicate;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

public final class ExecuteContextImpl implements ExecuteContext {
    private final TypeHandlerRegistry typeRegistry  = TypeHandlerRegistry.DEFAULT;
    private final MacroRegistry       macroRegistry = new MacroRegistry();
    private final RuleRegistry                  ruleRegistry  = new RuleRegistry();
    private final ClassLoader                   classLoader;
    private final HostContext                   hostContext;
    private final List<InterceptorRegistration> interceptors  = new CopyOnWriteArrayList<>();

    private record InterceptorRegistration(SqlExecutionInterceptor interceptor, SqlExecutionPredicate predicate) {
    }

    ExecuteContextImpl(HostContext hostContext, ClassLoader classLoader) {
        this.hostContext = Objects.requireNonNull(hostContext, "hostContext is null.");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader is null.");
    }

    @Override
    public SqlRule findRule(String ruleName) {
        return this.ruleRegistry.findRule(ruleName);
    }

    @Override
    public DynamicSql findMacro(String dynamicId) {
        return this.macroRegistry.findMacro(dynamicId);
    }

    public void addMacro(String name, String segment) {
        this.macroRegistry.register(name, segment);
    }

    @Override
    public TypeHandlerRegistry getTypeRegistry() {
        return this.typeRegistry;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public void addInterceptor(SqlExecutionInterceptor interceptor) {
        this.addInterceptor(interceptor, (type, fragmentString, hints) -> true);
    }

    @Override
    public void addInterceptor(SqlExecutionInterceptor interceptor, SqlExecutionPredicate predicate) {
        this.interceptors.add(new InterceptorRegistration(Objects.requireNonNull(interceptor, "interceptor is null."), Objects.requireNonNull(predicate, "predicate is null.")));
    }

    @Override
    public boolean removeInterceptor(SqlExecutionInterceptor interceptor) {
        return this.interceptors.removeIf(registration -> registration.interceptor() == interceptor);
    }

    @Override
    public List<SqlExecutionInterceptor> filterInterceptors(QueryType type, String fragmentString, Hints hints) {
        if (this.interceptors.isEmpty()) {
            return Collections.emptyList();
        }
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
        return ClassUtils.getClass(this.classLoader, className);
    }

    @Override
    public Connection findConnection(String sourceName, Hints hints) throws SQLException {
        return this.lookupConnection(sourceName, hints);
    }

    private Connection lookupConnection(String sourceName, Hints hints) throws SQLException {
        Connection c = null;
        ConnectionProvider provider = this.findConnectionProvider();
        if (provider != null) {
            c = provider.findConnection(sourceName, hints);
        }
        if (c == null) {
            throw new SQLException("connection '" + sourceName + "' not configured");
        }
        return c;
    }

    private ConnectionProvider findConnectionProvider() {
        try {
            return this.hostContext.getAttachment(ConnectionProvider.class);
        } catch (IllegalStateException e) {
            return null;
        }
    }
}

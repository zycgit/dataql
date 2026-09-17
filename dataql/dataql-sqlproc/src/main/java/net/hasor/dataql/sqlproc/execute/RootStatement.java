/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.config.InsertConfig;
import net.hasor.dataql.sqlproc.dynamic.config.SelectKeyConfig;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;

/**
 * 执行器总入口
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-07-20
 */
public class RootStatement {
    private final QueryContext                                 context;
    private final Map<StatementType, AbstractStatementExecute> executeMap;

    public RootStatement(QueryContext context) {
        this.context = context;
        this.executeMap = new HashMap<>();
        for (StatementType statementType : StatementType.values()) {
            executeMap.put(statementType, this.createExecute(statementType, context));
        }
    }

    private AbstractStatementExecute createExecute(StatementType statementType, QueryContext context) {
        return switch (statementType) {
            case Statement -> new StatementExecute(context);
            case Prepared -> new PreparedStatementExecute(context);
            case Callable -> new CallableStatementExecute(context);
            default -> {
                throw new UnsupportedOperationException("statementType '" + statementType.name() + "' Unsupported.");
            }
        };
    }

    public Object execute(Connection conn, Hints hints, SqlConfig config, Map<String, Object> data, Page pageInfo, boolean pageResult, List<SqlExecutionInterceptor> interceptors) throws SQLException {
        interceptors = interceptors == null ? Collections.emptyList() : interceptors;
        SelectKeyStatementExecute selectKeyExecute = null;

        if (config instanceof InsertConfig) {
            SelectKeyConfig keyConfig = ((InsertConfig) config).getSelectKey();
            if (keyConfig != null) {
                AbstractStatementExecute selectKey = this.createExecute(config.getStatementType(), context);
                selectKeyExecute = new SelectKeyStatementExecute(keyConfig, selectKey);
            }
        }

        if (selectKeyExecute != null) {
            selectKeyExecute.processBefore(conn, hints, data, interceptors);
        }

        AbstractStatementExecute statement = this.executeMap.get(config.getStatementType());
        Object result = statement.execute(conn, hints, config, data, pageInfo, pageResult, interceptors);

        if (selectKeyExecute != null) {
            selectKeyExecute.processAfter(conn, hints, data, interceptors);
        }

        return result;
    }
}

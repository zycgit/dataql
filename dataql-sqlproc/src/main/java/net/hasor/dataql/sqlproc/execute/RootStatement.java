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
package net.hasor.dataql.sqlproc.execute;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.config.InsertConfig;
import net.hasor.dataql.sqlproc.dynamic.config.SelectKeyConfig;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;

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
            default -> throw new UnsupportedOperationException("statementType '" + statementType.name() + "' Unsupported.");
        };
    }

    public Object execute(Connection conn, Hints hints, SqlConfig config, Map<String, Object> data, Page pageInfo, boolean pageResult) throws SQLException {
        SelectKeyStatementExecute selectKeyExecute = null;

        if (config instanceof InsertConfig) {
            SelectKeyConfig keyConfig = ((InsertConfig) config).getSelectKey();
            if (keyConfig != null) {
                AbstractStatementExecute selectKey = this.createExecute(config.getStatementType(), context);
                selectKeyExecute = new SelectKeyStatementExecute(keyConfig, selectKey);
            }
        }

        if (selectKeyExecute != null) {
            selectKeyExecute.processBefore(conn, hints, data);
        }

        Object result = this.executeMap.get(config.getStatementType()).execute(conn, hints, config, data, pageInfo, pageResult);

        if (selectKeyExecute != null) {
            selectKeyExecute.processAfter(conn, hints, data);
        }

        return result;
    }
}
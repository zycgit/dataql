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
import net.hasor.cobble.ExceptionUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.dialect.PageDialect;
import net.hasor.dataql.sqlproc.execute.page.Page;
import net.hasor.dataql.sqlproc.fragment.QueryType;
import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.StatementType;
import net.hasor.dataql.sqlproc.repository.config.QueryProcSql;
import net.hasor.dataql.sqlproc.repository.config.SelectKeyProcSql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 执行器总入口
 * @version : 2021-07-20
 * @author 赵永春 (zyc@hasor.net)
 */
public class ExecuteProxy {
    private final QueryType                   queryType;
    private final QueryProcSql                dynamicSql;
    private final AbstractStatementExecute<?> execute;
    private       SelectKeyExecute            selectKeyExecute;

    public ExecuteProxy(QueryType queryType, QueryProcSql dynamicSql, DynamicContext context) {
        this.queryType = queryType;
        this.dynamicSql = dynamicSql;
        this.execute = buildExecute(this.dynamicSql.getStatementType(), context);

        SelectKeyProcSql selectKey = dynamicSql.getSelectKey();
        if (selectKey != null) {
            AbstractStatementExecute<?> selectKeyExecute = buildExecute(selectKey.getStatementType(), context);
            SelectKeyHandler selectKeyHandler = null;

            if (StringUtils.isBlank(selectKey.getHandler())) {
                selectKeyHandler = new SelectKeySequenceHolder(selectKey, selectKeyExecute);
            } else {
                try {
                    Class<?> aClass = context.loadClass(selectKey.getHandler());
                    SelectKeyHandlerFactory holderFactory = (SelectKeyHandlerFactory) aClass.newInstance();
                    selectKeyHandler = holderFactory.createHandler(selectKey, selectKeyExecute);
                    if (selectKeyHandler == null) {
                        throw new NullPointerException("createSelectKeyHolder result is null.");
                    }
                } catch (Exception e) {
                    throw ExceptionUtils.toRuntime(e);
                }
            }
            this.selectKeyExecute = new SelectKeyExecute(selectKey, selectKeyHandler);
        }
    }

    public boolean isDynamic() {
        return this.dynamicSql.isDynamic();
    }

    public boolean isHavePlaceholder() {
        return this.dynamicSql.isHavePlaceholder();
    }

    public QueryType getQueryType() {
        return this.queryType;
    }

    private AbstractStatementExecute<?> buildExecute(StatementType statementType, DynamicContext context) {
        switch (statementType) {
            case Statement: {
                return new StatementExecute(context);
            }
            case Prepared: {
                return new PreparedStatementExecute(context);
            }
            case Callable: {
                return new CallableStatementExecute(context);
            }
            default: {
                throw new UnsupportedOperationException("statementType '" + statementType.getTypeName() + "' Unsupported.");
            }
        }
    }

    public Object execute(Connection conn, Map<String, Object> data, //
            boolean pageResult, boolean pageCount, Page pageInfo, PageDialect dialect,//
            OpenPackageType resultType, ColumnCaseType columnCaseType) throws SQLException {

        if (this.selectKeyExecute != null) {
            this.selectKeyExecute.processBefore(conn, data, resultType, columnCaseType);
        }

        Object result = this.execute.execute(conn, this.dynamicSql, data, pageResult, pageCount, pageInfo, dialect, resultType, columnCaseType);

        if (this.selectKeyExecute != null) {
            this.selectKeyExecute.processAfter(conn, data, resultType, columnCaseType);
        }

        return result;
    }

    private static class SelectKeySequenceHolder implements SelectKeyHandler {
        private final SelectKeyProcSql            keySqlConfig;
        private final AbstractStatementExecute<?> selectKeyExecute;

        public SelectKeySequenceHolder(SelectKeyProcSql keySqlConfig, AbstractStatementExecute<?> selectKeyExecute) {
            this.keySqlConfig = keySqlConfig;
            this.selectKeyExecute = selectKeyExecute;
        }

        public Object processSelectKey(Connection conn, Map<String, Object> parameter, OpenPackageType resultType, ColumnCaseType columnCaseType) throws SQLException {
            Object resultValue = this.selectKeyExecute.execute(conn, this.keySqlConfig, parameter, false, false, null, null, resultType, columnCaseType);

            if (resultValue instanceof List) {
                resultValue = ((List<?>) resultValue).get(0);
            }

            return resultValue;
        }
    }
}

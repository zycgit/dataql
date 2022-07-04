/*
 * Copyright 2002-2005 the original author or authors.
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
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.dialect.PageDialect;
import net.hasor.dataql.sqlproc.dialect.SqlDialectRegister;
import net.hasor.dataql.sqlproc.dynamic.DynamicContext;
import net.hasor.dataql.sqlproc.execute.config.AbstractProcSql;
import net.hasor.dataql.sqlproc.execute.config.QueryProcSql;
import net.hasor.dataql.sqlproc.execute.config.SelectKeyProcSql;
import net.hasor.dataql.sqlproc.execute.sequence.SelectKeySequenceHolderFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static net.hasor.dataql.sqlproc.SqlHintNames.FRAGMENT_SQL_PAGE_DIALECT;

/**
 * 执行器总入口
 * @version : 2021-07-20
 * @author 赵永春 (zyc@hasor.net)
 */
public class JdbcSqlExecute {
    private final AbstractStatementExecute<?> execute;
    private       KeySequenceExecute          selectKeyHolder;

    public JdbcSqlExecute(QueryProcSql procSql, DynamicContext context) {
        this.execute = buildExecute(procSql, context);

        SelectKeyProcSql selectKey = procSql.getSelectKey();
        if (selectKey != null) {
            AbstractStatementExecute<?> selectKeyExecute = buildExecute(selectKey, context);
            KeySequenceHolder sequenceHolder = null;

            if (StringUtils.isBlank(selectKey.getHandler())) {
                sequenceHolder = new SelectKeySequenceHolderFactory().createHolder(selectKey, selectKeyExecute);
            } else {
                try {
                    Class<?> aClass = context.loadClass(selectKey.getHandler());
                    KeySequenceHolderFactory holderFactory = (KeySequenceHolderFactory) aClass.newInstance();
                    sequenceHolder = holderFactory.createHolder(selectKey, selectKeyExecute);
                    if (sequenceHolder == null) {
                        throw new NullPointerException("createSelectKeyHolder result is null.");
                    }
                } catch (Exception e) {
                    throw ExceptionUtils.toRuntime(e);
                }
            }

            this.selectKeyHolder = new KeySequenceExecute(selectKey, sequenceHolder);
        }
    }

    private AbstractStatementExecute<?> buildExecute(AbstractProcSql procSql, DynamicContext context) {
        switch (procSql.getStatementType()) {
            case Statement: {
                return new StatementExecute(procSql, context);
            }
            case Prepared: {
                return new PreparedStatementExecute(procSql, context);
            }
            case Callable: {
                return new CallableStatementExecute(procSql, context);
            }
            default: {
                throw new UnsupportedOperationException("statementType '" + procSql.getStatementType().getTypeName() + "' Unsupported.");
            }
        }
    }

    public Object execute(Connection conn, Map<String, Object> data, Page page, boolean pageResult, PageDialect dialect) throws SQLException {
        if (this.selectKeyHolder != null) {
            this.selectKeyHolder.processBefore(conn, data);
        }

        Object result = this.execute.execute(conn, data, page, pageResult, dialect);

        if (this.selectKeyHolder != null) {
            this.selectKeyHolder.processAfter(conn, data);
        }

        return result;
    }

    private PageDialect getDialect(Hints hint, Connection conn, DynamicContext classLoader) throws SQLException {
        // .优先从 hint 中取方言，取不到在自动推断
        String dialectType = hint.getOrDefault(FRAGMENT_SQL_PAGE_DIALECT.name(), "").toString();
        if (StringUtils.isBlank(dialectType)) {
            String jdbcUrl = conn.getMetaData().getURL();
            String jdbcDriverName = conn.getMetaData().getDriverName();
            dialectType = JdbcUtils.getDbType(jdbcUrl, jdbcDriverName);

            if (StringUtils.isBlank(dialectType)) {
                throw new IllegalArgumentException("Query dialect missing.");
            }
        }

        return SqlDialectRegister.findOrCreate(dialectType, classLoader);
    }
}

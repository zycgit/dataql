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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.config.DqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.ResultSetType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;

/**
 * 负责一般SQL调用的执行器
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-07-20
 */
public class StatementExecute extends AbstractStatementExecute {
    public StatementExecute(QueryContext context) {
        super(context);
    }

    @Override
    protected void doCheck(Connection conn, Hints hints, SqlConfig config, Map<String, Object> data, Page pageInfo) throws SQLException {
        super.doCheck(conn, hints, config, data, pageInfo);
        if (ExecuteHelper.usingPage(pageInfo)) {
            throw new UnsupportedOperationException("STATEMENT does not support paging query, please using PREPARED.");
        }
    }

    @Override
    protected Statement createStatement(Connection conn, SqlConfig config, BoundSql execSql) throws SQLException {
        if (config instanceof DqlConfig) {
            ResultSetType resultSetType = ((DqlConfig) config).getResultSetType();
            if (resultSetType == null || resultSetType == ResultSetType.DEFAULT) {
                return conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            } else {
                int resultSetTypeInt = resultSetType.getResultSetType();
                return conn.createStatement(resultSetTypeInt, ResultSet.CONCUR_READ_ONLY);
            }
        } else {
            return conn.createStatement();
        }
    }

    @Override
    protected boolean executeQuery(Statement stat, SqlConfig config, BoundSql execSql) throws SQLException {
        try {
            return stat.execute(execSql.getSqlString());
        } catch (SQLException e) {
            logger.error("executeQuery failed, " + ExecuteHelper.fmtBoundSql(execSql), e);
            throw e;
        }
    }
}
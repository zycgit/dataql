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
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.dialect.SqlArg;
import net.hasor.dataql.sqlproc.dialect.SqlMode;
import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.ResultSetType;
import net.hasor.dataql.sqlproc.repository.ognl.OgnlUtils;
import net.hasor.dataql.sqlproc.types.TypeHandler;

import java.sql.*;
import java.util.List;

/**
 * 负责存储过程调用的执行器
 * @version : 2021-07-20
 * @author 赵永春 (zyc@hasor.net)
 */
public class CallableStatementExecute extends AbstractStatementExecute<Object> {
    public CallableStatementExecute(DynamicContext context) {
        super(context);
    }

    protected CallableStatement createCallableStatement(Connection conn, String queryString, ResultSetType resultSetType) throws SQLException {
        if (resultSetType == null || resultSetType.getResultSetType() == null) {
            return conn.prepareCall(queryString);
        } else {
            int resultSetTypeInt = resultSetType.getResultSetType();
            return conn.prepareCall(queryString, resultSetTypeInt, ResultSet.CONCUR_READ_ONLY);
        }
    }

    @Override
    protected Object executeQuery(Connection con, ExecuteInfo info, BoundSqlBuilder boundSql) throws SQLException {
        if (!con.getMetaData().supportsStoredProcedures()) {
            throw new UnsupportedOperationException("procedure DataSource Unsupported.");
        }
        if (usingPage(info)) {
            throw new UnsupportedOperationException("procedure does not support page query.");
        }

        String querySQL = boundSql.getSqlString();
        try (CallableStatement ps = createCallableStatement(con, querySQL, info.resultSetType)) {
            configStatement(info, ps);
            return executeQuery(ps, info, boundSql);
        } catch (SQLException e) {
            logger.error("executeQuery failed, " + fmtBoundSql(boundSql, info.data), e);
            throw e;
        }
    }

    protected Object executeQuery(CallableStatement cs, ExecuteInfo info, BoundSqlBuilder queryBuilder) throws SQLException {
        List<SqlArg> sqlArgs = toArgs(queryBuilder);

        for (int i = 0; i < sqlArgs.size(); i++) {
            int sqlColIndex = i + 1;
            SqlArg arg = sqlArgs.get(i);
            TypeHandler<Object> typeHandler = (TypeHandler<Object>) arg.getTypeHandler();

            switch (arg.getSqlMode()) {
                case In: {
                    typeHandler.setParameter(cs, sqlColIndex, arg.getValue(), arg.getJdbcType());
                    break;
                }
                case InOut: {
                    typeHandler.setParameter(cs, sqlColIndex, arg.getValue(), arg.getJdbcType());
                    cs.registerOutParameter(sqlColIndex, arg.getJdbcType());
                    break;
                }
                case Out: {
                    cs.registerOutParameter(sqlColIndex, arg.getJdbcType());
                    break;
                }
            }
        }

        // execute call
        boolean retVal = cs.execute();

        // fetch output
        for (int i = 0; i < sqlArgs.size(); i++) {
            SqlArg arg = sqlArgs.get(i);
            TypeHandler<Object> argHandler = (TypeHandler<Object>) arg.getTypeHandler();

            if (arg.getSqlMode() != SqlMode.Out) {
                continue;
            }

            if (arg.getJdbcType() == Types.REF_CURSOR) {
                throw new UnsupportedOperationException("Types.REF_CURSOR Unsupported.");
            }

            String expr = arg.getExpr();
            Object resultValue = argHandler.getResult(cs, i + 1);
            OgnlUtils.writeByExpr(expr, info.data, resultValue);
        }

        // result
        ResultTableExtractor extractor = super.buildExtractor(info);
        List<Object> resultSet = extractor.doResult(retVal, cs);
        return getResult(resultSet, info);
    }
}

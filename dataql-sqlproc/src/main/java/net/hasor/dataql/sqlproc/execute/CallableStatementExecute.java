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
import java.sql.*;
import java.util.Map;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.ColumnCaseType;
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlArg;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.SqlMode;
import net.hasor.dataql.sqlproc.dynamic.config.DqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.ResultSetType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

/**
 * 负责存储过程调用的执行器
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-07-20
 */
public class CallableStatementExecute extends AbstractStatementExecute {
    public CallableStatementExecute(QueryContext context) {
        super(context);
    }

    @Override
    protected void doCheck(Connection conn, Hints hints, SqlConfig config, Map<String, Object> data, Page pageInfo) throws SQLException {
        super.doCheck(conn, hints, config, data, pageInfo);
        if (!conn.getMetaData().supportsStoredProcedures()) {
            throw new UnsupportedOperationException("procedure DataSource Unsupported.");
        }
        if (ExecuteHelper.usingPage(pageInfo)) {
            throw new UnsupportedOperationException("CALLABLE does not support paging query, please using PREPARED.");
        }
    }

    @Override
    protected CallableStatement createStatement(Connection conn, SqlConfig config, BoundSql execSql) throws SQLException {
        if (config instanceof DqlConfig) {
            ResultSetType resultSetType = ((DqlConfig) config).getResultSetType();
            if (resultSetType == null || resultSetType == ResultSetType.DEFAULT) {
                return conn.prepareCall(execSql.getSqlString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            } else {
                int resultSetTypeInt = resultSetType.getResultSetType();
                return conn.prepareCall(execSql.getSqlString(), resultSetTypeInt, ResultSet.CONCUR_READ_ONLY);
            }
        } else {
            return conn.prepareCall(execSql.getSqlString());
        }
    }

    @Override
    protected boolean executeQuery(Statement stat, SqlConfig config, BoundSql execSql) throws SQLException {
        try {
            CallableStatement cs = (CallableStatement) stat;
            Object[] args = execSql.getArgs();
            for (int j = 0; j < args.length; j++) {
                TypeHandlerRegistry.DEFAULT.setParameterValue(cs, j + 1, args[j]);
            }

            return cs.execute();
        } catch (SQLException e) {
            logger.error("executeQuery failed, " + ExecuteHelper.fmtBoundSql(execSql), e);
            throw e;
        }
    }

    @Override
    protected void fetchMultipleResult(boolean retVal, SqlBuilder oriSql, Statement cs, ColumnCaseType caseType, Map<String, Object> resultMap) throws SQLException {
        Object[] sqlArgs = oriSql.getArgs();
        // fetch output
        for (int i = 1; i <= sqlArgs.length; i++) {
            Object arg = sqlArgs[i - 1];
            if (!(arg instanceof SqlArg)) {
                continue;
            }
            SqlMode sqlMode = ((SqlArg) arg).getSqlMode();
            if (sqlMode == null || !sqlMode.isOut()) {
                continue;
            }

            SqlArg sqlArg = (SqlArg) arg;
            String asName = sqlArg.getAsName();
            String argName = sqlArg.getName();
            String name;
            if (StringUtils.isNotBlank(asName)) {
                name = asName;
            } else if (StringUtils.isNotBlank(argName)) {
                name = argName;
            } else {
                name = "#out-" + i;
            }

            if (sqlArg.getSqlMode() == SqlMode.Cursor) {
                ResultSet rs = (ResultSet) ((CallableStatement) cs).getObject(i);
                Object resultValue = this.extractor.extractData(caseType, rs);
                resultMap.put(name, resultValue);
            } else {
                TypeHandlerRegistry registry = this.context.getTypeRegistry();
                Object resultValue = registry.getParameterValue((CallableStatement) cs, i, sqlArg);
                resultMap.put(name, resultValue);
            }
        }

        super.fetchMultipleResult(retVal, oriSql, cs, caseType, resultMap);
    }
}

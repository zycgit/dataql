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
import net.hasor.cobble.ArrayUtils;
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.Page;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.config.DqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.ExecuteConfig;
import net.hasor.dataql.sqlproc.dynamic.config.ResultSetType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.execute.reader.CallableMultipleResultSetExtractor;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

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
    protected Map<String, Object> multipleResultFetch(SqlBuilder buildSql, Statement stat, boolean retVal) throws SQLException {
        return new StatementCallableMultipleResultSetExtractor(buildSql).fetchResult(retVal, stat);
    }

    protected Object fetchResult(boolean retVal, Statement stat, SqlConfig config, SqlBuilder oriSql, Map<String, Object> ctx, Page oriPageInfo, long newPageCnt, boolean pageResult) throws SQLException {
        String[] bindOut = null;

        if (config instanceof DqlConfig) {
            bindOut = ((DqlConfig) config).getBindOut();
        } else if (config instanceof ExecuteConfig) {
            bindOut = ((ExecuteConfig) config).getBindOut();
        } else {
            bindOut = ArrayUtils.EMPTY_STRING_ARRAY;
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> multipleResult = this.multipleResultFetch(oriSql, stat, retVal);

        if (bindOut.length == 0) {
            return multipleResult;
        }

        for (String argName : bindOut) {
            if (multipleResult.containsKey(argName)) {
                result.put(argName, multipleResult.get(argName));
            } else if (ctx.containsKey(argName)) {
                result.put(argName, ctx.get(argName));
            } else {
                result.put(argName, null);
            }
        }
        return result;
    }

    private static class StatementCallableMultipleResultSetExtractor extends CallableMultipleResultSetExtractor {
        public StatementCallableMultipleResultSetExtractor(SqlBuilder buildSql) {
            super(buildSql);
        }

        public Map<String, Object> fetchResult(boolean retVal, Statement s) throws SQLException {
            try {
                Map<String, Object> resultsMap = createResultsMap();
                this.beforeFetchResult(s, resultsMap);
                this.fetchResult(retVal, s, resultsMap);
                this.afterFetchResult(s, resultsMap);
                return resultsMap;
            } finally {
                this.afterStatement(s);
            }
        }
    }
}

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
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.dialect.PageDialect;
import net.hasor.dataql.sqlproc.dialect.SqlArg;
import net.hasor.dataql.sqlproc.execute.page.PageResult;
import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.ResultSetType;
import net.hasor.dataql.sqlproc.types.TypeHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * 负责参数化SQL调用的执行器
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-07-20
 */
public class PreparedStatementExecute extends AbstractStatementExecute<Object> {
    public PreparedStatementExecute(DynamicContext context) {
        super(context);
    }

    protected PreparedStatement createPreparedStatement(Connection conn, String queryString, ResultSetType resultSetType) throws SQLException {
        if (resultSetType == null || resultSetType.getResultSetType() == null) {
            return conn.prepareStatement(queryString);
        } else {
            int resultSetTypeInt = resultSetType.getResultSetType();
            return conn.prepareStatement(queryString, resultSetTypeInt, ResultSet.CONCUR_READ_ONLY);
        }
    }

    @Override
    protected Object executeQuery(Connection con, ExecuteInfo info, BoundSqlBuilder sqlBuilder) throws SQLException {
        BoundSql boundSql = sqlBuilder;
        long resultCount = 0L;

        // prepare page
        if (info.pageResult) {
            PageDialect dialect = info.pageDialect;
            long position = info.pageInfo.getFirstRecordPosition();
            long pageSize = info.pageInfo.getPageSize();
            boundSql = dialect.pageSql(sqlBuilder, position, pageSize);

            // select count
            resultCount = info.pageInfo.getTotalCount(); // old value
            if (info.pageCount) {
                BoundSql countSql = dialect.countSql(sqlBuilder);
                try (PreparedStatement ps = createPreparedStatement(con, countSql.getSqlString(), info.resultSetType)) {
                    configStatement(info, ps);
                    resultCount = executeCount(ps, countSql);
                    info.pageInfo.setTotalCount(resultCount); // new value
                } catch (SQLException e) {
                    logger.error("executeCount failed, " + ExceptionUtils.getRootCauseMessage(e) + ", " + fmtBoundSql(countSql, info.data), e);
                    throw e;
                }
            }
        }

        // do query
        Object resultData;
        try (PreparedStatement ps = createPreparedStatement(con, boundSql.getSqlString(), info.resultSetType)) {
            configStatement(info, ps);
            resultData = executeQuery(ps, info, boundSql);
            if (!info.pageResult) {
                return resultData;
            }
        } catch (SQLException e) {
            logger.error("executeQuery failed, " + ExceptionUtils.getRootCauseMessage(e) + ", " + fmtBoundSql(boundSql, info.data), e);
            throw e;
        }

        // page result
        List<Object> records = (resultData instanceof List) ? (List<Object>) resultData : Collections.singletonList(resultData);
        PageResult<Object> pageResult = new PageResult<>(info.pageInfo.getPageSize(), resultCount, records);
        pageResult.setPageNumberOffset(info.pageInfo.getPageNumberOffset());
        pageResult.setCurrentPage(info.pageInfo.getCurrentPage());
        return pageResult;
    }

    protected void statementSet(PreparedStatement ps, BoundSql boundSql) throws SQLException {
        List<SqlArg> sqlArgs = toArgs(boundSql);
        for (int i = 0; i < sqlArgs.size(); i++) {
            SqlArg arg = sqlArgs.get(i);
            TypeHandler typeHandler = arg.getTypeHandler();
            typeHandler.setParameter(ps, i + 1, arg.getValue(), arg.getJdbcType());
        }
    }

    protected Object executeQuery(PreparedStatement ps, ExecuteInfo info, BoundSql boundSql) throws SQLException {
        if (logger.isTraceEnabled()) {
            logger.trace(fmtBoundSql(boundSql).toString());
        }

        statementSet(ps, boundSql);
        ResultTableExtractor extractor = super.buildExtractor(info);
        boolean retVal = ps.execute();
        List<Object> result = extractor.doResult(retVal, ps);

        return getResult(result, info);
    }

    protected int executeCount(PreparedStatement ps, BoundSql boundSql) throws SQLException {
        if (logger.isTraceEnabled()) {
            logger.trace(fmtBoundSql(boundSql).toString());
        }

        statementSet(ps, boundSql);
        try (ResultSet resultSet = ps.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return -1;
            }
        }

    }
}

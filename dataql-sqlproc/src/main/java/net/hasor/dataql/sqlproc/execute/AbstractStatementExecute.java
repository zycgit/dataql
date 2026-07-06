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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.hasor.cobble.ArrayUtils;
import net.hasor.cobble.ExceptionUtils;
import net.hasor.cobble.logging.Logger;
import net.hasor.cobble.logging.LoggerFactory;
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.ColumnCaseType;
import net.hasor.dataql.sqlproc.OpenPackageType;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dialect.*;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.config.DmlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.DqlConfig;
import net.hasor.dataql.sqlproc.dynamic.config.ExecuteConfig;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

/**
 * 执行器基类
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-07-20
 */
public abstract class AbstractStatementExecute {
    protected static final Logger             logger = LoggerFactory.getLogger(AbstractStatementExecute.class);
    protected final        QueryContext       context;
    protected final        MapResultExtractor extractor;

    public AbstractStatementExecute(QueryContext context) {
        this.context = context;
        this.extractor = new MapResultExtractor(context.getTypeRegistry());
    }

    protected void doCheck(Connection conn, Hints hints, SqlConfig config, Map<String, Object> data, Page pageInfo) throws SQLException {
        boolean hasOutBind;
        if (config instanceof ExecuteConfig) {
            hasOutBind = ((ExecuteConfig) config).getBindOut().length > 0;
        } else if (config instanceof DqlConfig) {
            hasOutBind = ((DqlConfig) config).getBindOut().length > 0;
        } else {
            hasOutBind = false;
        }

        if (hasOutBind && ExecuteHelper.usingPage(pageInfo)) {
            throw new SQLException("cannot use paging queries when using bindOut.");
        }
    }

    public final Object execute(Connection conn, Hints hints, SqlConfig config, Map<String, Object> data, Page pageInfo, boolean pageResult) throws SQLException {
        this.doCheck(conn, hints, config, data, pageInfo);

        // prepare sql
        MergedMap<String, Object> dataCtx;
        if (data instanceof MergedMap) {
            dataCtx = (MergedMap<String, Object>) data;
        } else {
            dataCtx = new MergedMap<>();
            dataCtx.appendMap(data, true);
        }

        SqlBuilder oriSql = config.buildQuery(dataCtx, this.context);
        BoundSql execSql = oriSql;
        BoundSql countSql = null;

        // prepare page
        long resultCount = 0L;
        if (ExecuteHelper.usingPage(pageInfo)) {
            PageDialect dialect = SqlDialectRegister.findDialect(conn, hints, this.context.getClassLoader());
            long position = pageInfo.getFirstRecordPosition();
            long pageSize = pageInfo.getPageSize();
            execSql = dialect.pageSql(oriSql, position, pageSize);

            if (pageInfo.isRefreshTotalCount() || pageInfo.getTotalCount() <= 0) {
                countSql = dialect.countSql(oriSql);
            }

            resultCount = pageInfo.getTotalCount(); // old value
        }

        // query count
        if (countSql != null && pageResult) {
            try (PreparedStatement stat = conn.prepareStatement(countSql.getSqlString())) {
                if (logger.isTraceEnabled()) {
                    logger.trace(ExecuteHelper.fmtBoundSql(countSql).toString());
                }
                this.configStatement(stat, config);
                resultCount = this.executeCount(stat, countSql.getArgs());
            } catch (SQLException e) {
                logger.error("executeCount failed, " + ExceptionUtils.getRootCauseMessage(e) + ", " + ExecuteHelper.fmtBoundSql(countSql), e);
                throw e;
            }
        }

        // query data
        try (Statement stat = this.createStatement(conn, config, execSql)) {
            if (logger.isTraceEnabled()) {
                logger.trace(ExecuteHelper.fmtBoundSql(execSql).toString());
            }

            this.configStatement(stat, config);

            boolean retVal = this.executeQuery(stat, config, execSql);
            return this.fetchResult(retVal, oriSql, stat, hints, config, dataCtx, pageInfo, resultCount, pageResult);
        } catch (SQLException e) {
            logger.error("executeQuery failed, " + ExceptionUtils.getRootCauseMessage(e) + ", " + ExecuteHelper.fmtBoundSql(countSql), e);
            throw e;
        }
    }

    protected abstract Statement createStatement(Connection conn, SqlConfig config, BoundSql execSql) throws SQLException;

    private void configStatement(Statement stat, SqlConfig config) throws SQLException {
        if (config.getTimeout() > 0) {
            stat.setQueryTimeout(config.getTimeout());
        }
        if (config instanceof DqlConfig && ((DqlConfig) config).getFetchSize() > 0) {
            stat.setFetchSize(((DqlConfig) config).getFetchSize());
        }
    }

    private long executeCount(PreparedStatement cntStat, Object[] args) throws SQLException {
        for (int j = 0; j < args.length; j++) {
            TypeHandlerRegistry.DEFAULT.setParameterValue(cntStat, j + 1, args[j]);
        }

        try (ResultSet resultSet = cntStat.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                return -1;
            }
        }
    }

    protected abstract boolean executeQuery(Statement stat, SqlConfig config, BoundSql execSql) throws SQLException;

    private Object fetchResult(boolean retVal, SqlBuilder oriSql, Statement stat, Hints hints, SqlConfig config, Map<String, Object> ctx, Page oriPageInfo, long newPageCnt, boolean pageResult) throws SQLException {
        String[] bindOut;
        boolean usingMultipleResultFetch = false;
        String caseTypeStr = SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_COLUMN_CASE);
        String openPackageStr = SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE);
        ColumnCaseType caseType = ColumnCaseType.valueOfCode(caseTypeStr);
        OpenPackageType openPackage = OpenPackageType.valueOfCode(openPackageStr);

        if (config instanceof DqlConfig) {
            bindOut = ((DqlConfig) config).getBindOut();
            usingMultipleResultFetch = bindOut.length > 0;
        } else if (config instanceof ExecuteConfig) {
            bindOut = ((ExecuteConfig) config).getBindOut();
            usingMultipleResultFetch = bindOut.length > 0;
        } else {
            bindOut = ArrayUtils.EMPTY_STRING_ARRAY;
        }

        if (usingMultipleResultFetch) {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> tempResult = new HashMap<>();
            this.fetchMultipleResult(retVal, oriSql, stat, caseType, openPackage, tempResult);
            for (String argName : bindOut) {
                if (tempResult.containsKey(argName)) {
                    result.put(argName, tempResult.get(argName));
                } else if (ctx.containsKey(argName)) {
                    result.put(argName, ctx.get(argName));
                } else {
                    result.put(argName, null);
                }
            }
            return result;
        } else {
            if (config instanceof DmlConfig) {
                return stat.getUpdateCount();
            }

            if (retVal) {
                try (ResultSet rs = stat.getResultSet()) {
                    if (rs.isLast()) {
                        return Collections.emptyList();
                    }

                    List<Map<String, Object>> objects = this.extractor.extractData(caseType, rs);
                    if (pageResult) {
                        PageResult<?> page = new PageResult<>(oriPageInfo, objects);
                        page.setTotalCount(newPageCnt);
                        return page;
                    } else {
                        return this.convertResult(openPackage, objects);
                    }
                }
            } else {
                return stat.getUpdateCount();
            }
        }
    }

    protected Object convertResult(OpenPackageType openPackage, List<Map<String, Object>> mapList) {
        if (openPackage == OpenPackageType.Off || (mapList != null && mapList.size() > 1)) {
            return mapList;
        }
        if (mapList == null || mapList.isEmpty()) {
            return openPackage == OpenPackageType.Column ? null : Collections.emptyMap();
        }

        Map<String, Object> rowObject = mapList.get(0);
        if (openPackage == OpenPackageType.Column && rowObject != null && rowObject.size() == 1) {
            return rowObject.values().iterator().next();
        }

        return rowObject;
    }

    protected void fetchMultipleResult(boolean retVal, SqlBuilder oriSql, Statement cs, ColumnCaseType caseType, OpenPackageType openPackage, Map<String, Object> resultMap) throws SQLException {
        // fetch ResultSet -- first ResultSet
        int resultIndex = 1;
        String resultName;
        Object resultValue;
        if (retVal) {
            try (ResultSet rs = cs.getResultSet()) {
                resultName = "#result-set-" + resultIndex;
                resultValue = this.convertResult(openPackage, this.extractor.extractData(caseType, rs));
            }
        } else {
            resultName = "#update-count-" + resultIndex;
            resultValue = cs.getUpdateCount();
        }

        // fetch ResultSet -- more ResultSet
        resultMap.put(resultName, resultValue);
        while ((cs.getMoreResults()) || (cs.getUpdateCount() != -1)) {
            resultIndex++;
            int updateCount = cs.getUpdateCount();
            if (updateCount == -1) {
                resultName = "#result-set-" + resultIndex;
                try (ResultSet rs = cs.getResultSet()) {
                    resultValue = this.convertResult(openPackage, this.extractor.extractData(caseType, rs));
                }
            } else {
                resultName = "#update-count-" + resultIndex;
                resultValue = updateCount;
            }
            resultMap.put(resultName, resultValue);
        }
    }
}

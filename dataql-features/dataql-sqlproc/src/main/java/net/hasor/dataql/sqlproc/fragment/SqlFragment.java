/*
 * Copyright 2008-2009 the original author or authors.
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
package net.hasor.dataql.sqlproc.fragment;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.codec.MD5;
import net.hasor.cobble.io.IOUtils;
import net.hasor.cobble.setting.SettingNode;
import net.hasor.cobble.setting.data.TreeNode;
import net.hasor.core.AppContext;
import net.hasor.core.BindInfo;
import net.hasor.core.Inject;
import net.hasor.core.Singleton;
import net.hasor.core.spi.SpiTrigger;
import net.hasor.dataql.FragmentProcess;
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.MultipleResultsType;
import net.hasor.dataql.sqlproc.repository.ProcSqlParser;
import net.hasor.dataql.sqlproc.repository.config.AbstractProcSql;
import net.hasor.dataql.sqlproc.repository.config.QueryProcSql;
import net.hasor.dataql.sqlproc.spi.LookupConnectionListener;
import net.hasor.dataql.sqlproc.spi.LookupDataSourceListener;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.hasor.dataql.sqlproc.SqlHintNames.FRAGMENT_SQL_DATA_SOURCE;
import static net.hasor.dataql.sqlproc.SqlHintNames.FRAGMENT_SQL_MULTIPLE_QUERIES;

/**
 * 支持 SQL 的代码片段执行器。整合了分页、批处理能力。
 * 已支持的语句有：insert、update、delete、replace、select、create、drop、alter
 * 已经提供原生：insert、update、delete、replace 语句的批量能力。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-03-28
 */
@Singleton
public class SqlFragment implements FragmentProcess, DynamicContext {
    @Inject
    protected AppContext                   appContext;
    @Inject
    protected SpiTrigger                   spiTrigger;
    private   DataSource                   defaultDataSource;
    private   Map<String, DataSource>      dataSourceMap;
    private   Map<String, AbstractProcSql> procDynamicCache;

    @PostConstruct
    public void init() {
        this.dataSourceMap = new ConcurrentHashMap<>();
        this.procDynamicCache = new ConcurrentHashMap<>();
        List<BindInfo<DataSource>> bindInfos = this.appContext.findBindingRegister(DataSource.class);
        for (BindInfo<DataSource> bindInfo : bindInfos) {
            if (StringUtils.isBlank(bindInfo.getBindName())) {
                if (this.defaultDataSource == null) {
                    this.defaultDataSource = this.appContext.getInstance(bindInfo);
                }
            } else {
                DataSource dataSource = this.appContext.getInstance(bindInfo);
                if (dataSource != null) {
                    this.dataSourceMap.put(bindInfo.getBindName(), dataSource);
                }
            }
        }
    }

    /**
     * 尝试推断SQL语句类型
     * - Query 可以执行分页，其它类型语句会退化。
     * - Insert/Update/Delete 语句之外的：退化为 非批量
     */
    private static QueryType evalSqlMode(String fragmentString) throws IOException {
        List<String> readLines = IOUtils.readLines(new StringReader(fragmentString));
        boolean multipleLines = false;
        for (String lineStr : readLines) {
            String tempLine = lineStr.trim();
            if (!multipleLines) {
                // 空行
                if (StringUtils.isBlank(tempLine)) {
                    continue;
                }
                // 单行注释
                if (tempLine.startsWith("--") || tempLine.startsWith("//")) {
                    continue;
                }
                // 多行注释
                if (tempLine.startsWith("/*")) {
                    if (tempLine.contains("*/")) {
                        tempLine = tempLine.substring(tempLine.indexOf("*/") + 2).trim();// 使用多行注释定义了一个单行注释
                    }
                    if (StringUtils.isBlank(tempLine)) {
                        continue;
                    }
                    multipleLines = true;
                }
            }
            if (multipleLines) {
                if (tempLine.contains("*/")) {
                    tempLine = tempLine.substring(tempLine.indexOf("*/")).trim();
                    multipleLines = false;
                } else {
                    continue;
                }
            }

            tempLine = tempLine.toLowerCase();
            if (tempLine.startsWith("insert") || tempLine.startsWith("replace")) {
                return QueryType.Insert;
            } else if (tempLine.startsWith("update")) {
                return QueryType.Update;
            } else if (tempLine.startsWith("merge")) {
                return QueryType.Merge;
            } else if (tempLine.startsWith("delete")) {
                return QueryType.Delete;
            } else if (tempLine.startsWith("exec")) {
                return QueryType.Call;
            } else if (tempLine.startsWith("select") || tempLine.startsWith("with")) {
                return QueryType.Query;
            } else if (tempLine.startsWith("create")) {
                return QueryType.Create;
            } else if (tempLine.startsWith("drop")) {
                return QueryType.Drop;
            } else if (tempLine.startsWith("alter")) {
                return QueryType.Alter;
            }
        }
        return QueryType.Other;
    }

    protected Connection fetchConnection(Hints hint) throws SQLException {
        String sourceName = hint.getOrDefault(FRAGMENT_SQL_DATA_SOURCE.name(), "").toString();

        // .首先尝试 Connection
        if (this.spiTrigger.hasSpi(LookupConnectionListener.class)) {
            // .通过 SPI 查找数据源
            Connection jdbcConnection = this.spiTrigger.notifySpi(LookupConnectionListener.class, (listener, lastResult) -> {
                return listener.lookUp(sourceName);
            }, null);
            // .构造JdbcTemplate
            if (jdbcConnection != null) {
                return jdbcConnection;
            }
        }

        // .其次在通过数据源获取
        DataSource useDataSource = null;
        if (StringUtils.isBlank(sourceName)) {
            useDataSource = this.defaultDataSource;
        } else {
            useDataSource = this.dataSourceMap.get(sourceName);
        }
        if (useDataSource == null) {
            if (this.spiTrigger.hasSpi(LookupDataSourceListener.class)) {
                // .通过 SPI 查找数据源
                DataSource dataSource = this.spiTrigger.notifySpi(LookupDataSourceListener.class, (listener, lastResult) -> {
                    return listener.lookUp(sourceName);
                }, null);
                // .构造JdbcTemplate
                if (dataSource != null) {
                    return dataSource.getConnection();
                }
            }
            throw new NullPointerException("DataSource " + sourceName + " is undefined.");
        }

        return useDataSource.getConnection();
    }

    protected AbstractProcSql buildOrGetProc(String fragmentString, Hints hint) throws Exception {
        StringBuffer evalMd5Key = new StringBuffer(fragmentString);
        hint.forEach((key, value) -> evalMd5Key.append(key).append("=").append(value));
        String dynamicId = MD5.getMD5(evalMd5Key.toString());

        AbstractProcSql execute = this.procDynamicCache.get(dynamicId);
        if (execute == null) {
            synchronized (this) {
                execute = this.procDynamicCache.get(dynamicId);
                if (execute == null) {
                    execute = initProc(dynamicId, fragmentString, hint);
                    this.procDynamicCache.put(dynamicId, execute);
                }
            }
        }

        return execute;
    }

    private AbstractProcSql initProc(String dynamicId, String fragmentString, Hints hint) throws Exception {
        SettingNode options = new TreeNode();
        hint.forEach((key, value) -> options.addValue(key, value.toString()));

        String multipleResultStr = hint.getOrDefault(FRAGMENT_SQL_MULTIPLE_QUERIES.name(), MultipleResultsType.LAST.getTypeName()).toString();
        MultipleResultsType multipleResult = MultipleResultsType.valueOfCode(multipleResultStr, MultipleResultsType.LAST);
        options.setValue("multipleResult", multipleResult.getTypeName());

        String wrapQueryBody = "<script id='" + dynamicId + "'>" + fragmentString + "</script>";
        QueryProcSql dynamic = new ProcSqlParser().parseDynamicSql(wrapQueryBody, hint);

        if (evalSqlMode(fragmentString) == QueryType.Call) {
            options.setValue("multipleResult", multipleResult.getTypeName());
        }

        return dynamic;
    }

    //    public List<Object> batchRunFragment(Hints hint, List<Map<String, Object>> params, String fragmentString) throws Throwable {
    //        // 如果批量参数为空退：退化为 非批量
    //        if (params == null || params.size() == 0) {
    //            return Collections.singletonList(this.runFragment(hint, Collections.emptyMap(), fragmentString));
    //        }
    //
    //        // 批量参数只有一组：退化为 非批量
    //        if (params.size() == 1) {
    //            return Collections.singletonList(this.runFragment(hint, params.get(0), fragmentString));
    //        }
    //
    //        // 确定是否支持批量模式
    //        AbstractProcSql procSql = buildOrGetProc(fragmentString, hint);
    //        boolean useBatch = procSql.supportBatch();
    //
    //        // 不支持批模式：一条一条的执行
    //        if (!useBatch) {
    //            List<Object> resultList = new ArrayList<>(params.size());
    //            for (Map<String, Object> paramItem : params) {
    //                Object result = this.runFragment(hint, paramItem, fragmentString);
    //                resultList.add(result);
    //            }
    //            return resultList;
    //        }
    //
    //        // 批量执行
    //        return doRunByBatch(hint, procSql, params);
    //    }
    //
    //    @Override
    //    public Object runFragment(Hints hint, Map<String, Object> paramMap, String fragmentString) throws Throwable {
    //        AbstractProcSql procSql = buildOrGetProc(fragmentString, hint);
    //        return doRunByOne(hint, procSql, paramMap);
    //    }
    //
    //    // --------------------------------------------------------------------------------------------
    //
    //    protected Object doRunByOne(Hints hint, AbstractProcSql procSql, Map<String, Object> params) throws Throwable {
    //        if (usePage(hint, procSql)) {
    //            Page pageInfo = new PageObject(1, new ESupplier<Integer, SQLException>() {
    //                @Override
    //                public Integer eGet() throws SQLException {
    //                    return null;
    //                }
    //            });
    //
    //            Object pageOffset = hint.getOrDefault(FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET.name(), FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET.getDefaultVal());
    //            pageInfo.setPageNumberOffset(Integer.parseInt(pageOffset.toString()));
    //            return new SqlPageObject(hint, procSql, params, pageInfo, getDialect(hint), this);
    //        } else {
    //
    //            procSql.execute()
    //        }
    //    }
    //
    //    protected List<Object> doRunByBatch(Hints hint, AbstractProcSql procSql, List<Map<String, Object>> params) throws Throwable {
    //        if (usePage(hint, procSql)) {
    //            throw new UnsupportedOperationException("batch not support page.");
    //        }
    //
    //        return null;
    //    }
    //
    //    private boolean usePage(Hints hint, AbstractProcSql procSql) {
    //        if (procSql.supportPage() && !procSql.supportBatch()) {
    //            SqlHintNames queryByPage = SqlHintNames.FRAGMENT_SQL_QUERY_BY_PAGE;
    //            Object hintOrDefault = hint.getOrDefault(queryByPage.name(), queryByPage.getDefaultVal());
    //            return FRAGMENT_SQL_QUERY_BY_PAGE_ENABLE.equalsIgnoreCase(hintOrDefault.toString());
    //        } else {
    //            return false;
    //        }
    //    }
    //
    //    //
    //    //
    //    //
    //    //
    //    //
    //
    //    public boolean supportBatch() {
    //        if (this.target.isHavePlaceholder()) {
    //            // 分析SQL后如果含有占位符：退化为 非批量（占位符会导致每次执行的SQL语句可能不一样）
    //            return false;
    //        } else {
    //            // 只有 Insert/Update/Delete 支持批量
    //            return (QueryType.Insert == this.queryType || QueryType.Update == this.queryType || QueryType.Delete == this.queryType);
    //        }
    //    }
    //
    //    public boolean supportPage() {
    //        return this.queryType == QueryType.Query;
    //    }
    //
    //    /** 执行 SQL */
    //    protected <T> T executeSQL(boolean batch, String sourceName, String sqlString, Object[] paramArrays, SqlQuery<T> sqlQuery) throws SQLException {
    //        if (this.spiTrigger.hasSpi(FxSqlCheckChainSpi.class)) {
    //            final FxSqlInfo fxSqlInfo = new FxSqlInfo(batch, sourceName, sqlString, paramArrays);
    //            final AtomicBoolean doExit = new AtomicBoolean(false);
    //            this.spiTrigger.chainSpi(FxSqlCheckChainSpi.class, (listener, lastResult) -> {
    //                if (doExit.get()) {
    //                    return lastResult;
    //                }
    //                int doCheck = listener.doCheck(fxSqlInfo);
    //                if (doCheck == FxSqlCheckChainSpi.EXIT) {
    //                    doExit.set(true);
    //                }
    //                return lastResult;
    //            }, fxSqlInfo);
    //            //
    //            return sqlQuery.doQuery(fxSqlInfo.getQueryString(), fxSqlInfo.getQueryParams(), this.getJdbcTemplate(sourceName));
    //        } else {
    //            return sqlQuery.doQuery(sqlString, paramArrays, this.getJdbcTemplate(sourceName));
    //        }
    //    }
    //
    //    @Override
    //    public Class<?> loadClass(String name) throws ClassNotFoundException {
    //        return null;
    //    }
    //
    //    @Override
    //    public DynamicSql findDynamic(String dynamicId) {
    //        return null;
    //    }
}

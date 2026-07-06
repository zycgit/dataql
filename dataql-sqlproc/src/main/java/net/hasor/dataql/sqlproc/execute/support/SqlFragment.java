/// *
// * Copyright 2008-2009 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package net.hasor.dataql.sqlproc.support;
//import static net.hasor.dataql.sqlproc.SqlHintNames.*;
//import static net.hasor.dataql.sqlproc.SqlHintValue.FRAGMENT_SQL_QUERY_BY_PAGE_ENABLE;
//
//import java.io.IOException;
//import java.io.StringReader;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import javax.annotation.PostConstruct;
//import javax.sql.DataSource;
//
//import net.hasor.cobble.StringUtils;
//import net.hasor.cobble.codec.MD5;
//import net.hasor.cobble.io.IOUtils;
//import net.hasor.core.AppContext;
//import net.hasor.core.BindInfo;
//import net.hasor.core.Inject;
//import net.hasor.core.Singleton;
//import net.hasor.core.spi.SpiTrigger;
//import net.hasor.dataql.FragmentProcess;
//import net.hasor.dataql.Hints;
//import net.hasor.dataql.runtime.HintsSet;
//import net.hasor.dataql.sqlproc.JdbcHelper;
//import net.hasor.dataql.sqlproc.SqlHintNames;
//import net.hasor.dataql.sqlproc.dialect.PageDialect;
//import net.hasor.dataql.sqlproc.dialect.SqlDialectRegister;
//import net.hasor.dataql.sqlproc.execute.ExecuteProxy;
//import net.hasor.dataql.sqlproc.fragment.QueryType;
//import net.hasor.dataql.sqlproc.repository.config.QueryProcSql;
//import net.hasor.dataql.sqlproc.spi.LookupConnectionListener;
//import net.hasor.dataql.sqlproc.spi.LookupDataSourceListener;
//
/// **
// * 支持 SQL 的代码片段执行器。整合了分页、批处理能力。
// * 已支持的语句有：insert、update、delete、replace、select、create、drop、alter
// * 已经提供原生：insert、update、delete、replace 语句的批量能力。
// * @author 赵永春 (zyc@hasor.net)
// * @version : 2020-03-28
// */
//@Singleton
//public class SqlFragment implements FragmentProcess, DynamicContext {
//    @Inject
//    protected     AppContext                appContext;
//    @Inject
//    protected     SpiTrigger                spiTrigger;
//    private       DataSource                defaultDataSource;
//    private       Map<String, DataSource>   dataSourceMap;
//    private       Map<String, ExecuteProxy> procDynamicCache;
//    private final ProcSqlParser             sqlParser = new ProcSqlParser();
//
//    @PostConstruct
//    public void init() {
//        this.dataSourceMap = new ConcurrentHashMap<>();
//        this.procDynamicCache = new ConcurrentHashMap<>();
//        List<BindInfo<DataSource>> bindInfos = this.appContext.findBindingRegister(DataSource.class);
//        for (BindInfo<DataSource> bindInfo : bindInfos) {
//            if (StringUtils.isBlank(bindInfo.getBindName())) {
//                if (this.defaultDataSource == null) {
//                    this.defaultDataSource = this.appContext.getInstance(bindInfo);
//                }
//            } else {
//                DataSource dataSource = this.appContext.getInstance(bindInfo);
//                if (dataSource != null) {
//                    this.dataSourceMap.put(bindInfo.getBindName(), dataSource);
//                }
//            }
//        }
//    }
//
//    @Override
//    public Class<?> loadClass(String name) throws ClassNotFoundException {
//        return this.appContext.getClassLoader().loadClass(name);
//    }
//
//    /** 尝试推断SQL语句类型 */
//    private static QueryType evalSqlMode(String fragmentString) throws IOException {
//        List<String> readLines = IOUtils.readLines(new StringReader(fragmentString));
//        boolean multipleLines = false;
//        for (String lineStr : readLines) {
//            String tempLine = lineStr.trim();
//            if (!multipleLines) {
//                // 空行
//                if (StringUtils.isBlank(tempLine)) {
//                    continue;
//                }
//                // 单行注释
//                if (tempLine.startsWith("--") || tempLine.startsWith("//")) {
//                    continue;
//                }
//                // 多行注释
//                if (tempLine.startsWith("/*")) {
//                    if (tempLine.contains("*/")) {
//                        tempLine = tempLine.substring(tempLine.indexOf("*/") + 2).trim();// 使用多行注释定义了一个单行注释
//                    }
//                    if (StringUtils.isBlank(tempLine)) {
//                        continue;
//                    }
//                    multipleLines = true;
//                }
//            }
//            if (multipleLines) {
//                if (tempLine.contains("*/")) {
//                    tempLine = tempLine.substring(tempLine.indexOf("*/")).trim();
//                    multipleLines = false;
//                } else {
//                    continue;
//                }
//            }
//
//            tempLine = tempLine.toLowerCase();
//            if (tempLine.startsWith("insert") || tempLine.startsWith("replace")) {
//                return QueryType.Insert;
//            } else if (tempLine.startsWith("update")) {
//                return QueryType.Update;
//            } else if (tempLine.startsWith("delete")) {
//                return QueryType.Delete;
//            } else if (tempLine.startsWith("merge")) {
//                return QueryType.Merge;
//            } else if (tempLine.startsWith("exec")) {
//                return QueryType.Call;
//            } else if (tempLine.startsWith("select") || tempLine.startsWith("with")) {
//                return QueryType.Query;
//            } else if (tempLine.startsWith("create")) {
//                return QueryType.Create;
//            } else if (tempLine.startsWith("drop")) {
//                return QueryType.Drop;
//            } else if (tempLine.startsWith("alter")) {
//                return QueryType.Alter;
//            }
//        }
//        return QueryType.Other;
//    }
//
//    protected ExecuteProxy buildOrGetExecute(String fragmentString, Hints hints) throws Exception {
//        StringBuffer evalMd5Key = new StringBuffer(fragmentString);
//        hints.forEach((key, value) -> evalMd5Key.append(key).append("=").append(value));
//        String dynamicId = MD5.getMD5(evalMd5Key.toString());
//
//        ExecuteProxy execute = this.procDynamicCache.get(dynamicId);
//        if (execute == null) {
//            synchronized (this) {
//                execute = this.procDynamicCache.get(dynamicId);
//                if (execute == null) {
//                    execute = initExecute(fragmentString, hints);
//                    this.procDynamicCache.put(dynamicId, execute);
//                }
//            }
//        }
//
//        return execute;
//    }
//
//    private ExecuteProxy initExecute(String fragmentString, Hints hints) throws Exception {
//        HintsSet procArgs = new HintsSet(hints);
//
//        // multipleResult
//        String multipleResultStr = hints.getHint(FRAGMENT_SQL_MULTIPLE_QUERIES.name()).toString();
//        MultipleResultsType multiple = MultipleResultsType.valueOfCode(multipleResultStr, MultipleResultsType.LAST);
//        procArgs.setHint("multipleResult", multiple.getValue());
//
//        // statementType
//        QueryType queryType = evalSqlMode(fragmentString);
//        if (queryType == QueryType.Call) {
//            procArgs.setHint("statementType", StatementType.Callable.getValue());
//        }
//
//        // fetchSize
//        // useGeneratedKeys
//        // keyProperty
//        // timeout
//        QueryProcSql querySql = this.sqlParser.parseDynamicSql(fragmentString, procArgs);
//        return new ExecuteProxy(queryType, querySql, this);
//    }
//
//    @Override
//    public DynamicSql findDynamic(String dynamicId) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public List<Object> batchRunFragment(Hints hints, List<Map<String, Object>> params, String fragmentString) throws Throwable {
//        // 获取 Execute
//        ExecuteProxy execute = buildOrGetExecute(fragmentString, hints);
//
//        // use Page
//        SqlHintNames queryByPage = SqlHintNames.FRAGMENT_SQL_QUERY_BY_PAGE;
//        Object hintOrDefault = hints.getOrDefault(queryByPage.name(), queryByPage.getDefaultVal());
//        boolean usePage = FRAGMENT_SQL_QUERY_BY_PAGE_ENABLE.equalsIgnoreCase(hintOrDefault.toString());
//
//        // 如果批量参数为空退：退化为 非批量
//        if (params == null || params.size() == 0) {
//            return Collections.singletonList(this.doRunByOne(hints, execute, Collections.emptyMap(), usePage));
//        }
//
//        // 批量参数只有一组：退化为 非批量
//        if (params.size() == 1) {
//            return Collections.singletonList(this.doRunByOne(hints, execute, params.get(0), usePage));
//        }
//
//        // 确定是否支持批量模式
//        //    - isHavePlaceholder 如果含有注入 SQL 退化为非批量
//        //    - isDynamic 如果含有动态成分，例如：bind/if/foreach/choose 等标签 退化为非批量
//        //    - isHavePlaceholder 如果含有占位符：退化为 非批量（占位符会导致每次执行的SQL语句可能不一样）
//        //    - 只有 Insert/Update/Delete 支持批量
//        QueryType queryType = execute.getQueryType();
//        boolean useBatch = !execute.isHavePlaceholder() && !execute.isDynamic();
//        boolean isIUD = QueryType.Insert == queryType || QueryType.Update == queryType || QueryType.Delete == queryType;
//
//        // 不支持批模式：一条一条的执行
//        if (!useBatch || !isIUD || usePage) {
//            List<Object> resultList = new ArrayList<>(params.size());
//            for (Map<String, Object> paramItem : params) {
//                resultList.add(this.doRunByOne(hints, execute, paramItem, usePage));
//            }
//            return resultList;
//        }
//
//        // 批量执行
//        return doRunByBatch(hints, execute, params);
//    }
//
//    @Override
//    public Object runFragment(Hints hints, Map<String, Object> paramMap, String fragmentString) throws Throwable {
//        // 获取 Execute
//        ExecuteProxy execute = buildOrGetExecute(fragmentString, hints);
//
//        // use Page
//        SqlHintNames queryByPage = SqlHintNames.FRAGMENT_SQL_QUERY_BY_PAGE;
//        Object hintOrDefault = hints.getOrDefault(queryByPage.name(), queryByPage.getDefaultVal());
//        boolean usePage = FRAGMENT_SQL_QUERY_BY_PAGE_ENABLE.equalsIgnoreCase(hintOrDefault.toString());
//
//        return doRunByOne(hints, execute, paramMap, usePage);
//    }
//
//    // --------------------------------------------------------------------------------------------
//    protected Connection fetchConnection(Hints hints) throws SQLException {
//        String sourceName = hints.getOrDefault(FRAGMENT_SQL_DATA_SOURCE.name(), "").toString();
//
//        // .首先尝试 Connection
//        if (this.spiTrigger.hasSpi(LookupConnectionListener.class)) {
//            // .通过 SPI 查找数据源
//            Connection jdbcConnection = this.spiTrigger.notifySpi(LookupConnectionListener.class, (listener, lastResult) -> {
//                return listener.lookUp(sourceName);
//            }, null);
//            // .构造JdbcTemplate
//            if (jdbcConnection != null) {
//                return jdbcConnection;
//            }
//        }
//
//        // .其次在通过数据源获取
//        DataSource useDataSource = null;
//        if (StringUtils.isBlank(sourceName)) {
//            useDataSource = this.defaultDataSource;
//        } else {
//            useDataSource = this.dataSourceMap.get(sourceName);
//        }
//        if (useDataSource == null) {
//            if (this.spiTrigger.hasSpi(LookupDataSourceListener.class)) {
//                // .通过 SPI 查找数据源
//                DataSource dataSource = this.spiTrigger.notifySpi(LookupDataSourceListener.class, (listener, lastResult) -> {
//                    return listener.lookUp(sourceName);
//                }, null);
//                // .构造JdbcTemplate
//                if (dataSource != null) {
//                    return dataSource.getConnection();
//                }
//            }
//            throw new NullPointerException("DataSource " + sourceName + " is undefined.");
//        }
//
//        return useDataSource.getConnection();
//    }
//
//    protected PageDialect fetchDialect(Hints hints, Connection con) throws SQLException {
//        // .优先从 hint 中取方言，取不到在自动推断
//        String sqlDialect = hints.getOrDefault(FRAGMENT_SQL_PAGE_DIALECT.name(), "").toString();
//        if (StringUtils.isBlank(sqlDialect)) {
//            String jdbcUrl = con.getMetaData().getURL();
//            String jdbcDriverName = con.getMetaData().getDriverName();
//            sqlDialect = JdbcHelper.getDbType(jdbcUrl, jdbcDriverName);
//
//            if (StringUtils.isBlank(sqlDialect)) {
//                throw new IllegalArgumentException("Query dialect missing.");
//            }
//        }
//
//        return SqlDialectRegister.findOrCreate(sqlDialect, classType -> appContext.getInstance(classType));
//    }
//
//    protected List<Object> doRunByBatch(Hints hints, ExecuteProxy execute, List<Map<String, Object>> params) throws Throwable {
//        // 1. 不是分页
//        // 2. 只有 IUD
//        // 3. 没有注入代码
//        // 4. 没有动态成分
//        //execute.execute()
//        throw new UnsupportedOperationException(); // TODO
//    }
//
//    protected Object doRunByOne(Hints hints, ExecuteProxy execute, Map<String, Object> params, boolean usePage) throws Throwable {
//        if (usePage) {
//            return new LazyPageQuery(hints, execute, params, this);
//        }
//
//        try (Connection con = fetchConnection(hints)) {
//            PageDialect dialect = fetchDialect(hints, con);
//            return execute.execute(con, params, hints, false, false, null, dialect);
//        }
//    }
//}
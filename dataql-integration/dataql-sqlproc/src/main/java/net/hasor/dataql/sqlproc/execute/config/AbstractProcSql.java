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
package net.hasor.dataql.sqlproc.execute.config;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.setting.SettingNode;
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.DynamicContext;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.execute.MultipleResultsType;
import net.hasor.dataql.sqlproc.execute.ResultSetType;
import net.hasor.dataql.sqlproc.execute.StatementType;

import java.sql.SQLException;
import java.util.Map;

/**
 * Segment SqlConfig
 * @version : 2021-06-19
 * @author 赵永春 (zyc@hasor.net)
 */
public abstract class AbstractProcSql {
    private final   StatementType       statementType;
    private final   int                 timeout;
    private final   String              resultMap;
    private final   int                 fetchSize;
    private final   ResultSetType       resultSetType;
    private final   MultipleResultsType multipleResultType;
    protected final DynamicSql          target;
    private         String              pageDialectHint;

    public AbstractProcSql(DynamicSql target, SettingNode options) {
        String statementType = options != null ? options.findValue("statementType") : null;
        String timeout = options != null ? options.findValue("timeout") : null;
        String resultMap = options != null ? options.findValue("resultMap") : null;
        String fetchSize = options != null ? options.findValue("fetchSize") : null;
        String resultSetType = options != null ? options.findValue("resultSetType") : null;
        String multipleResult = options != null ? options.findValue("multipleResult") : null;

        this.statementType = StatementType.valueOfCode(statementType, StatementType.Prepared);
        this.timeout = StringUtils.isBlank(timeout) ? -1 : Integer.parseInt(timeout);
        this.resultMap = resultMap;
        this.fetchSize = StringUtils.isBlank(fetchSize) ? 256 : Integer.parseInt(fetchSize);
        this.resultSetType = ResultSetType.valueOfCode(resultSetType, ResultSetType.DEFAULT);
        this.multipleResultType = MultipleResultsType.valueOfCode(multipleResult, MultipleResultsType.LAST);
        this.target = target;
    }

    public StatementType getStatementType() {
        return this.statementType;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public String getResultMap() {
        return this.resultMap;
    }

    public int getFetchSize() {
        return this.fetchSize;
    }

    public ResultSetType getResultSetType() {
        return this.resultSetType;
    }

    public MultipleResultsType getMultipleResultType() {
        return this.multipleResultType;
    }

    public boolean isHavePlaceholder() {
        return this.target.isHavePlaceholder();
    }

    public void buildQuery(Map<String, Object> data, DynamicContext context, BoundSqlBuilder sqlBuilder) throws SQLException {
        this.target.buildQuery(data, context, sqlBuilder);
    }
}
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
package net.hasor.dataql.sqlproc.fragment.config;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.setting.SettingNode;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.dynamic.nodes.ArrayDynamicSql;
import net.hasor.dataql.sqlproc.dynamic.nodes.SelectKeyDynamicSql;
import net.hasor.dataql.sqlproc.fragment.QueryType;
import net.hasor.dataql.sqlproc.fragment.ResultSetType;
import net.hasor.dataql.sqlproc.fragment.StatementType;

/**
 * All DML SqlConfig
 * @version : 2021-06-19
 * @author 赵永春 (zyc@hasor.net)
 */
public abstract class DmlProcSql extends AbstractProcSql {
    private StatementType    statementType = StatementType.Prepared;
    private int              timeout       = -1;
    private SelectKeyProcSql selectKey;

    public DmlProcSql(DynamicSql target, SettingNode options) {
        super(target);
        String statementType = options != null ? options.findValue("statementType") : null;
        String timeout = options != null ? options.findValue("timeout") : null;

        this.statementType = StatementType.valueOfCode(statementType, StatementType.Prepared);
        this.timeout = StringUtils.isBlank(timeout) ? -1 : Integer.parseInt(timeout);

        this.processSelectKey(target);
    }

    protected void processSelectKey(DynamicSql target) {
        if (target instanceof ArrayDynamicSql) {
            for (DynamicSql dynamicSql : ((ArrayDynamicSql) target).getSubNodes()) {
                if (dynamicSql instanceof SelectKeyDynamicSql) {
                    SelectKeyDynamicSql skDynamicSql = (SelectKeyDynamicSql) dynamicSql;
                    StatementType skStatementType = StatementType.valueOfCode(skDynamicSql.getStatementType(), StatementType.Prepared);
                    ResultSetType skResultSetType = ResultSetType.valueOfCode(skDynamicSql.getResultSetType(), ResultSetType.DEFAULT);

                    this.selectKey = new SelectKeyProcSql(skDynamicSql);
                    this.selectKey.setStatementType(skStatementType);
                    this.selectKey.setTimeout(skDynamicSql.getTimeout());
                    this.selectKey.setResultMap(skDynamicSql.getResultMap());
                    this.selectKey.setFetchSize(skDynamicSql.getFetchSize());
                    this.selectKey.setResultSetType(skResultSetType);
                    this.selectKey.setKeyProperty(skDynamicSql.getKeyProperty());
                    this.selectKey.setKeyColumn(skDynamicSql.getKeyColumn());
                    this.selectKey.setOrder(skDynamicSql.getOrder());
                    this.selectKey.setHandler(skDynamicSql.getHandler());
                }
            }
        }
    }

    public SelectKeyProcSql getSelectKey() {
        return this.selectKey;
    }

    public abstract QueryType getDynamicType();

    public StatementType getStatementType() {
        return this.statementType;
    }

    public void setStatementType(StatementType statementType) {
        this.statementType = statementType;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}

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
package net.hasor.dataql.sqlproc.repository.config;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.DynamicSql;
import net.hasor.dataql.sqlproc.repository.StatementType;
import net.hasor.dataql.sqlproc.repository.nodes.ArrayDynamicSql;
import net.hasor.dataql.sqlproc.repository.nodes.SelectKeyDynamicSql;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.sql.SQLException;
import java.util.Map;

/**
 * All DML SqlConfig
 * @version : 2021-06-19
 * @author 赵永春 (zyc@hasor.net)
 */
public abstract class AbstractProcSql implements DynamicSql {
    protected final DynamicSql       target;
    private         StatementType    statementType = StatementType.Prepared;
    private         int              timeout       = -1;
    private         SelectKeyProcSql selectKey;

    public AbstractProcSql(DynamicSql target) {
        this.target = target;
        this.processSelectKey(target);
    }

    public AbstractProcSql(DynamicSql target, Node operationNode) {
        this.target = target;
        NamedNodeMap nodeAttributes = operationNode.getAttributes();
        Node statementTypeNode = nodeAttributes.getNamedItem("statementType");
        Node timeoutNode = nodeAttributes.getNamedItem("timeout");
        String statementType = (statementTypeNode != null) ? statementTypeNode.getNodeValue() : null;
        String timeout = (timeoutNode != null) ? timeoutNode.getNodeValue() : null;

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

                    this.selectKey = new SelectKeyProcSql(skDynamicSql);
                    this.selectKey.setStatementType(skStatementType);
                    this.selectKey.setTimeout(skDynamicSql.getTimeout());
                    this.selectKey.setFetchSize(skDynamicSql.getFetchSize());
                    this.selectKey.setKeyProperty(skDynamicSql.getKeyProperty());
                    this.selectKey.setKeyColumn(skDynamicSql.getKeyColumn());
                    this.selectKey.setOrder(skDynamicSql.getOrder());
                    this.selectKey.setHandler(skDynamicSql.getHandler());
                }
            }
        }
    }

    @Override
    public boolean isHavePlaceholder() {
        return this.target.isHavePlaceholder();
    }

    @Override
    public void buildQuery(Map<String, Object> data, DynamicContext context, BoundSqlBuilder sqlBuilder) throws SQLException {
        this.target.buildQuery(data, context, sqlBuilder);
    }

    public SelectKeyProcSql getSelectKey() {
        return this.selectKey;
    }

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

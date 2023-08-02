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

import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.MultipleResultsType;
import net.hasor.dataql.sqlproc.repository.nodes.SelectKeyDynamicSql;

import java.sql.SQLException;
import java.util.Map;

/**
 * SelectKey SqlConfig
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public class SelectKeyProcSql extends QueryProcSql {
    private String keyProperty;
    private String keyColumn;
    private String order;
    private String handler;

    public SelectKeyProcSql(SelectKeyDynamicSql target) {
        super(target);
        this.setMultipleResultType(MultipleResultsType.LAST);
    }

    public String getKeyProperty() {
        return this.keyProperty;
    }

    public void setKeyProperty(String keyProperty) {
        this.keyProperty = keyProperty;
    }

    public String getKeyColumn() {
        return this.keyColumn;
    }

    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    public String getOrder() {
        return this.order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getHandler() {
        return this.handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    @Override
    public void buildQuery(Hints hints, Map<String, Object> data, DynamicContext context, BoundSqlBuilder sqlBuilder) throws SQLException {
        ((SelectKeyDynamicSql) this.target).buildSqlQuery(hints, data, context, sqlBuilder);
    }
}

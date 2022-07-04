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
import net.hasor.cobble.setting.SettingNode;
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.DynamicContext;
import net.hasor.dataql.sqlproc.dynamic.nodes.SelectKeyDynamicSql;

import java.sql.SQLException;
import java.util.Map;

/**
 * SelectKey SqlConfig
 * @version : 2021-06-19
 * @author 赵永春 (zyc@hasor.net)
 */
public class SelectKeyProcSql extends AbstractProcSql {
    private final String keyProperty;
    private final String keyColumn;
    private final String order;
    private final String handler;

    SelectKeyProcSql(SelectKeyDynamicSql target, SettingNode options) {
        super(target, options);
        this.keyProperty = target.getKeyProperty();
        this.keyColumn = target.getKeyColumn();
        this.order = target.getOrder();
        this.handler = target.getHandler();
    }

    public String getKeyProperty() {
        return this.keyProperty;
    }

    public String getKeyColumn() {
        return this.keyColumn;
    }

    public String getOrder() {
        return this.order;
    }

    public String getHandler() {
        return this.handler;
    }

    @Override
    public void buildQuery(Map<String, Object> data, DynamicContext context, BoundSqlBuilder sqlBuilder) throws SQLException {
        ((SelectKeyDynamicSql) this.target).buildSelectKeyQuery(data, context, sqlBuilder);
    }
}

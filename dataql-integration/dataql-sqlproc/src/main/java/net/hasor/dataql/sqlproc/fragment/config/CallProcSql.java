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
import net.hasor.cobble.setting.SettingNode;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.fragment.QueryType;
import net.hasor.dataql.sqlproc.fragment.StatementType;

/**
 * Query SqlConfig
 * @version : 2021-06-19
 * @author 赵永春 (zyc@hasor.net)
 */
public class CallProcSql extends QueryProcSql {

    public CallProcSql(DynamicSql target, SettingNode options) {
        super(target, options);
        this.setStatementType(StatementType.Callable);
    }

    @Override
    public QueryType getDynamicType() {
        return QueryType.Call;
    }
}

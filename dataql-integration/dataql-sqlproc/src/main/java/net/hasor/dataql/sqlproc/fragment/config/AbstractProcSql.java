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
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.DynamicContext;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.fragment.QueryType;

import java.sql.SQLException;
import java.util.Map;

/**
 * Segment SqlConfig
 * @version : 2021-06-19
 * @author 赵永春 (zyc@hasor.net)
 */
public abstract class AbstractProcSql implements ProcSql {
    protected final DynamicSql target;

    public AbstractProcSql(DynamicSql target) {
        this.target = target;
    }

    public abstract QueryType getDynamicType();

    @Override
    public boolean isHavePlaceholder() {
        return this.target.isHavePlaceholder();
    }

    @Override
    public void buildQuery(Map<String, Object> data, DynamicContext context, BoundSqlBuilder sqlBuilder) throws SQLException {
        this.target.buildQuery(data, context, sqlBuilder);
    }

    public boolean supportBatch() {
        if (this.isHavePlaceholder()) {
            // 分析SQL后如果含有占位符：退化为 非批量（占位符会导致每次执行的SQL语句可能不一样）
            return false;
        } else {
            // 只有 Insert/Update/Delete 支持批量
            QueryType queryType = getDynamicType();
            return (QueryType.Insert == queryType || QueryType.Update == queryType || QueryType.Delete == queryType);
        }
    }

    public boolean supportPage() {
        return getDynamicType() == QueryType.Query;
    }
}
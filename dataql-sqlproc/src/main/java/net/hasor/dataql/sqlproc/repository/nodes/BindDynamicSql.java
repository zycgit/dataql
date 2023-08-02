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
package net.hasor.dataql.sqlproc.repository.nodes;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.DynamicSql;
import net.hasor.dataql.sqlproc.repository.ognl.OgnlUtils;

import java.util.Map;

/**
 * <bind> 标签
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-05-24
 */
public class BindDynamicSql implements DynamicSql {
    private final String name;      // 名字
    private final String valueExpr; // 值

    public BindDynamicSql(String name, String valueExpr) {
        this.name = name;
        this.valueExpr = valueExpr;
    }

    @Override
    public boolean isHavePlaceholder() {
        return false;
    }

    @Override
    public void buildQuery(Hints hints, Map<String, Object> data, DynamicContext context, BoundSqlBuilder sqlBuilder) {
        if (StringUtils.isNotBlank(this.name)) {
            Object testExprResult = OgnlUtils.evalOgnl(this.valueExpr, data);
            data.put(this.name, testExprResult);
        }
    }
}
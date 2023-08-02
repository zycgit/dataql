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
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.dialect.BoundSqlBuilder;
import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.DynamicSql;

import java.sql.SQLException;
import java.util.Map;

/**
 * <choose>、<when>、<otherwise> 标签
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-05-24
 */
public class ChooseDynamicSql extends ArrayDynamicSql {
    private DynamicSql defaultDynamicSql;

    public void addWhen(String test, ArrayDynamicSql nodeBlock) {
        IfDynamicSql whenSqlNode = new IfDynamicSql(test);
        whenSqlNode.addChildNode(nodeBlock);

        super.addChildNode(whenSqlNode);
    }

    /** 追加子节点 */
    public void setDefaultNode(DynamicSql block) {
        this.defaultDynamicSql = block;
    }

    @Override
    public void buildQuery(Hints hints, Map<String, Object> data, DynamicContext context, BoundSqlBuilder sqlBuilder) throws SQLException {
        boolean useDefault = true;
        try {
            for (DynamicSql dynamicSql : this.subNodes) {
                if (dynamicSql instanceof IfDynamicSql) {

                    boolean test = ((IfDynamicSql) dynamicSql).test(data);
                    if (test) {
                        ((IfDynamicSql) dynamicSql).buildBody(hints, data, context, sqlBuilder);
                        useDefault = false;
                        break;
                    }

                } else {
                    dynamicSql.buildQuery(hints, data, context, sqlBuilder);
                }
            }
        } finally {
            if (useDefault) {
                if (!sqlBuilder.lastSpaceCharacter()) {
                    sqlBuilder.appendSql(" ");
                }
                this.defaultDynamicSql.buildQuery(hints, data, context, sqlBuilder);
            }
        }
    }
}
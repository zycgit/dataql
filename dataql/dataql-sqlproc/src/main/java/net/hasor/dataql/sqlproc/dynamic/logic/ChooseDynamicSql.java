/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.logic;
import java.sql.SQLException;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * <choose>、<when>、<otherwise> 标签
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-05-24
 */
public class ChooseDynamicSql extends ArrayDynamicSql {
    private DynamicSql defaultDynamicSql;

    public void addThen(String test, DynamicSql nodeBlock) {
        IfDynamicSql whenSqlNode = new IfDynamicSql(test);
        whenSqlNode.addChildNode(nodeBlock);

        this.addChildNode(whenSqlNode);
    }

    @Override
    public void addChildNode(DynamicSql node) {
        if (node instanceof IfDynamicSql) {
            this.subNodes.add(node);
        }
    }

    /** 追加子节点 */
    public void setDefaultNode(DynamicSql block) {
        this.defaultDynamicSql = block;
    }

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        boolean useDefault = true;
        try {
            for (DynamicSql dynamicSql : this.subNodes) {
                if (dynamicSql instanceof IfDynamicSql) {

                    boolean test = ((IfDynamicSql) dynamicSql).test(data);
                    if (test) {
                        ((IfDynamicSql) dynamicSql).buildBody(data, context, sqlBuilder);
                        useDefault = false;
                        break;
                    }
                }
            }
        } finally {
            if (useDefault) {
                if (!sqlBuilder.lastSpaceCharacter()) {
                    sqlBuilder.appendSql(" ");
                }
                this.defaultDynamicSql.buildQuery(data, context, sqlBuilder);
            }
        }
    }
}

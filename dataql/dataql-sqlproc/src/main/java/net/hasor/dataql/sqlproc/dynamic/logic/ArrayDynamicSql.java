/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.logic;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.segment.PlanDynamicSql;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * 多个 SQL 节点组合成一个 SqlNode
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-05-24
 */
public class ArrayDynamicSql implements DynamicSql {
    /** 子节点 */
    protected List<DynamicSql> subNodes = new ArrayList<>();

    /** 获取节点 */
    public List<DynamicSql> getSubNodes() {
        return this.subNodes;
    }

    /** 追加子节点 */
    public void addChildNode(DynamicSql node) {
        if (node != null) {
            this.subNodes.add(node);
        }
    }

    /** 最后一个节点是文本 */
    public boolean lastIsText() {
        if (this.subNodes.isEmpty()) {
            return false;
        } else {
            return this.subNodes.get(this.subNodes.size() - 1) instanceof PlanDynamicSql;
        }
    }

    /** 最后一个节点 */
    public DynamicSql lastNode() {
        if (this.subNodes.isEmpty()) {
            return null;
        } else {
            return this.subNodes.get(this.subNodes.size() - 1);
        }
    }

    @Override
    public boolean isHaveInjection() {
        for (DynamicSql dynamicSql : this.subNodes) {
            if (dynamicSql.isHaveInjection()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException {
        for (int i = 0; i < this.subNodes.size(); i++) {
            DynamicSql dynamicSql = this.subNodes.get(i);
            if (visitItem(i, dynamicSql, context, sqlBuilder)) {
                dynamicSql.buildQuery(data, context, sqlBuilder);
            }
        }
    }

    protected boolean visitItem(int i, DynamicSql dynamicSql, QueryContext context, SqlBuilder sqlBuilder) {
        return true;
    }
}

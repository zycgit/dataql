/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic;
import java.sql.SQLException;
import java.util.Map;
import net.hasor.dataql.sqlproc.dynamic.args.MapSqlArgSource;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/**
 * 本处理器，兼容 @{...}、#{...}、${...} 三种写法。
 * @author 赵永春 (zyc@hasor.net)
 * @version 2020-03-28
 */
public interface DynamicSql {
    /** 是否包含替换占位符，如果包含替换占位符那么不能使用批量模式 */
    boolean isHaveInjection();

    void buildQuery(SqlArgSource data, QueryContext context, SqlBuilder sqlBuilder) throws SQLException;

    default SqlBuilder buildQuery(SqlArgSource data, QueryContext context) throws SQLException {
        SqlBuilder fxBuilder = new SqlBuilder();
        this.buildQuery(data, context, fxBuilder);
        return fxBuilder;
    }

    default SqlBuilder buildQuery(Map<String, Object> data, QueryContext context) throws SQLException {
        SqlBuilder fxBuilder = new SqlBuilder();
        this.buildQuery(new MapSqlArgSource(data), context, fxBuilder);
        return fxBuilder;
    }
}

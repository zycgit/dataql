/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect.provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.hasor.dataql.sqlproc.dialect.BoundSql;

/**
 * Informix 的 SqlDialect 实现
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public class InformixDialect extends AbstractDialect {
    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        String sqlString = boundSql.getSqlString();
        List<Object> paramArrays = new ArrayList<>(Arrays.asList(boundSql.getArgs()));

        StringBuilder sqlBuilder = new StringBuilder();
        List<Object> newParam = new ArrayList<>();
        sqlBuilder.append("SELECT ");
        if (start > 0) {
            sqlBuilder.append(" SKIP ? ");
            newParam.add(start);
        }
        if (limit > 0) {
            sqlBuilder.append(" FIRST ? ");
            newParam.add(limit);
        }
        sqlBuilder.append(" * FROM ( ");
        sqlBuilder.append(sqlString);
        sqlBuilder.append(" ) TEMP_T");

        paramArrays.addAll(0, newParam);
        return new BoundSql.BoundSqlObj(sqlBuilder.toString(), paramArrays.toArray());
    }
}

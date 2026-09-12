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
 * Oracle 的 SqlDialect 实现
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public class OracleDialect extends AbstractDialect {
    @Override
    public BoundSql countSql(BoundSql boundSql) {
        String sqlBuilder = "SELECT COUNT(*) FROM (" + boundSql.getSqlString() + ") TEMP_T";
        return new BoundSql.BoundSqlObj(sqlBuilder, boundSql.getArgs());
    }

    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        String sqlString = boundSql.getSqlString();
        List<Object> paramArrays = new ArrayList<>(Arrays.asList(boundSql.getArgs()));

        String sqlBuilder = "SELECT * FROM ( SELECT TMP.*, ROWNUM ROW_ID FROM ( " + sqlString + " ) TMP WHERE ROWNUM <= ? ) WHERE ROW_ID > ?";

        paramArrays.add(start + limit);
        paramArrays.add(start);
        return new BoundSql.BoundSqlObj(sqlBuilder, paramArrays.toArray());
    }
}

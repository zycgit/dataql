/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dialect.provider;
import net.hasor.dataql.sqlproc.dialect.BoundSql;

/**
 * DB2 的 SqlDialect 实现
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-10-31
 */
public class Db2Dialect extends AbstractDialect {
    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        String sqlBuilder = "SELECT * FROM (SELECT TMP_PAGE.*,ROWNUMBER() OVER() AS ROW_ID FROM ( " + boundSql.getSqlString() + " ) AS TMP_PAGE) TMP_PAGE WHERE ROW_ID BETWEEN ? AND ?";

        Object[] paramArray = boundSql.getArgs();
        Object[] destArgs = new Object[paramArray.length + 2];
        System.arraycopy(paramArray, 0, destArgs, 0, paramArray.length);
        destArgs[paramArray.length] = start + 1;
        destArgs[paramArray.length + 1] = start + limit;
        return new BoundSql.BoundSqlObj(sqlBuilder, destArgs);
    }
}

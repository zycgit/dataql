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
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.dialect.BoundSql;

/**
 * SqlServer2005 的 SqlDialect 实现
 * @author 赵永春 (zyc@hasor.net)
 * @since 2016-11-10
 */
public class SqlServerDialect extends AbstractDialect {
    private static String getOrderByPart(String sql) {
        String loweredString = sql.toLowerCase();
        int orderByIndex = loweredString.indexOf("order by");
        if (orderByIndex != -1) {
            return sql.substring(orderByIndex);
        } else {
            return "";
        }
    }

    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        String sqlString = boundSql.getSqlString();
        List<Object> paramArrays = new ArrayList<>(Arrays.asList(boundSql.getArgs()));
        //
        StringBuilder pagingBuilder = new StringBuilder();
        String orderby = getOrderByPart(sqlString);
        String distinctStr = "";
        String loweredString = sqlString.toLowerCase();
        String sqlPartString = sqlString;
        if (loweredString.trim().toLowerCase().startsWith("select")) {
            int index = 6;
            if (loweredString.toLowerCase().startsWith("select distinct")) {
                distinctStr = "DISTINCT ";
                index = 15;
            }
            sqlPartString = sqlPartString.substring(index);
        }
        pagingBuilder.append(sqlPartString);
        // if no ORDER BY is specified use fake ORDER BY field to avoid errors
        if (StringUtils.isBlank(orderby)) {
            orderby = "ORDER BY CURRENT_TIMESTAMP";
        }
        long firstParam = start + 1;
        long secondParam = start + limit;
        sqlString = "WITH selectTemp AS (SELECT " + distinctStr + "TOP 100 PERCENT " + //
                " ROW_NUMBER() OVER (" + orderby + ") as __row_number__, " + pagingBuilder + ") SELECT * FROM selectTemp WHERE __row_number__ BETWEEN " +
                //FIX#299：原因：mysql 中 limit 10(offset,size) 是从第10开始（不包含10）,；而这里用的BETWEEN是两边都包含，所以改为offset+1
                firstParam + " AND " + secondParam + " ORDER BY __row_number__";
        //
        paramArrays.add(firstParam);
        paramArrays.add(secondParam);
        return new BoundSql.BoundSqlObj(sqlString, paramArrays.toArray());
    }
}

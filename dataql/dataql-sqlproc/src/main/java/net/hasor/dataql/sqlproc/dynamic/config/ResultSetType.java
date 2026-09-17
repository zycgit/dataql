/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.config;
import java.sql.ResultSet;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.SqlHintValue;

/**
 * FORWARD_ONLY，SCROLL_SENSITIVE, SCROLL_INSENSITIVE 或 DEFAULT（等价于 unset） 中的一个，默认值为 unset （依赖数据库驱动）。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public enum ResultSetType {
    FORWARD_ONLY(SqlHintValue.FRAGMENT_SQL_STAT_RESULT_FORWARD_ONLY, ResultSet.TYPE_FORWARD_ONLY),
    SCROLL_SENSITIVE(SqlHintValue.FRAGMENT_SQL_STAT_RESULT_SCROLL_SENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE),
    SCROLL_INSENSITIVE(SqlHintValue.FRAGMENT_SQL_STAT_RESULT_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_INSENSITIVE),
    DEFAULT(SqlHintValue.FRAGMENT_SQL_STAT_RESULT_DEFAULT, null),
    ;

    private final String  typeName;
    private final Integer resultSetType;

    ResultSetType(String typeName, Integer resultSetType) {
        this.typeName = typeName;
        this.resultSetType = resultSetType;
    }

    public String getTypeName() {
        return this.typeName;
    }

    public Integer getResultSetType() {
        return this.resultSetType;
    }

    public static ResultSetType valueOfCode(String code, ResultSetType defaultType) {
        for (ResultSetType tableType : ResultSetType.values()) {
            if (StringUtils.equalsIgnoreCase(tableType.typeName, code)) {
                return tableType;
            }
        }
        return defaultType;
    }

}

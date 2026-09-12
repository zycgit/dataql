/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.config;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.SqlHintValue;

/**
 * 使用 Statement 方式。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public enum StatementType {
    /** 使用 java.sql.Statement */
    Statement(SqlHintValue.FRAGMENT_SQL_STAT_TYPE),
    /** 使用 java.sql.PreparedStatement */
    Prepared(SqlHintValue.FRAGMENT_SQL_STAT_PREPARED),
    /** 使用 java.sql.CallableStatement */
    Callable(SqlHintValue.FRAGMENT_SQL_STAT_CALLABLE),
    ;

    private final String value;

    StatementType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static StatementType valueOfCode(String code, StatementType defaultType) {
        for (StatementType tableType : StatementType.values()) {
            if (StringUtils.equalsIgnoreCase(tableType.value, code)) {
                return tableType;
            }
        }
        return defaultType;
    }
}

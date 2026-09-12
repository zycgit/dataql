/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc;
import net.hasor.cobble.StringUtils;

/**
 * 返回值类型
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-02-04
 */
public enum ColumnCaseType {

    /**
     * SqlFragment 返回的列信息,全部列名保持大小写敏感。
     */
    ColumnCaseDefault(SqlHintValue.FRAGMENT_SQL_COLUMN_CASE_DEFAULT),
    /**
     * SqlFragment 全部列名保持大写，如果在转换过程中发生冲突，那么会产生覆盖问题。
     */
    ColumnCaseUpper(SqlHintValue.FRAGMENT_SQL_COLUMN_CASE_UPPER),
    /**
     * SqlFragment 全部列名保持小写，如果在转换过程中发生冲突，那么会产生覆盖问题。
     */
    ColumnCaseLower(SqlHintValue.FRAGMENT_SQL_COLUMN_CASE_LOWER),
    /**
     * SqlFragment 返回的列信息,全部列名做一次驼峰转换。如：goods_id => goodsId、GOODS_id => goodsId。
     */
    ColumnCaseHump(SqlHintValue.FRAGMENT_SQL_COLUMN_CASE_HUMP);

    private final String typeCode;

    ColumnCaseType(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeCode() {
        return this.typeCode;
    }

    public static ColumnCaseType valueOfCode(String typeCode) {
        if (StringUtils.isBlank(typeCode)) {
            typeCode = SqlHintNames.FRAGMENT_SQL_COLUMN_CASE.getDefaultVal();
        }
        for (ColumnCaseType type : ColumnCaseType.values()) {
            if (StringUtils.equalsIgnoreCase(type.typeCode, typeCode)) {
                return type;
            }
        }
        return ColumnCaseDefault;
    }
}

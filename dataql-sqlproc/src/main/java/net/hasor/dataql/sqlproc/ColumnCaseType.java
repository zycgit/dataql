/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
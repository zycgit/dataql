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
package net.hasor.dataql.sqlproc.execute;

import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.SqlHintValue;

/**
 * 返回值类型
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-02-04
 */
public enum OpenPackageType {
    /**
     * SqlFragment 返回值不拆开，无论返回数据，都以 List/Map 形式返回。
     */
    Off(SqlHintValue.FRAGMENT_SQL_OPEN_PACKAGE_OFF),
    /**
     * SqlFragment 返回值拆分到行，如果返回值是多条记录那么行为和 off 相同。
     *  - 当返回 0 或 1 条记录时，自动解开最外层的 List，返回一个 Object。
     */
    Row(SqlHintValue.FRAGMENT_SQL_OPEN_PACKAGE_ROW),
    /**
     * SqlFragment 返回值拆分到行，如果返回值是多条记录那么行为和 off 相同。
     *  - 如果返回值是 1条记录并且具有多个字段值，那么行为和 row 相同。
     *  - 一条记录中如果只有一个字段，那么会忽略字段名直接返回这个字段的值。
     *  - 如果查询结果为空集合，那么返回 null 值。 */
    Column(SqlHintValue.FRAGMENT_SQL_OPEN_PACKAGE_COLUMN),
    ;

    private final String typeCode;

    OpenPackageType(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeCode() {
        return this.typeCode;
    }

    public static OpenPackageType valueOfCode(String typeCode) {
        for (OpenPackageType type : OpenPackageType.values()) {
            if (StringUtils.equalsIgnoreCase(type.typeCode, typeCode)) {
                return type;
            }
        }
        return null;
    }
}
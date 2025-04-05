/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.sqlproc.dynamic.config;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.SqlHintValue;

/**
 * FORWARD_ONLY，SCROLL_SENSITIVE, SCROLL_INSENSITIVE 或 DEFAULT（等价于 unset） 中的一个，默认值为 unset （依赖数据库驱动）。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public enum ResultSetType {
    FORWARD_ONLY(SqlHintValue.FRAGMENT_SQL_STAT_TYPE_FORWARD_ONLY),
    SCROLL_SENSITIVE(SqlHintValue.FRAGMENT_SQL_STAT_TYPE_SCROLL_SENSITIVE),
    SCROLL_INSENSITIVE(SqlHintValue.FRAGMENT_SQL_STAT_SCROLL_INSENSITIVE),
    DEFAULT(null),
    ;

    private final String value;

    ResultSetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static ResultSetType valueOfCode(String code, ResultSetType defaultType) {
        for (ResultSetType tableType : ResultSetType.values()) {
            if (StringUtils.equalsIgnoreCase(tableType.value, code)) {
                return tableType;
            }
        }
        return defaultType;
    }
}

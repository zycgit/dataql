/*
 * Copyright 2015-2022 the original author or authors.
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
public enum ConfigFormatType {
    /**
     * SqlFragment 返回值不拆开，无论返回数据，都以 List/Map 形式返回。
     */
    Xml(SqlHintValue.FRAGMENT_FORMAT_XML),
    /**
     * SqlFragment 返回值拆分到行，如果返回值是多条记录那么行为和 off 相同。
     * - 当返回 0 或 1 条记录时，自动解开最外层的 List，返回一个 Object。
     */
    Text(SqlHintValue.FRAGMENT_FORMAT_TEXT),
    ;

    private final String typeCode;

    ConfigFormatType(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeCode() {
        return this.typeCode;
    }

    public static ConfigFormatType valueOfCode(String typeCode) {
        if (StringUtils.isBlank(typeCode)) {
            typeCode = SqlHintNames.FRAGMENT_SQL_FORMAT.getDefaultVal();
        }
        for (ConfigFormatType type : ConfigFormatType.values()) {
            if (StringUtils.equalsIgnoreCase(type.typeCode, typeCode)) {
                return type;
            }
        }
        return Text;
    }
}
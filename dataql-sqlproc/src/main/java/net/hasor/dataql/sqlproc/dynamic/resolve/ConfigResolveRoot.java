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
package net.hasor.dataql.sqlproc.dynamic.resolve;
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.ConfigFormatType;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;

/**
 * parse dynamic SQL from mapperFile
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-05
 */
public class ConfigResolveRoot {
    private final ConfigResolveByPlanSql planSql = new ConfigResolveByPlanSql();
    private final ConfigResolveByXmlSql  xmlSql  = new ConfigResolveByXmlSql();

    public SqlConfig parseConfig(String fragmentName, Hints hint, String config) {
        String formatStr = SqlHintNames.getValue(hint, SqlHintNames.FRAGMENT_SQL_FORMAT);
        ConfigFormatType formatType = ConfigFormatType.valueOfCode(formatStr);

        switch (formatType) {
            case Xml:
                return this.xmlSql.parseConfig(fragmentName, hint, config);
            case Text:
                return this.planSql.parseConfig(fragmentName, hint, config);
            default:
                throw new UnsupportedOperationException("fragment '" + fragmentName + "' formatType " + formatStr + " Unsupported.");
        }
    }
}
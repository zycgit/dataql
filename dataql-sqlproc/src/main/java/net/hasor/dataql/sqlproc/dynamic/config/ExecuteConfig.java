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
package net.hasor.dataql.sqlproc.dynamic.config;
import net.hasor.cobble.ArrayUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;

/**
 * Execute SqlConfig
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public class ExecuteConfig extends SqlConfig {
    private String[] bindOut = ArrayUtils.EMPTY_STRING_ARRAY;

    public ExecuteConfig(ArrayDynamicSql target, Hints config) {
        super(target, config);

        if (config != null) {
            String bindOutStr = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_BIND_OUT);

            this.bindOut = StringUtils.isNotBlank(bindOutStr) ? bindOutStr.split(",") : ArrayUtils.EMPTY_STRING_ARRAY;
        }
    }

    @Override
    public QueryType getType() {
        return QueryType.Execute;
    }

    public String[] getBindOut() {
        return this.bindOut;
    }
}
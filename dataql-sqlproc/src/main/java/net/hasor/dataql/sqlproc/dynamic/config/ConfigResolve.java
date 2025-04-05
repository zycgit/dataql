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
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;

/**
 * parse dynamic SQL from mapperFile
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-05
 */
public abstract class ConfigResolve {
    public abstract SqlConfig parseConfig(String fragmentName, Hints hint, String config);

    protected SqlConfig createConfig(QueryType queryType, Hints hint, ArrayDynamicSql dynamicSql) {
        switch (queryType) {
            case Insert:
                return new InsertConfig(dynamicSql, hint);
            case Delete:
                return new DeleteConfig(dynamicSql, hint);
            case Update:
                return new UpdateConfig(dynamicSql, hint);
            case Execute:
                return new ExecuteConfig(dynamicSql, hint);
            case Select:
                return new SelectConfig(dynamicSql, hint);
            default:
                throw new UnsupportedOperationException("queryType '" + queryType.name() + "' Unsupported.");
        }
    }
}
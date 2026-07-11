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
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.config.*;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;

/**
 * parse dynamic SQL from mapperFile
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-05
 */
public abstract class ConfigResolve {
    public static final SqlHintNames[] CONFIG_HINTS = new SqlHintNames[] { //
            SqlHintNames.FRAGMENT_SQL_STATEMENT,       //
            SqlHintNames.FRAGMENT_SQL_TIMEOUT,         //
            SqlHintNames.FRAGMENT_SQL_FETCH_SIZE,      //
            SqlHintNames.FRAGMENT_SQL_RESULT_SET_TYPE, //
            SqlHintNames.FRAGMENT_SQL_BIND_OUT,        //
            SqlHintNames.FRAGMENT_SQL_KEY_GENERATED,   //
            SqlHintNames.FRAGMENT_SQL_KEY_PROPERTY,    //
            SqlHintNames.FRAGMENT_SQL_KEY_COLUMN,      //
            SqlHintNames.FRAGMENT_SQL_ORDER            //
    };

    public abstract SqlConfig parseConfig(String fragmentName, Hints hint, String config);

    protected SqlConfig createConfig(QueryType queryType, Hints hint, ArrayDynamicSql dynamicSql) {
        return switch (queryType) {
            case Insert -> new InsertConfig(dynamicSql, hint);
            case Delete -> new DeleteConfig(dynamicSql, hint);
            case Update -> new UpdateConfig(dynamicSql, hint);
            case Execute, Call -> new ExecuteConfig(dynamicSql, hint);
            case Select -> new SelectConfig(dynamicSql, hint);
            default -> throw new UnsupportedOperationException("queryType '" + queryType.name() + "' Unsupported.");
        };
    }
}
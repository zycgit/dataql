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
package net.hasor.dataql.sqlproc.execute.support;

import java.sql.Connection;
import java.sql.SQLException;
import net.hasor.cobble.function.EFunction;
import net.hasor.dataql.Hints;
import net.hasor.dataql.runtime.HintsProxy;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.StatementType;

public class CallFragmentProcess extends AbstractSqlFragment {
    public CallFragmentProcess(EFunction<String, Connection, SQLException> c, QueryContext queryContext) {
        super(c, queryContext);
    }

    @Override
    protected QueryType queryType(String fragmentString, Hints hints) {
        return QueryType.Call;
    }

    @Override
    protected Hints resolveHints(Hints hints) {
        return new HintsProxy(hints) {
            @Override
            public Object getHint(String optionKey) {
                if (SqlHintNames.FRAGMENT_SQL_STATEMENT.matchKey(optionKey)) {
                    return StatementType.Callable.getValue();
                }
                return super.getHint(optionKey);
            }
        };
    }
}

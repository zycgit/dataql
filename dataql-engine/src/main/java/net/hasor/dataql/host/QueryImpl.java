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
package net.hasor.dataql.host;

import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.kernel.*;

/**
 * Query 的默认实现，委托给 {@link QueryExecutor} 执行。
 */
class QueryImpl extends HintsSet implements Query {
    private final QIL                 qil;
    private final Finder              finder;
    private final Map<String, Object> shareVarMap;

    QueryImpl(QIL qil, Finder finder) {
        this.qil = qil;
        this.finder = finder;
        this.shareVarMap = new HashMap<>();
    }

    @Override
    public Query clone() {
        QueryImpl query = new QueryImpl(this.qil, this.finder);
        query.shareVarMap.putAll(this.shareVarMap);
        return query;
    }

    @Override
    public void addShareVar(String key, Object value) {
        this.shareVarMap.put(key, value);
    }

    @Override
    public QueryResult execute(CustomizeScope customize) throws QueryRuntimeException {
        return new QueryExecutor(this.qil, this.finder).execute(this, this.shareVarMap, customize);
    }
}

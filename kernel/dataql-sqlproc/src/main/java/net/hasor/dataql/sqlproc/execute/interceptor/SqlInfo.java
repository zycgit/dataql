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
package net.hasor.dataql.sqlproc.execute.interceptor;

import java.util.Objects;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.HintsReadOnly;

/**
 * Final SQL information exposed to execution interceptors after dynamic SQL rendering and dialect rewriting.
 */
public record SqlInfo(boolean batch, String sourceName, String queryString, Object[] queryParams, HintsReadOnly hints) {
    public SqlInfo(boolean batch, String sourceName, String queryString, Object[] queryParams, Hints hints) {
        this(batch, sourceName, queryString, queryParams, new HintsReadOnly(hints));
    }

    public SqlInfo {
        queryParams = queryParams == null ? new Object[0] : queryParams.clone();
        hints = Objects.requireNonNull(hints, "hints is null.");
    }

    /** Returns the data source name selected by the SQL fragment. */
    @Override
    public String sourceName() {
        return this.sourceName;
    }

    /** Returns the final SQL sent to JDBC when the interceptor chain proceeds. */
    @Override
    public String queryString() {
        return this.queryString;
    }

    /** Returns a copy of the final binding arguments, including any SQL argument metadata. */
    @Override
    public Object[] queryParams() {
        return this.queryParams.clone();
    }
}

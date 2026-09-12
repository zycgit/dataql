/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
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

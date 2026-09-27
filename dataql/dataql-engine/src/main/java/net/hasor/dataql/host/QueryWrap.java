/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataql.kernel.QueryRuntimeException;

/**
 * Standard delegating wrapper for Query.
 * The inherited execute overloads call this wrapper's execute(CustomizeScope).
 */
public class QueryWrap implements Query {
    private final Query query;

    public QueryWrap(Query query) {
        this.query = query;
    }

    /** Returns the query wrapped by this instance. */
    public Query getQuery() {
        return this.query;
    }

    @Override
    public void addShareVar(String key, Object value) {
        this.query.addShareVar(key, value);
    }

    @Override
    public QueryResult execute(CustomizeScope customizeScope) throws QueryRuntimeException {
        return this.query.execute(customizeScope);
    }

    /** Wraps a copy of the delegate. Subclasses can override this to copy their own state. */
    @Override
    public QueryWrap clone() {
        return new QueryWrap(this.query.clone());
    }

    @Override
    public String[] getHints() {
        return this.query.getHints();
    }

    @Override
    public Object getHint(String optionKey) {
        return this.query.getHint(optionKey);
    }

    @Override
    public void removeHint(String optionKey) {
        this.query.removeHint(optionKey);
    }

    @Override
    public void setHint(String hintName, Object value) {
        this.query.setHint(hintName, value);
    }
}

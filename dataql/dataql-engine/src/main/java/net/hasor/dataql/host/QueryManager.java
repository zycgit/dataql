/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host;
import java.util.Objects;

/** Creates query builders for a shared host context. */
public class QueryManager {
    private final HostContext hostContext;

    public QueryManager(HostContext hostContext) {
        this.hostContext = Objects.requireNonNull(hostContext, "hostContext is null.");
    }

    public HostContext getHostContext() {
        return this.hostContext;
    }

    public QueryBuilder newBuilder() {
        return new QueryBuilder(this.hostContext);
    }
}

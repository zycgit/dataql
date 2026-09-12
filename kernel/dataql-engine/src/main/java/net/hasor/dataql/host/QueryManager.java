/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host;

import java.util.Objects;

/**
 * 查询中心，提供 QueryBuilder，并为后续的 Query 管理预留统一入口。
 */
public record QueryManager(HostContext hostContext) {
    public QueryManager(HostContext hostContext) {
        this.hostContext = Objects.requireNonNull(hostContext, "hostContext is null.");
    }

    public QueryBuilder newBuilder() {
        return new QueryBuilder(this.hostContext);
    }
}

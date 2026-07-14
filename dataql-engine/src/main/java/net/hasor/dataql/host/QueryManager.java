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

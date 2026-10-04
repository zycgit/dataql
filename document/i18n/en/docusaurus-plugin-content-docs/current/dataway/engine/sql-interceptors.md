---
title: "9.8 SQL Interceptors"
---

:::info[Requires the SQL executor]

This extension requires the [SQL executor](../dataql-engine/sql.md) (`dataql-sqlproc`). Configure [SQL data sources in Dataway](../capabilities/datasources.md) before using it.

:::

`SqlExecutionInterceptor` intercepts final JDBC execution for SQL logging, timing or execution checks.

## Implementation

```java
package com.example.sql;

import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInvocation;

public class SqlTraceInterceptor implements SqlExecutionInterceptor {
    private final System.Logger logger = System.getLogger(SqlTraceInterceptor.class.getName());

    @Override
    public Object invoke(SqlExecutionInvocation invocation) throws Throwable {
        this.logger.log(System.Logger.Level.INFO, invocation.getSqlInfo().queryString());
        return invocation.proceed();
    }
}
```

## Register in Dataway

`config` is the `DatawayConfig` registered by your application. Add this callback to its configuration method before Dataway is created. The same configuration API applies to Spring, Solon and Hasor; see [Engine and query configuration](customizers.md).

```java
import com.example.sql.SqlTraceInterceptor;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;

config.configureHost(host -> {
    ExecuteContext context = host.getAttachment(ExecuteContext.class);
    context.addInterceptor(new SqlTraceInterceptor());
});
```

Dataway supplies `host` when it initializes the engine. The callback registers the interceptor in that engine’s `ExecuteContext`; published APIs and console debugging use the same registration.

## Usage

Create a DataQL API in the Dataway console, then debug it or publish and call it:

```javascript
var find = @@selectSql(value)<% SELECT #{value} AS result_value %>;
return find(42);
```

Each query enters `invoke()`. This example logs the final SQL, `SELECT ? AS result_value`, at INFO level, then `proceed()` executes it. The script result is `42`.

## Execution rules

Interceptors run in registration order. `getSqlInfo()` exposes final SQL, parameters, source name and hints. Count and page queries enter the chain separately.

`addInterceptor(interceptor, predicate)` filters by operation, fragment and hints. Matching results are cached with fragment configuration, so register before the first SQL execution. The exposed SQL information is observational; a short-circuit return must match the execution chain’s result type.

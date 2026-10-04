---
title: "9.12 SQL Dialects"
---

:::info[Requires the SQL executor]

This extension requires the [SQL executor](../dataql-engine/sql.md) (`dataql-sqlproc`). For Dataway, configure [data sources](../capabilities/datasources.md) first.

:::

`PageDialect` generates count and page SQL for paginated queries. See [Pagination and dialects](../../dataql/sql/dialect.md) for built-in dialects and script usage. Implement this interface to support other pagination syntax.

## Implement a dialect

This example appends H2-compatible `LIMIT ? OFFSET ?` to a SELECT without existing pagination or a trailing semicolon. `start` is the zero-based row offset; `limit` is the page size.

```java
package com.example.sql;

import java.util.Arrays;
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.BoundSql.BoundSqlObj;
import net.hasor.dataql.sqlproc.dialect.PageDialect;

public class LimitOffsetDialect implements PageDialect {
    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        Object[] original = boundSql.getArgs();
        Object[] arguments = Arrays.copyOf(original, original.length + 2);
        arguments[original.length] = limit;
        arguments[original.length + 1] = start;
        String sql = boundSql.getSqlString() + " LIMIT ? OFFSET ?";
        return new BoundSqlObj(sql, arguments);
    }
}
```

Original arguments retain their order. The new arguments match `LIMIT` and `OFFSET`, in that order. The default `countSql` wraps the original query as `SELECT COUNT(*) FROM (...) as TEMP_T`; override it for different count syntax.

The dialect needs a public no-argument constructor, generated implicitly in this example. Instances are cached, so avoid storing per-query state.

## Register in Dataway

`config` is the application's registered `DatawayConfig`. Add this configuration before Dataway is created:

```java
import com.example.sql.LimitOffsetDialect;
import net.hasor.dataql.sqlproc.dialect.SqlDialectRegister;

config.configureHost(host -> {
    SqlDialectRegister.registerDialectAlias("appLimitOffset", LimitOffsetDialect.class);
});
```

The same configuration applies to Spring, Solon and Hasor. The SQL module shares its alias registry and dialect cache. Use an application-specific alias and register before the first paginated query. With the standalone engine, call the same registration method before creating queries.

## Use in a script

Initialize the `people` table from [SQL execution](../../dataql/sql/execute.md) in H2, then create a DataQL API in the console:

```javascript
hint FRAGMENT_SQL_PAGE_DIALECT = 'appLimitOffset';
hint FRAGMENT_SQL_QUERY_BY_PAGE = true;
hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1;
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(minAge)<%
    SELECT id, name FROM people WHERE age >= #{minAge} ORDER BY id
%>;
var page = find(18);
run page.setPageInfo({'currentPage':2, 'pageSize':1});
return page.data();
```

Debug the API or publish and call it. The page contains Bob. Page SQL arguments are `18`, `1`, `1`: minimum age, page size and row offset.

Alternatively, set `FRAGMENT_SQL_PAGE_DIALECT` to `com.example.sql.LimitOffsetDialect` to load the class directly without registering an alias.

---
title: "9.9 SQL Fragments"
---

:::info[Requires the SQL executor]

This extension requires the [SQL executor](../dataql-engine/sql.md) (`dataql-sqlproc`). Configure [SQL data sources in Dataway](../capabilities/datasources.md) before using it.

:::

SQL macros register named SQL text for reuse in plain SQL and dynamic XML.

## Register in Dataway

`config` is the `DatawayConfig` registered by your application. Add this callback to its configuration method before Dataway is created. The same configuration API applies to Spring, Solon and Hasor; see [Engine and query configuration](customizers.md).

```java
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContextImpl;

config.configureHost(host -> {
    ExecuteContext context = host.getAttachment(ExecuteContext.class);
    ((ExecuteContextImpl) context).addMacro("adult", "age >= 18");
});
```

The callback registers `adult` in the current Dataway engine. Macro registration currently belongs to `ExecuteContextImpl`, so retrieve `ExecuteContext` from the callback and cast it to the built-in implementation.

## Usage

Initialize the `people` table from [SQL execution](../../dataql/sql/execute.md) in the business database. Create a DataQL API in the console. Plain SQL references the registered macro with `@{macro, adult}`:

```javascript
var find = @@selectSql()<%
    SELECT count(*) FROM people WHERE @{macro, adult}
%>;
return find();
```

Dynamic XML references the same macro through `include`:

```javascript
var find = @@selectXml()<%
    SELECT count(*) FROM people WHERE <include refid="adult"/>
%>;
return find();
```

Both forms insert `age >= 18` into the SQL. Debug the API, or save, publish and call it; both scripts return `2` for the sample data.

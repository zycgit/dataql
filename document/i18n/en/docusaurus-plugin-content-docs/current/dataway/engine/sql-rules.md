---
title: "9.10 SQL Rules"
---

:::info[Requires the SQL executor]

This extension requires the [SQL executor](../dataql-engine/sql.md) (`dataql-sqlproc`). Configure [SQL data sources in Dataway](../capabilities/datasources.md) before using it.

:::

`SqlRule` adds application-specific rules to dynamic SQL. The example encapsulates a fixed condition for active records.

## Implementation

```java
package com.example.sql;

import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/** Adds the application's fixed active-record condition. */
public class EnabledRule implements SqlRule {
    @Override
    public boolean test(SqlArgSource data, QueryContext context, String expression) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sql, String expression, String value) {
        sql.appendSql("enabled = 1");
    }
}
```

## Register in Dataway

`config` is the `DatawayConfig` registered by your application. Add this callback to its configuration method before Dataway is created. The same configuration API applies to Spring, Solon and Hasor; see [Engine and query configuration](customizers.md).

```java
import com.example.sql.EnabledRule;
import net.hasor.dataql.sqlproc.dynamic.rule.RuleRegistry;

config.configureHost(host -> {
    RuleRegistry.DEFAULT.register("manualEnabled", new EnabledRule());
});
```

The callback registers `manualEnabled` when Dataway initializes. `RuleRegistry.DEFAULT` is shared: registration affects all SQL executors using it. Use distinct application rule names and keep implementations thread-safe.

## Usage

Initialize the `people` table from [SQL execution](../../dataql/sql/execute.md), then create a DataQL API in the console and use the registered name:

```javascript
var find = @@selectSql()<% SELECT count(*) FROM people WHERE @{manualEnabled} %>;
return find();
```

`test` determines whether the rule runs; `executeRule` writes SQL and parameters to `SqlBuilder`. This example expands to `WHERE enabled = 1`. Debug the API, or save, publish and call it; the script returns `2` for the sample data. See [Dynamic rules](../../dataql/sql/rules.md) for built-in rules.

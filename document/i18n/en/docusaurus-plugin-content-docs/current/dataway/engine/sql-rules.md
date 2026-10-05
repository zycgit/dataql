---
title: "9.10 SQL Rules"
---

:::info[Requires the SQL executor]

This extension depends on the [SQL executor](../dataql-engine/sql.md), provided by `dataql-sqlproc`. Configure [Dataway data sources](../capabilities/datasources.md) first.

:::

`SqlRule` implements custom `@{ruleName, expression, content}` behavior, such as shared filters and argument transformations. Rules run before SQL execution and append SQL and bound values to `SqlBuilder`.

## Implement a fixed condition

Encapsulate the active-record filter as `manualEnabled`:

```java
package com.example.sql;

import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

public class EnabledRule implements SqlRule {
    @Override
    public boolean test(SqlArgSource data, QueryContext context, String expression) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context,
                            SqlBuilder sql, String expression, String value) {
        sql.appendSql("enabled = 1");
    }
}
```

`executeRule` runs only if test returns true. Returning false omits the entire rule. This rule has no business arguments and always emits a fixed condition.

## Register in Dataway

Register rules through the application's `DatawayConfig`. The framework integration creates Dataway. Here `connectionProvider` is the application's configured SQL connection provider:

```java
import com.example.sql.EnabledRule;
import net.hasor.dataql.sqlproc.dynamic.rule.RuleRegistry;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.service.DatawayConfig;

public class SqlConfiguration {
    public DatawayConfig datawayConfig(ConnectionProvider connectionProvider) {
        DatawayConfig config = new DatawayConfig();
        config.attachment(ConnectionProvider.class, connectionProvider);
        config.configureHost(host -> {
            RuleRegistry.DEFAULT.register("manualEnabled", new EnabledRule());
        });
        return config;
    }
}
```

Expose this configuration through the [Spring](../integration/spring.md), [Solon](../integration/solon.md) or [Hasor](../integration/hasor.md) integration. For an existing `DatawayConfig`, add the configureHost callback. With a standalone DataQL engine, call the same `RuleRegistry.DEFAULT.register(...)` before creating queries.

`RuleRegistry.DEFAULT` is shared. Register once during application startup. Names are case-insensitive; registering the same name replaces the previous rule. Use application-specific names and stateless or thread-safe implementations. Do not store per-request data in rule fields.

## Invoke from a script

Initialize the `people` table from [SQL Execution](../../dataql/sql/execute.md), then run:

```javascript
var find = @@selectSql()<%
    SELECT count(*) FROM people WHERE @{manualEnabled}
%>;
return find();
```

This generates `SELECT count(*) FROM people WHERE enabled = 1`. In Dataway, a SQL-type API accepts the SQL body directly; a DataQL-type API accepts the complete script. See [Dynamic Rules](../../dataql/sql/rules.md) for built-in rules.

## Generate bound values

The `appEquals` rule binds a named argument to an allowed column. Column names come from a fixed application allowlist, while values always use JDBC placeholders.

```java
package com.example.sql;

import java.sql.SQLException;
import java.util.Set;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

public class EqualsRule implements SqlRule {
    private static final Set<String> COLUMNS = Set.of("id", "name", "age");

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String expression) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context,
                            SqlBuilder sql, String expression, String value) throws SQLException {
        String parameter = expression == null ? "" : expression.trim();
        String column = value == null ? "" : value.trim();
        if (!COLUMNS.contains(column)) {
            throw new SQLException("Unsupported filter column: " + column);
        }
        if (!data.hasValue(parameter)) {
            throw new SQLException("Missing filter parameter: " + parameter);
        }
        Object parameterValue = data.getValue(parameter);
        if (parameterValue == null) {
            sql.appendSql(column + " IS NULL");
        } else {
            sql.appendSql(column + " = ?", parameterValue);
        }
    }
}
```

Add this registration to the same initialization callback:

```java
RuleRegistry.DEFAULT.register("appEquals", new EqualsRule());
```

Import `com.example.sql.EqualsRule`. The second rule section names the argument and the third names the column:

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(searchName)<%
    SELECT id, name FROM people WHERE @{appEquals, searchName, name}
%>;
return find('Alice');
```

It produces `WHERE name = ?` with `["Alice"]`. A null input produces `WHERE name IS NULL` without an argument. Quotes inside argument values remain data passed to JDBC.

## Interface and parsing contract

- `test(data, context, expression)`: decides whether to run the rule. The rule defines how expression is interpreted; OGNL evaluation is not automatic.
- `executeRule(data, context, sql, expression, value)`: appends SQL and arguments; may throw `SQLException`.
- `SqlArgSource`: supplies current fragment arguments. The example reads by name. For expressions, use `OgnlUtils.evalOgnl(expression, data)`.
- `QueryContext`: finds rules, shared SQL fragments, type handlers and the class loader.
- `SqlBuilder.appendSql(text, args...)`: appends SQL and arguments. Each appended `?` must correspond to one argument.

The parser uses the first top-level comma to separate the rule name and the second to separate the expression. Remaining content becomes value. Commas inside quotes or nested rules do not split these sections. For `@{appEquals, searchName, name}`, the expression is `searchName` and the value is `name`.

To parse inner parameters and rules, call this inside executeRule:

```java
DynamicParsed.getParsedSql(value).buildQuery(data, context, sql);
```

Import `net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed`. Calling `appendSql(value)` instead appends literal text without parsing; choose the behavior appropriate to the rule. See [Nested Rules](../../dataql/sql/rules/nesting.md).

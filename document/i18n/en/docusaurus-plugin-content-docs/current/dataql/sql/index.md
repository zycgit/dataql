---
id: index
slug: /dataql/sql
title: 6. SQL executor
---

The SQL executor is a DataQL extension module. A fragment declares database statements and parameters, then runs as a function. Query results can be used in expressions and transformations.

:::info Requirements
The application must load `dataql-sqlproc` and provide database connections. See [DataQL engine: SQL executor](../../dataway/dataql-engine/sql.md) for setup. This chapter covers script usage.
:::

```js
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(minAge)<%
    SELECT name, age FROM people WHERE age >= #{minAge} ORDER BY id
%>;
return find(25) => [{"name", "nextAge": age + 1}];
```

Using the `people` table in [SQL execution](execute.md), this returns `[{"name":"Alice","nextAge":26},{"name":"Bob","nextAge":31}]`.

## Guide

- [SQL execution](execute.md): entry points, queries, writes and batch calls.
- [Parameter binding](parameters.md): values, JDBC types and text substitution.
- [Dynamic rules](rules.md) and [XML dynamic SQL](mybaits.md): conditional SQL construction.
- [Results and keys](results.md) and [Pagination and dialects](dialect.md): rows and pages.
- [Type handling](types.md): parameter and result conversions.
- [Transactions](transactions.md): group calls in a transaction.
- [Procedures and multiple results](procedures.md): calls and output parameters.
- [SQL hints](../hints/hint_sql.md): all SQL execution options.

See [Engine extensions](../../dataway/engine/index.md) to add rules, dialects and handlers.

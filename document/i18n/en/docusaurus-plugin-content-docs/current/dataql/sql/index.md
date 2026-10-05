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

- [SQL execution](execute.md): query, update and batch fragment calls.
- [Parameter binding](parameters.md): positional and named parameters, text replacement, rules, options and marker escaping.
- [Dynamic rules](rules.md): SQL generation, collections, branches and nested templates.
- [XML dynamic SQL](mybaits.md): tag attributes, complete examples and generated SQL.
- [Results and keys](results.md), [pagination and dialects](dialect.md): result shapes, keys and page navigation.
- [Type handling](types.md): Java/JDBC mappings, basic types, JSON, binary streams, arrays and vectors.
- [Transactions](transactions.md): commit, rollback, seven propagation modes, isolation and data-source scope.
- [Procedures and multiple results](procedures.md): input/output parameters and named results.
- [SQL hints](../hints/hint_sql.md): execution options.

For custom rules, dialects and handlers, see [engine extensions](../../dataway/engine/index.md).

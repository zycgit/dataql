---
id: rules
title: 6.3 Dynamic Rules
---

Dynamic rules generate SQL conditions, expand collections and create bound values from fragment arguments. They work inside `selectSql`, `insertSql`, `updateSql`, `deleteSql` and `executeSql` fragments.

## Start with a query

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT id, name FROM people WHERE 1 = 1
    @{ifand, name != null and name != '', name = #{name}}
    @{ifand, minAge != null, age >= #{minAge}}
    ORDER BY id
%>;
return find(null, 30);
```

The null name omits the name condition. `minAge = 30` generates `AND age >= ?`, with 30 bound through JDBC. Use the sample table in [SQL Execution](execute.md). For a SQL-type API in Dataway, enter the fragment body directly; its arguments come from the request.

## Syntax

```sql
@{ruleName, content}
@{ruleName, condition, content}
```

Rule names are case-insensitive. Conditions use OGNL and reference names directly, such as `age >= 18`. Use `#{age}` to bind a value inside SQL. These rules belong to SQL fragments; functions and expressions outside the fragment use DataQL syntax.

Content can contain nested rules. Markers inside single-quoted strings, double-quoted text and SQL comments remain literal text.

## Reading guide

- [Statement Generation Rules](rules/statements.md): conditions, collections, branches, macros and generated arguments.
- [Nested Rules](rules/nesting.md): combine rules and understand evaluation order.
- [Custom SQL Rules](../../dataway/engine/sql-rules.md): implement `SqlRule` and register it in Dataway.
- [XML Dynamic SQL](mybaits.md): use tags for longer conditions and loops.

## Processing query results

Dynamic rules run before SQL execution. Configure result unpacking, column-name conversion and `bindOut` through [Results and Keys](results.md); see [Procedures and Multiple Results](procedures.md) for procedure outputs. The SQL executor does not register dbVisitor's `resultSet`, `resultUpdate` or `defaultResult` rules, so those rules cannot be copied into a fragment.

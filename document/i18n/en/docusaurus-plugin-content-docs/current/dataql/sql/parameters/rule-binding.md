---
id: rule-binding
title: 6.2.4 Rule parameters
---

`@{rule, ...}` executes while SQL is generated. Rules handle optional conditions, collection expansion and parameter preprocessing, producing SQL text and bound values.

## Optional conditions

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT name, age FROM people WHERE enabled = 1
    @{and, name = #{name}}
    @{ifand, minAge != null, age >= #{minAge}}
    ORDER BY id
%>;
return find(null, 30);
```

With the [people table](../execute.md#sample-data), the generated SQL is:

```sql
SELECT name, age FROM people WHERE enabled = 1 AND age >= ? ORDER BY id
```

It binds `[30]` and returns `[{"name":"Bob","age":30}]`. `and` omits a condition with only null parameters; `ifand` uses an explicit condition.

## Expand a collection

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(ids)<%
    SELECT name FROM people WHERE 1 = 1
    @{in, AND id IN #{ids}}
    ORDER BY id
%>;
return find([1,2]);
```

This generates `AND id IN (?, ?)`, binding `[1,2]`. A direct `#{ids}` binds one JDBC array, whereas `in` creates a placeholder for each element.

An empty or null collection omits the entire `in` rule, removing the ID filter. To return no rows for an empty list, explicitly generate `AND 1 = 0`; see [statement rules](../rules/statements.md#in).

## Choose a rule

- Conditions: `and`, `or`, or explicit tests with `ifand`, `ifor`.
- Updates: `set`, `ifset`.
- Collections: `in`, `ifin`; use `pairs` or XML `foreach` for templates.
- Branches: `if`, `case`, `when`, `else`.
- Parameter processing: `arg`, `md5`, `uuid32`, `uuid36`.
- Text and reusable fragments: `text`, `iftext`, `macro`, `ifmacro`.

See [dynamic rules](../rules.md) for syntax, null behavior and examples. Rule names are case-insensitive; markers inside SQL strings or comments remain literal.

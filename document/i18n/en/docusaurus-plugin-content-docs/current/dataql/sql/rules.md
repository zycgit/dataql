---
id: rules
title: 6.3 Dynamic rules
---

Plain SQL supports `@{rule, content}` and conditional `@{rule, expression, content}`. Conditions use OGNL; values still use JDBC bindings. Rule names are case-insensitive. Rules inside SQL string literals or comments remain ordinary text.

## Conditions

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT name FROM people WHERE 1 = 1
    @{ifand, name != null and name != '', name = #{name}}
    @{ifand, minAge != null, age >= #{minAge}}
    ORDER BY id
%>;
return find(null, 30);
```

This skips the empty name condition and returns Bob. Use explicit conditions with `ifand` or `ifor` for null filtering.

## Collections

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(ids)<%
    SELECT name FROM people WHERE 1 = 1 @{in, AND id IN #{ids}} ORDER BY id
%>;
return find([1,2]);
```

`in` expands a collection into JDBC placeholders. A null or empty collection removes the entire rule content. Define the intended empty-list behavior before execution to avoid broadening the query.

## Built-in rules

| Rules | Purpose |
| --- | --- |
| `if` | Conditional SQL |
| `ifand`, `ifor`, `ifset` | Conditional SQL with a separator |
| `in`, `ifin` | Collection expansion |
| `text`, `iftext` | Literal SQL text |
| `macro`, `ifmacro` | Reusable SQL fragments |
| `and`, `or`, `set` | Separator for content with one bound argument |
| `arg` | JDBC argument options |
| `uuid32`, `uuid36` | UUID arguments |

The unconditional separator rules restrict argument counts. Use conditional rules or [XML SQL](mybaits.md) for complex conditions. Raw text must be trusted. See [SQL rules](../../dataway/engine/sql-rules.md) and [SQL fragments](../../dataway/engine/sql-macros.md).

The registered `md5` rule currently hashes the parameter wrapper’s text rather than the business value. Compute business-value hashes in the script or application before binding them.

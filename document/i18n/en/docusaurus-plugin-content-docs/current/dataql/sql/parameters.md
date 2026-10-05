---
id: parameters
title: 6.2 Parameter binding
---

Declare parameters on a SQL fragment and supply values when calling it. The executor builds SQL and binds values through JDBC. Parameters support queries, inserts, updates, stored procedures and native commands.

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT id, name, age FROM people
    WHERE name = #{name} AND age >= :minAge
%>;
return find('Alice', 20);
```

Using the [people table](execute.md#sample-data), this produces `WHERE name = ? AND age >= ?`, binds `["Alice",20]` and returns `[{"id":1,"name":"Alice","age":25}]`.

## Choose a parameter form

| Form | Purpose | Example |
| --- | --- | --- |
| `?` | Bind `arg0`, `arg1`, etc. in occurrence order | `WHERE id = ?` |
| `#{expression}` | Bind a value with optional parameter options | `#{name, jdbcType=VARCHAR}` |
| `:expression`, `&expression` | Short named parameters | `:filter.minAge` |
| `${expression}` | Insert text into the SQL structure | `ORDER BY ${column}` |
| `@{rule, ...}` | Generate SQL and parameters conditionally | `@{in, id IN #{ids}}` |

Bind business values. Use text replacement for SQL identifiers chosen from a fixed set. Rules and XML tags can combine these forms.

## Guide

- [Positional parameters](parameters/position.md): names, ordering and repeated values.
- [Named parameters](parameters/named.md): properties, indexes, LIKE queries and nulls.
- [SQL text replacement](parameters/injection.md): identifiers and sorting.
- [Rule parameters](parameters/rule-binding.md): optional conditions and collection expansion.
- [Parameter options](parameter-options.md): JDBC types, handlers and outputs.
- [Parameter marker escaping](parameter-escape.md): literal `?`, `:` and `&` in commands.

## Pass API parameters

```javascript
var find = @@selectSql(id)<%
    SELECT name FROM people WHERE id = #{id}
%>;
return find(${id});
```

Outside the fragment, `${id}` reads the current DataQL parameter. The function call passes it to the SQL fragment, where `#{id}` binds that argument. Inside SQL, `${id}` means text replacement. SQL expressions use OGNL and are evaluated separately from DataQL expressions.

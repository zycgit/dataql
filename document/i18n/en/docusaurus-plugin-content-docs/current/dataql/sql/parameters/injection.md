---
id: injection
title: 6.2.3 SQL text replacement
---

`${expression}` converts a value to text and inserts it into SQL. Use it for table names, column names and sort directions that cannot be bound as JDBC values.

## Dynamic sorting

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(column, direction, minAge)<%
    SELECT id, name, age FROM people
    WHERE age >= #{minAge} ORDER BY ${column} ${direction}
%>;
return find('age', 'DESC', 20);
```

With the [people table](../execute.md#sample-data), this generates:

```sql
SELECT id, name, age FROM people WHERE age >= ? ORDER BY age DESC
```

Only `minAge` is bound, with `[20]`. Results are Bob, then Alice. The example supplies fixed values for the column and direction; an application can select them from an allowed set.

## Compare with binding

| Form | Generated SQL | Parameters |
| --- | --- | --- |
| `name = #{name}`, value `Alice` | `name = ?` | `["Alice"]` |
| `ORDER BY ${column}`, value `age` | `ORDER BY age` | None |
| `ORDER BY #{column}`, value `age` | `ORDER BY ?` | `["age"]`: a value, not an identifier |

Text replacement does not quote, escape or validate SQL identifiers. Choose SQL structure from a fixed application-defined set and bind user-provided business values.

## Dynamic table names

```javascript
var count = @@selectSql(tableName, minAge)<%
    SELECT count(*) FROM ${tableName} WHERE age >= #{minAge}
%>;
return count('people', 25);
```

This generates `SELECT count(*) FROM people WHERE age >= ?`, binds `[25]` and returns `2`. A null replacement writes the text `null`; it does not omit the fragment. Resolve a valid name before calling it.

## Fixed text and reusable fragments

- For a few fixed alternatives, use `case` or `iftext` rules.
- Register [SQL fragments](../../../dataway/engine/sql-macros.md) and reuse them through `macro` or `include`.
- Use [marker escaping](../parameter-escape.md) for literal `?`, `:` and `&` in native commands.

The `${...}` forms above occur inside SQL. Outside a fragment, `${name}` is DataQL parameter access; see the [binding overview](../parameters.md#pass-api-parameters).

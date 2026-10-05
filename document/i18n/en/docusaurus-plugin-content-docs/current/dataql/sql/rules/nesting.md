---
title: 6.3.2 Nested Rules
---

Rule bodies can contain other rules to combine conditions, collections and generated arguments. The outer rule first decides whether to use its body, then parses inner rules. `text` and `iftext` emit literal content without executing inner rules.

## Conditions and collections

This query expands an IN clause when the list is nonempty. Use the `people` table from [SQL Execution](../execute.md).

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(ids)<%
    SELECT id, name FROM people WHERE enabled = 1
    @{ifand, ids != null and ids.size() > 0, id IN @{in, #{ids}}}
    ORDER BY id
%>;
return find([1, 2]);
```

It produces `WHERE enabled = 1 AND id IN (?, ?)`, with `[1, 2]`. `ifand` checks the list, `in` expands placeholders, then `ifand` adds AND. An empty list leaves only `enabled = 1`; handle empty input before querying when it should return no rows.

Alternatively, place the complete condition inside `in` and let `and` manage its connector:

```sql
SELECT * FROM people WHERE enabled = 1
@{and, @{in, id IN #{ids}}}
```

An empty list makes the inner SQL empty and the outer rule emits nothing. With `@{and, id IN @{in, #{ids}}}`, an all-null list also omits the condition because all generated arguments are null. Use `ifand` for explicit control.

## Grouped conditions

Connector rules preserve SQL AND/OR precedence. Parentheses group optional conditions:

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT id, name FROM people WHERE enabled = 1
    @{if, name != null or minAge != null,
        AND (1 = 0
            @{or, name = #{name}}
            @{or, age >= #{minAge}}
        )
    }
    ORDER BY id
%>;
return find('Alice', 30);
```

This produces `WHERE enabled = 1 AND (1 = 0 OR name = ? OR age >= ?)`, with `["Alice", 30]`. When both arguments are null, the outer if skips the whole group, avoiding empty parentheses.

## Branches and collections

`case` runs only the first selected branch. An explicit empty-list branch guarantees an empty result:

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(ids)<%
    SELECT id, name FROM people WHERE enabled = 1
    @{case, ,
        @{when, ids != null and ids.size() > 0,
            @{in, AND id IN #{ids}}
        }
        @{else, AND 1 = 0}
    }
    ORDER BY id
%>;
return find([]);
```

An empty list generates `AND 1 = 0` and returns `[]`. Input `[1]` generates `AND id IN (?)`, binding 1. Unselected branches do not generate parameters or look up macros.

## Loops and separators

Each pairs template can contain conditional rules. Here the iteration index controls `UNION ALL`:

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var rows = @@selectSql(names)<%
    @{pairs, #{names},
        @{iftext, i > 0, UNION ALL}
        SELECT CAST(#{v} AS VARCHAR) AS name
    }
%>;
return rows(['Alice', 'Bob']);
```

`CAST` supplies an explicit VARCHAR type for parameters in the UNION query.

Generated SQL is `SELECT CAST(? AS VARCHAR) AS name UNION ALL SELECT CAST(? AS VARCHAR) AS name`, returning `[{"name":"Alice"},{"name":"Bob"}]`. Empty input generates no SQL, so skip this call in the script for an empty list.

## Generated arguments and null checks

```sql
SELECT * FROM payload_log WHERE 1 = 1
@{ifand, content != null, digest = @{md5, #{content}}}
```

With `content = "abc"`, MD5 generates a digest argument and the outer rule emits `AND digest = ?`. A null content fails the outer condition, so MD5 is not evaluated.

With `@{and, digest = @{md5, #{content}}}`, MD5 first converts null to the empty-string digest. The outer and sees a non-null digest and retains the condition. Use if/ifand to check the original input.

## Boundaries

- Rules inside SQL strings or comments are not executed. For example, `'@{uuid32}'` is a string literal.
- `text` and `iftext` do not parse placeholders or inner rules. Use `if` when parsing is required.
- Prefer named parameters in dynamic rules so nested conditions and branches can share them clearly.
- Rules control their own content. They cannot remove dangling AND keywords, commas or parentheses outside that content. Put an optional connector and its SQL in the same conditional branch.

---
title: 6.3.1 Statement Generation Rules
---

Examples show fragment bodies, generated SQL and arguments in placeholder order. Whitespace in generated SQL is normalized. See [Dynamic Rules](../rules.md) for a complete DataQL invocation.

## AND and IFAND {#and}

`@{and, condition}` chooses a condition based on its bound values, then adds `WHERE` or `AND`. `@{ifand, OGNL expression, condition}` uses an explicit expression.

```sql
SELECT * FROM people WHERE enabled = 1
@{and, name = #{name}}
@{ifand, minAge != null and minAge >= 0, age >= #{minAge}}
```

With `name = "Alice"` and `minAge = 18`:

```sql
SELECT * FROM people WHERE enabled = 1 AND name = ? AND age >= ?
-- Arguments: ["Alice", 18]
```

- `and` omits content when all bound values are null, or when there are no bound arguments. Empty strings, zero and false are non-null values.
- If any argument is non-null, the whole content and all its other arguments are retained. For example, `@{and, age BETWEEN #{low} AND #{high}}` retains both placeholders for `low = 18, high = null`.
- Content that directly contains `${...}` text substitution is retained even if all bound values are null. See [SQL Parameters](../parameters.md).
- `ifand` includes its content whenever its condition is true, even if it has no parameters or null arguments. Check empty strings explicitly with `name != null and name != ''`.

The rule checks whether preceding SQL contains `where`, then inspects trailing keywords such as `where`, `and` and `or` before adding a connector. Write `name = #{name}` in the body without a leading `AND`. For complex subqueries, use an explicit `WHERE 1 = 1` and append optional conditions.

Use `@{ifand, true, enabled = 1}` or plain SQL for fixed conditions. `@{and, enabled = 1}` has no bound value and is omitted.

## OR and IFOR {#or}

`or` and `ifor` follow the same null rules as `and` and `ifand`, using `OR` as the connector.

```sql
SELECT * FROM people WHERE name = #{name}
@{or, id = #{id}}
```

With `name = "Alice"` and `id = 2`:

```sql
SELECT * FROM people WHERE name = ? OR id = ?
-- Arguments: ["Alice", 2]
```

A null id leaves only the name condition. For explicit control use `@{ifor, id != null, id = #{id}}`. SQL AND/OR precedence remains unchanged; add parentheses for grouped conditions as shown in [Nested Rules](nesting.md).

## SET and IFSET {#set}

`@{set, assignment}` adds `SET` or a separating comma and retains null assignments. `@{ifset, condition, assignment}` includes an assignment only when its condition is true.

```sql
UPDATE people
@{set, name = #{name}}
@{ifset, changeAge, age = #{age}}
WHERE id = #{id}
```

With `name = "Alice"`, `changeAge = true`, `age = null` and `id = 1`:

```sql
UPDATE people SET name = ?, age = ? WHERE id = ?
-- Arguments: ["Alice", null, 1]
```

Setting `changeAge = false` leaves age unchanged. `set` supports clearing a column to NULL; use `ifset` when updates should ignore absent values.

Rules add commas before assignments. Do not append commas after rules. If every conditional assignment is omitted, no valid UPDATE is produced automatically; retain a required assignment or skip the update in DataQL.

## IF, TEXT and IFTEXT {#if}

`@{if, condition, content}` parses parameters and nested rules when the condition is true. It does not add SQL connectors.

```sql
SELECT * FROM people WHERE 1 = 1
@{if, name != null, AND name = #{name}}
@{iftext, newest, ORDER BY id DESC}
```

With `name = "Alice"` and `newest = true`:

```sql
SELECT * FROM people WHERE 1 = 1 AND name = ? ORDER BY id DESC
-- Arguments: ["Alice"]
```

`text` always emits literal content; `iftext` emits literal content after a condition check:

```sql
SELECT * FROM people @{text, ORDER BY id ASC}
```

This generates `SELECT * FROM people ORDER BY id ASC`, with no arguments. Neither `text` nor `iftext` parses `#{...}`, `${...}` or nested rules inside its body. Use `if` when parameters must be bound. Literal rules are useful for fixed keywords written in the script; they do not convert request data into SQL.

An empty condition activates any `if*` rule. A nonempty condition must evaluate to boolean true. The number 1 and string `"true"` are not boolean true.

## IN and IFIN {#in}

`@{in, SQL fragment}` expands one bound argument into a parenthesized placeholder list. `ifin` checks a condition first. Neither rule adds AND or OR.

```sql
SELECT * FROM people WHERE enabled = 1
@{in, AND id IN #{ids}}
```

With `ids = [1, 2, 3]`:

```sql
SELECT * FROM people WHERE enabled = 1 AND id IN (?, ?, ?)
-- Arguments: [1, 2, 3]
```

The conditional form is `@{ifin, ids != null and ids.size() > 0, AND id IN #{ids}}`. Collections and arrays expand; a non-null scalar produces one placeholder. JDBC argument options, such as `#{ids, jdbcType=INTEGER}`, are propagated to each element.

- A null or empty collection omits the entire rule; other conditions still execute.
- Null elements remain bound values and follow database NULL comparison semantics.
- The body must produce exactly one bound argument. No arguments emit nothing; multiple arguments cause an exception.
- Do not add parentheses around `#{ids}`; the rule supplies them.

If an empty list should return no rows, return an empty list in DataQL before executing SQL, or use a branch that emits `AND 1 = 0`. Omitting a filter does not mean an empty result.

## CASE, WHEN and ELSE {#case}

`case` selects the first matching branch during SQL generation. Only that branch is sent to the database.

Leave the case expression empty for condition mode:

```sql
SELECT * FROM people WHERE 1 = 1
@{case, ,
    @{when, name != null, AND name = #{name}}
    @{when, minAge != null, AND age >= #{minAge}}
    @{else, AND enabled = 1}
}
```

With `name = "Alice", minAge = 18`, only `AND name = ?` is emitted, with `["Alice"]`. If both are null, the output is `AND enabled = 1`, without arguments.

Value mode compares the case expression with each when value:

```sql
SELECT * FROM people
@{case, sort,
    @{when, 'name', ORDER BY name, id}
    @{when, 'age', ORDER BY age, id}
    @{else, ORDER BY id}
}
```

`sort = "age"` produces `SELECT * FROM people ORDER BY age, id`. Branch values are OGNL expressions; quote constant strings. Value mode compares equality, then falls back to string representations, so numeric 1 can match string `"1"`.

Place `else` last. No match and no else produces no content. `when` and `else` require a surrounding case. The case processes only direct child when/else rules; put SQL inside those branches. Branch bodies support further nesting.

## MACRO and IFMACRO {#macro}

`macro` references an application-registered SQL fragment. Suppose `activePeople` contains `AND enabled = 1`:

```sql
SELECT * FROM people WHERE age >= #{minAge}
@{macro, activePeople}
```

With `minAge = 18`, the generated SQL is `SELECT * FROM people WHERE age >= ? AND enabled = 1`, with `[18]`. Use `@{ifmacro, activeOnly, activePeople}` for conditional inclusion; a false condition skips macro lookup.

Write the registered name directly, without quotes or `#{...}`. The macro parses its parameters and rules using the current fragment's arguments. Missing macros cause an execution error. See [SQL Fragments](../../../dataway/engine/sql-macros.md) for registration and XML inclusion.

## MD5 {#md5}

`md5` converts one bound argument to text, hashes it and binds the digest as VARCHAR.

```sql
INSERT INTO payload_log(digest) VALUES (@{md5, #{content}})
```

With `content = "abc"`:

```sql
INSERT INTO payload_log(digest) VALUES (?)
-- Arguments: ["900150983cd24fb0d6963f7d28e17f72"]
```

Null is hashed as an empty string, producing `d41d8cd98f00b204e9800998ecf8427e`. The body must produce exactly one bound argument; a bare name does not bind an argument. This rule supports existing digest fields; MD5 is unsuitable for new password storage designs.

## UUID32 and UUID36 {#uuid}

Both rules produce a new VARCHAR argument: `uuid32` has 32 characters without hyphens, while `uuid36` has 36 characters including hyphens.

```sql
INSERT INTO event_log(id, correlation_id, message)
VALUES (@{uuid32}, @{uuid36}, #{message})
```

The output is `VALUES (?, ?, ?)`, binding two new UUIDs followed by message. UUIDs are generated on each execution and require no business arguments. Generated values are not automatically written back to DataQL variables.

## ARG {#arg}

Use `#{...}` for normal argument configuration. `arg` exposes the corresponding rule form, with an empty second section and the expression/options in the third:

```sql
SELECT * FROM people WHERE id = @{arg, , id, jdbcType=INTEGER}
```

With `id = 1`, this produces `WHERE id = ?` with a JDBC INTEGER argument, equivalent to `#{id, jdbcType=INTEGER}`. Null still creates a placeholder; specifying the JDBC type helps drivers bind null. See [Parameter Options](../parameter-options.md) for `mode`, `name`, `scale`, `typeName` and `typeHandler`.

## PAIRS {#pairs}

`@{pairs, #{collection}, template}` iterates objects, lists or arrays using fixed local names:

- `k`: object key, or list/array index as a string.
- `v`: current value.
- `i`: zero-based integer iteration index.

These variables shadow names only within the current template; other outer arguments remain available. Iterations follow object iteration or list order and are separated by whitespace. Commas are **not** added automatically.

```sql
@{pairs, #{names},
    @{iftext, i > 0, UNION ALL}
    SELECT CAST(#{v} AS VARCHAR) AS name
}
```

`CAST` supplies an explicit VARCHAR type for parameters in the UNION query.

With `names = ["Alice", "Bob"]`:

```sql
SELECT CAST(? AS VARCHAR) AS name UNION ALL SELECT CAST(? AS VARCHAR) AS name
-- Arguments: ["Alice", "Bob"]
```

For commas, emit `@{iftext, i > 0, ,}` in the template, or use [foreach](../mybaits.md) with `separator`. Null and empty collections emit nothing. Other scalar values cause an error. Bind values using `#{v}`; any dynamic column names must come from an application allowlist.

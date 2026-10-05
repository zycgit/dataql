---
id: position
title: 6.2.1 Positional parameters
---

Each `?` binds one value, reading fragment arguments named `arg0`, `arg1`, etc. from left to right. Examples use the [people table](../execute.md#sample-data).

## Bind in order

```javascript
var find = @@selectSql(arg0, arg1)<%
    SELECT name FROM people WHERE age >= ? AND age < ?
%>;
return find(20, 30);
```

The two placeholders bind `[20,30]`; the result is `Alice`. Declare arguments as `arg0`, `arg1`. With names such as `minAge`, use `#{minAge}` instead.

## Repeat a value

Each placeholder has its own position:

```javascript
var find = @@selectSql(arg0, arg1)<%
    SELECT count(*) FROM people WHERE age >= ? AND age <= ?
%>;
return find(25, 25);
```

This binds `[25,25]` and returns `1`. [Named parameters](named.md) let you repeat `#{age}` without passing the value twice.

## Add options

A bare `?` cannot carry options. Refer to the argument by name:

```javascript
var find = @@selectSql(arg0)<%
    SELECT name FROM people WHERE id = #{arg0, jdbcType=BIGINT}
%>;
return find(1);
```

The result is `Alice`. See [parameter options](../parameter-options.md) for `jdbcType` and `typeHandler`.

## Boundaries

- Question marks inside quotes or SQL comments remain literal.
- One placeholder binds one value. A list binds as one JDBC array; use `in` or `foreach` to expand it.
- Dynamic rules, tags and shared fragments parse their SQL separately. Use named parameters within them to avoid relying on positional numbering across branches.
- Escape a literal question mark as `\?`; see [marker escaping](../parameter-escape.md).

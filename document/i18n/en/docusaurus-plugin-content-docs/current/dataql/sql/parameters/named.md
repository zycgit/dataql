---
id: named
title: 6.2.2 Named parameters
---

`#{name}`, `:name` and `&name` read the named argument and generate JDBC `?`. The braced form also accepts expressions and parameter options. Examples use the [people table](../execute.md#sample-data).

## Bind and reuse

```javascript
var find = @@selectSql(age)<%
    SELECT name FROM people WHERE age >= #{age} AND age <= :age
%>;
return find(25);
```

This produces `WHERE age >= ? AND age <= ?`, binds `[25,25]` and returns `Alice`. SQL occurrence order determines binding order; declaration order determines how function arguments are received.

`&name` is also value binding. In XML, write `&amp;name`.

## Properties and indexes

```javascript
var find = @@selectSql(filter, ids)<%
    SELECT name FROM people
    WHERE age >= #{filter.minAge} AND id = #{ids[0]}
%>;
return find({'minAge':20}, [1,2]);
```

This binds `[20,1]` and returns `Alice`. Use dot notation for properties, zero-based indexes for lists, and `#{filter['min-age']}` for special map keys.

`#{ids}` binds the entire list. `IN (#{ids})` does not expand it into individual placeholders. Use a [collection rule](../rules.md) or [foreach](../mybaits.md).

## Special property names

OGNL treats `size`, `keys`, `keySet`, `values`, and `isEmpty` as special properties of the parameter collection. Use bracket access for business parameters with these names, and forms such as `filter['size']` for nested objects:

```javascript
var find = @@selectSql(values)<%
    SELECT name FROM people WHERE id = #{['values']}
%>;
return find(1);
```

This binds the business parameter `values` as `1` and returns `Alice`. Writing `#{values}` instead reads the collection of all fragment parameter values.

## LIKE queries

Include wildcards in the bound value:

```javascript
var find = @@selectSql(keyword)<%
    SELECT name FROM people WHERE name LIKE #{'%' + keyword + '%'}
%>;
return find('Ali');
```

This generates `WHERE name LIKE ?`, binds `%Ali%` and returns `Alice`. `'%#{keyword}%'` is a SQL string literal and does not bind the parameter.

## Expressions

Braced parameters accept OGNL property access, string methods and calculations:

```javascript
var find = @@selectSql(minAge, name)<%
    SELECT name FROM people
    WHERE age >= #{minAge + 1} AND name = #{name.trim()}
%>;
return find(24, ' Alice ');
```

This binds `[25,"Alice"]` and returns `Alice`. Use braces for expressions with operators, spaces or method calls. Short forms are suitable for names and property paths.

Expressions read fragment arguments. Pass API parameters into the fragment explicitly; SQL expressions do not receive the entire HTTP request.

## Nulls and missing values

Binding does not remove SQL conditions. With null, `name = #{name}` still generates `name = ?`; equality with SQL NULL does not match null rows.

```javascript
var find = @@selectSql(name)<%
    SELECT count(*) FROM people WHERE 1 = 1
    @{if, name != null, AND name = #{name}}
    @{if, name == null, AND name IS NULL}
%>;
return find(null);
```

This explicitly selects an equality or `IS NULL` condition. Use [dynamic rules](../rules.md) to omit optional conditions, and `#{name, jdbcType=VARCHAR}` when assigning a typed null.

Validate required values before calling the fragment. Null property paths and invalid indexes can fail during expression evaluation.

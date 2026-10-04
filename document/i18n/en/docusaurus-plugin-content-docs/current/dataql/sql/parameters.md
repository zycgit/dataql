---
id: parameters
title: 6.2 Parameter binding
---

Fragment arguments form the SQL parameter context. Expressions can access object properties and collection elements.

## Named parameters

```javascript
var find = @@selectSql(filter)<%
    SELECT name FROM people
    WHERE id = #{filter.id, jdbcType=BIGINT} AND age >= :filter.minAge
%>;
return find({'id':1, 'minAge':20});
```

Both `#{filter.id}` and `:filter.minAge` become JDBC `?` bindings.

| Syntax | Meaning |
| --- | --- |
| `#{name}`, `:name` | Bind a value or property |
| `#{ids[0]}` | Bind a collection element |
| `?` | Read fragment arguments `arg0`, `arg1`, etc. in order |
| `${name}`, `&name` | Substitute raw SQL text |

Positional placeholders require matching argument names:

```javascript
var find = @@selectSql(arg0)<% SELECT name FROM people WHERE id = ? %>;
return find(1);
```

Use bindings for business values. Raw substitution is appropriate only for trusted, allowlisted identifiers such as table and column names.

## JDBC options

```sql
WHERE id = #{id, jdbcType=BIGINT}
```

| Option | Purpose |
| --- | --- |
| `jdbcType` | JDBC type name or numeric code |
| `typeHandler` | Fully qualified handler class |
| `mode` | `IN`, `OUT`, `INOUT` |
| `name` | Output parameter name |
| `typeName` | Database type name for output registration |
| `scale` | Numeric output scale |

Specify the JDBC type for null values. `javaType` is not implemented as a placeholder option. See [SQL type handlers](../../dataway/engine/sql-types.md).

## Script parameters

```javascript
var find = @@selectSql(id)<% SELECT name FROM people WHERE id = #{id} %>;
return find(${id});
```

DataQL resolves `${id}` before calling the fragment. Within the fragment use SQL bindings. See [Custom scopes](../../dataway/engine/scope.md).

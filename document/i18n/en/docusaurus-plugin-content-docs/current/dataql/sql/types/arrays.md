---
id: arrays
title: 6.7.5 Array Handlers
---

`ArrayTypeHandler` binds a DataQL list as one JDBC ARRAY parameter and converts array results into script lists. The database and driver must support `createArrayOf`, `setArray` and `getArray`.

## Bind a list

This script runs on H2:

```javascript
var query = @@selectSql(items)<%
    SELECT CAST(#{items, jdbcType=ARRAY} AS INTEGER ARRAY)
%>;
return query([1, 2, 3]);
```

The result is `[1,2,3]`. Non-null lists select the array handler even without `jdbcType=ARRAY`; keep the option when binding null.

## Element types

| Elements | Inferred type |
| --- | --- |
| Boolean | BOOLEAN |
| Byte / Short / Integer / Long | TINYINT / SMALLINT / INTEGER / BIGINT |
| Float / Double | FLOAT / DOUBLE |
| BigInteger / BigDecimal | NUMERIC |
| String | VARCHAR |

Nulls preserve their positions but do not determine the type. Mixed integer representations widen to BIGINT, mixed floating-point values to DOUBLE, and mixtures with high-precision numbers to NUMERIC. Mixed strings/numbers fail. Maps and nested lists cannot be inferred by the general write handler.

```javascript
var query = @@selectSql(items)<%
    SELECT CAST(#{items, jdbcType=ARRAY} AS DOUBLE ARRAY)
%>;
return query([1.25, null, 2]);
```

The result is `[1.25,null,2.0]`; DOUBLE arrays retain double precision.

## Empty lists and null

- `null` is a SQL NULL array.
- `[]` is a zero-element array.
- `[null,null]` contains two null elements.

Empty and all-null lists cannot determine an element type. The generic handler requests a JAVA_OBJECT array. H2 can handle it with a CAST; other drivers may reject that type before SQL CAST runs. Use a handler with an explicit element type in that case.

PostgreSQL's `PgArrayTypeHandler` accepts an element name, such as `new PgArrayTypeHandler("int4", 1)`. It has no no-argument constructor, so its class name alone is insufficient in a parameter option. Supply it through registration or a no-argument subclass; see [custom handlers](../../../dataway/engine/sql-types.md#postgres-array).

## ARRAY versus IN

ARRAY binds a list as one database array. To expand a list into several placeholders for an IN condition, use the IN rule:

```javascript
var find = @@selectSql(ids)<%
    SELECT id, name FROM people WHERE 1 = 1 @{in, AND id IN #{ids}}
%>;
return find([1, 2]);
```

This produces several `?` parameters and needs no ARRAY support. See [SQL rules](../rules.md) for expansion and empty-list behavior.

## Results and resources

Reading extracts values and frees the JDBC Array. DataQL receives a list detached from the connection. Offset date/time elements become ISO text; regular date/time values become epoch milliseconds. Supported result shapes depend on the driver and do not imply generic multidimensional array writing.

Arrays created by the handler are freed after binding. A JDBC Array explicitly supplied by an application remains the application's responsibility.

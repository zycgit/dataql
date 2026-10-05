---
id: mappings
title: 6.7.1 Java/JDBC Mappings
---

DataQL converts function arguments into script values before handing fragment parameters to SQL. Handlers normally receive strings, numbers, booleans, Maps, Lists or `BinaryModel`, rather than the original application entity.

## Default parameter types

The concrete Java numeric type depends on the literal, calculation or supplied value.

| SQL parameter value | Default JDBC type | Behavior |
| --- | --- | --- |
| Boolean | BIT | Boolean binding |
| Byte / Short / Integer / Long | TINYINT / SMALLINT / INTEGER / BIGINT | Integer binding |
| Float / Double | FLOAT / DOUBLE | Floating-point binding |
| BigInteger / BigDecimal | BIGINT / DECIMAL | Handlers bind large numbers through BigDecimal; column precision must be sufficient |
| String | VARCHAR | Date text needs an explicit date JDBC type |
| List | ARRAY | Non-null elements determine the element type; requires database array support |
| BinaryModel | BLOB | Includes uploads; VARBINARY and other binary types can be explicit |
| Map | No general object serialization | Explicitly select JsonTypeHandler to store JSON |
| null | No runtime type | Supply jdbcType for the destination column |

SQL execution also accepts Java byte arrays and JDBC date values directly. Through ordinary DataQL calls, however, Java arrays become lists, dates become epoch milliseconds, and enums become names. Application UDFs should return `BinaryModel` for binary content; see [streams and binary values](binary.md).

## Explicit JDBC types

```javascript
var query = @@selectSql(name, createdAt)<%
    SELECT CAST(#{name, jdbcType=VARCHAR} AS VARCHAR) AS "name",
           CAST(#{createdAt, jdbcType=TIMESTAMP} AS TIMESTAMP) AS "createdAt"
%>;
return query(null, '2026-10-05 12:34:56');
```

This H2 query binds the first value as SQL NULL and parses the second as a timestamp. `jdbcType` accepts JDBC names or integer codes, such as `VARCHAR` or `12`. It does not coerce every value into the requested type: the selected handler and driver determine supported conversions. There is no `javaType` parameter option.

## Selection order

Parameter binding uses this order:

1. An explicit `typeHandler`.
2. For a non-null value with `jdbcType`, the exact Java/JDBC combination, then a compatible superclass/interface combination.
3. The Java type default, including registered parent types and array handling.
4. For null with `jdbcType`, the JDBC type handler.
5. Generic JDBC object binding when no handler is available.

A TIMESTAMP cross mapping converts string and numeric parameters to timestamps. Registering a JDBC-only mapping does not override all existing Java mappings for non-null values.

Result reading considers the JDBC column type and the Java class reported by the driver. It checks combination mappings, Java mappings, then JDBC mappings. Missing class metadata or a generic Object handler falls back to JDBC. MySQL YEAR uses integers; Oracle-specific classes use their corresponding JDBC types.

## Enums

Java enums become the string returned by `name()` before SQL binding. `Status.ACTIVE` becomes `"ACTIVE"`, not its ordinal or an application code.

```javascript
var query = @@selectSql(status)<%
    SELECT id, name FROM people WHERE status = #{status}
%>;
return query('ACTIVE');
```

This requires a text `status` column. If a database stores numeric status codes, map the name in the script or use a [custom handler](../../../dataway/engine/sql-types.md). Results remain strings or numbers whose meaning is defined by the script.

## Spatial data

No Geometry or Point Java handler is built in. Use database conversion functions, for example PostgreSQL with PostGIS installed:

```javascript
var query = @@selectSql(point)<%
    SELECT ST_AsText(ST_GeomFromText(#{point}, 4326))
%>;
return query('POINT(120.15 30.28)');
```

The result is the WKT string `POINT(120.15 30.28)`. WKB can use database binary functions with `BinaryModel`. A proprietary JDBC spatial object requires an application handler.

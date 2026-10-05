---
id: types
title: 6.7 Type Handling
---

Type handlers bind DataQL values to JDBC parameters and convert result columns back into script values. Common strings, numbers, booleans and dates have default mappings. JSON, vectors and database-specific formats can use explicit handlers.

```javascript
var query = @@selectSql(createdAt)<%
    SELECT CAST(#{createdAt, jdbcType=TIMESTAMP} AS TIMESTAMP)
%>;
return query('2026-10-05 12:34:56');
```

This H2 example binds a string as TIMESTAMP and returns epoch milliseconds. `jdbcType` selects the JDBC type; `typeHandler` can select a specific conversion implementation.

## Guides

- [Java/JDBC mappings](types/mappings.md): script values, selection order, enums and spatial data.
- [Basic handlers](types/basic.md): strings, numbers, booleans, dates, times and XML.
- [JSON serialization](types/json.md): store objects and lists as JSON and parse query results.
- [Streams and binary values](types/binary.md): uploaded files, BLOB queries and resource ownership.
- [Array handlers](types/arrays.md): bind a list as JDBC ARRAY and distinguish arrays from IN expansion.
- [Vector handlers](types/vectors.md): PostgreSQL pgvector and ClickHouse Float32 arrays.
- [Custom type handlers](../../dataway/engine/sql-types.md): implement converters and register them in Dataway.

## Scope

SQL parameters have already passed through the DataQL data model. Java enums become their names, ordinary Java arrays become lists, and business objects become field objects. Entity annotations and ORM property mappings are not part of this execution path. There is no built-in GIS object handler; use database WKT/WKB functions or an application handler.

A conversion handler does not add support for a column type to a database. Examples identify the database they target; use the SQL syntax and JDBC types supported by your driver.

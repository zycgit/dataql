---
id: types
title: 6.7 Type handling
---

The SQL executor uses type handlers to write JDBC parameters and read result columns. Common types convert automatically; see [SQL type handlers](../../dataway/engine/sql-types.md) for custom implementations and registration.

## Built-in conversion

Handlers cover strings, booleans, numbers, dates and times, byte arrays, JDBC Blob and Clob. Parameter selection uses the runtime Java type; result selection uses the column type reported by the driver.

DataQL converts some Java values into its script data model, so Java handler support does not imply every Java type is preserved in scripts. Default enum reads and writes are currently unimplemented; convert enum values to strings first.

## Parameter type options

```javascript
var add = @@insertSql(name)<%
    INSERT INTO people(name, age) VALUES (#{name, jdbcType=VARCHAR}, 20)
%>;
return add('carol');
```

`jdbcType` supplies a JDBC type to the handler. It accepts a type name such as `VARCHAR` or `BIGINT`, or its numeric code.

When the application supplies a custom handler, select it for a parameter with `typeHandler`:

```sql
VALUES (#{name, jdbcType=VARCHAR, typeHandler=com.example.sql.UpperTextHandler}, 20)
```

The complete `UpperTextHandler` example is in [SQL type handlers](../../dataway/engine/sql-types.md). This option affects parameter writes, not result reads.

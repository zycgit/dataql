---
id: parameter-options
title: 6.2.5 Parameter options
---

Use `#{expression, option=value}` to configure one binding. Separate options with commas. Option names, JDBC type names and `mode` values are case-insensitive; handler names are fully qualified Java class names. Option values are unquoted.

```sql
WHERE created_at >= #{since, jdbcType=TIMESTAMP}
```

These options apply to one placeholder. See [SQL hints](../hints/hint_sql.md) for connection selection, timeouts and pagination.

## Options

| Option | Value | Purpose |
| --- | --- | --- |
| `jdbcType` | JDBC name or integer code | Database type, such as `VARCHAR`, `TIMESTAMP`, `12` |
| `typeHandler` | Fully qualified handler class | Conversion for this parameter |
| `mode` | `IN`, `OUT`, `INOUT`, `CURSOR` | Direction; defaults to input |
| `name` | Output name | Key used in the output object |
| `typeName` | Database type name | Named type passed to JDBC output registration |
| `scale` | Integer | Scale for numeric output registration |

Handlers are selected from actual DataQL values. The SQL executor has no `javaType`, entity mapping or `rowMapper` parameter options.

## jdbcType

A date input can be text or epoch milliseconds. Select its handler with `jdbcType`:

```javascript
var read = @@selectSql(value)<%
    SELECT CAST(#{value, jdbcType=DATE} AS DATE)
%>;
return read('2026-10-05');
```

The handler uses JDBC date binding; DataQL receives epoch milliseconds. Common types include `VARCHAR`, `INTEGER` (also `INT`), `BIGINT`, `DECIMAL`, `DATE`, `TIME`, `TIMESTAMP`, `BLOB`, and `ARRAY`. `jdbcType=VARCHAR` and `jdbcType=12` are equivalent.

Specify a type when writing null:

```javascript
var clear = @@updateSql(id, name)<%
    UPDATE people SET name = #{name, jdbcType=VARCHAR} WHERE id = #{id}
%>;
return clear(1, null);
```

This assigns SQL NULL and returns an update count of `1`.

## typeHandler

This H2 example serializes a DataQL object to JSON text:

```javascript
var encode = @@selectSql(document)<%
    SELECT CAST(#{document,
        jdbcType=VARCHAR,
        typeHandler=net.hasor.dataql.sqlproc.types.json.JsonTypeHandler} AS VARCHAR)
%>;
return encode({'name':'Alice','tags':['java','dataql']});
```

It returns the JSON string `{"name":"Alice","tags":["java","dataql"]}`. An explicit handler takes precedence for this binding, but does not change result-column conversion. Configure a corresponding result mapping to read an object; see [SQL type handlers](../../dataway/engine/sql-types.md).

The class must be on the application classpath. A public no-argument constructor supports class-name creation. Loading or construction failures fail the SQL call.

## mode and name

`callSql` and `callXml` require a driver supporting stored procedures. Assume the database provides `add_one(IN input_value INT, OUT output_value INT)` and increments its input:

```javascript
hint bindOut = 'answer';
var calculate = @@callSql(value)<%
    {call add_one(
        #{value, mode=IN, jdbcType=INTEGER},
        #{output, mode=OUT, jdbcType=INTEGER, name=answer}
    )}
%>;
return calculate(41);
```

The expected result is `{"answer":42}`. `OUT` does not read an input value, so `output` needs no fragment argument. `name` assigns the output key, and `bindOut` selects outputs to return. Without `name`, the expression name is used.

`INOUT` takes an initial value:

```javascript
hint bindOut = 'answer';
var calculate = @@callSql(value)<%
    {call increment_value(#{value, mode=INOUT, jdbcType=INTEGER, name=answer})}
%>;
return calculate(41);
```

This requires an existing `increment_value(INOUT value INT)` procedure. Non-cursor output parameters need `jdbcType`. See [stored procedures](procedures.md) for a database procedure definition and multiple results.

## typeName and scale

Use a database type name for named output types or a scale for numeric outputs:

```sql
#{total, mode=OUT, jdbcType=DECIMAL, scale=2}
#{value, mode=OUT, jdbcType=STRUCT, typeName=APP.ADDRESS_TYPE}
```

These configure JDBC output registration. When both are present, `typeName` takes precedence over `scale`; without either, registration uses only `jdbcType`. Database-specific objects still require a matching read handler.

## CURSOR

Assume Oracle provides `find_people`, accepting a minimum age and returning a `SYS_REFCURSOR`:

```javascript
hint bindOut = 'rows';
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@callSql(minAge)<%
    {call find_people(#{minAge}, #{rows, mode=CURSOR})}
%>;
return find(25);
```

The executor registers the cursor for the driver and reads it as a query result. `off` preserves the row list, producing `{"rows":[...]}`. This requires a database supporting cursor outputs; the H2 example environment cannot validate Oracle procedures.

## ARG rule

`@{arg, , expression, options}` and `#{expression, options}` both create one binding:

```sql
WHERE age >= @{arg, , minAge, jdbcType=INTEGER}
```

Keep the empty position after the rule name; the expression and options belong to the rule body. This generates `WHERE age >= ?`. The braced parameter form is sufficient for ordinary use.

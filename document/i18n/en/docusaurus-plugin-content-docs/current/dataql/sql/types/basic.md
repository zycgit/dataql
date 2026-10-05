---
id: basic
title: 6.7.2 Basic Type Handlers
---

Built-in handlers are in subpackages of `net.hasor.dataql.sqlproc.types`. Standard mappings work automatically; optional conversions use `typeHandler` or application registration.

## Strings

| Handler | JDBC types | Script result |
| --- | --- | --- |
| string.StringTypeHandler | CHAR, VARCHAR, LONGVARCHAR | Full string |
| string.NStringTypeHandler | NCHAR, NVARCHAR, LONGNVARCHAR | Unicode string |
| string.ClobAsStringTypeHandler | CLOB | Full text |
| string.NClobAsStringTypeHandler | NCLOB | Full Unicode text |
| string.SqlXmlTypeHandler | SQLXML | XML text |

CHAR and NCHAR return the full field. Fixed-width padding and empty-string storage follow database behavior. SQL NULL becomes null.

```javascript
var query = @@selectSql(text)<%
    SELECT CAST(#{text, jdbcType=NCHAR} AS NCHAR(4))
%>;
return query('中文测试');
```

H2 returns `"中文测试"`. CLOB, NCLOB and SQLXML are materialized before the query finishes; scripts do not receive JDBC Readers.

## Numbers and booleans

| Handler | Default JDBC types | Behavior |
| --- | --- | --- |
| bool.BooleanTypeHandler | BIT, BOOLEAN | Boolean |
| number.ByteTypeHandler / ShortTypeHandler | TINYINT, SMALLINT | Small integer |
| number.IntegerTypeHandler / LongTypeHandler | INTEGER, BIGINT | Integer |
| number.FloatTypeHandler / DoubleTypeHandler | FLOAT, REAL / DOUBLE | Floating point |
| number.BigDecimalTypeHandler | NUMERIC, DECIMAL | Decimal |
| number.BigIntegerTypeHandler | Large integer parameters | BigDecimal binding |
| number.NumberTypeHandler | Other Number parameters | BigDecimal binding |

SQL NULL reads are checked rather than becoming zero or false. The column range and precision still limit stored values.

```javascript
var query = @@selectSql(amount, enabled)<%
    SELECT CAST(#{amount} AS DECIMAL(12, 2)) AS "amount",
           CAST(#{enabled, jdbcType=BOOLEAN} AS BOOLEAN) AS "enabled"
%>;
return query(123.45, true);
```

H2 returns `{"amount":123.45,"enabled":true}`.

Optional handlers:

- `number.IntegerAsBooleanTypeHandler` writes true/false as 1/0 and reads nonzero integers as true.
- `number.StringAsBigIntegerTypeHandler` and `number.StringAsBigDecimalTypeHandler` store large numbers as text and read them back as numbers. Non-null input is BigInteger or BigDecimal respectively.
- `number.PgMoneyAsBigDecimalTypeHandler` converts PostgreSQL money text and BigDecimal. Money formatting depends on the database locale.

```javascript
var query = @@selectSql(enabled)<%
    SELECT CAST(#{enabled, typeHandler=net.hasor.dataql.sqlproc.types.number.IntegerAsBooleanTypeHandler} AS INTEGER)
%>;
return query(true);
```

The result is `1`. The explicit handler changes parameter binding; the result column is still read as INTEGER.

## Dates and times

| jdbcType | Input | Script result |
| --- | --- | --- |
| DATE | Epoch milliseconds or yyyy-MM-dd text | Epoch milliseconds |
| TIME | Epoch milliseconds or HH:mm:ss text | Epoch milliseconds |
| TIMESTAMP | Epoch milliseconds or yyyy-MM-dd HH:mm:ss with optional fractional seconds | Epoch milliseconds |
| TIME_WITH_TIMEZONE | Epoch milliseconds or `12:34:56+08:00` | ISO text with offset |
| TIMESTAMP_WITH_TIMEZONE | Epoch milliseconds or `2026-10-05T12:34:56+08:00` | ISO text with offset |

DATE, TIME and TIMESTAMP use `time.SqlDateTypeHandler`, `time.SqlTimeTypeHandler` and `time.SqlTimestampTypeHandler`. Offset types use `time.OffsetTimeTypeHandler` and `time.OffsetDateTimeTypeHandler`.

Timestamp text also accepts a `T` separator. Values without an offset follow JDBC and application local-time semantics. Offset handlers interpret numeric input as UTC. DataQL date results have millisecond precision and do not retain extra JDBC Timestamp nanoseconds.

```javascript
var query = @@selectSql(createdAt)<%
    SELECT CAST(#{createdAt, jdbcType=TIMESTAMP_WITH_TIMEZONE} AS TIMESTAMP WITH TIME ZONE)
%>;
return query('2026-10-05T12:34:56+08:00');
```

H2 returns `"2026-10-05T12:34:56+08:00"`. SQL NULL returns null; invalid date text fails the query.

The optional PostgreSQL `time.PgDateTypeHandler` accepts ISO date strings, supports BC-year conversion and returns ISO strings. It does not automatically replace DATE handling.

## XML text

`SqlXmlTypeHandler` creates SQLXML for binding, reads XML as text, and releases SQLXML resources:

```javascript
var save = @@insertSql(xml)<%
    INSERT INTO documents(xml_content)
    VALUES (#{xml, jdbcType=SQLXML})
%>;
return save('<person><name>Alice</name></person>');
```

The driver must support SQLXML and `documents.xml_content` must have a suitable type. For a normal text column, use string binding.

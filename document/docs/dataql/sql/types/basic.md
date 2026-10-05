---
id: basic
title: 6.7.2 基础类型处理器
---

内置处理器位于 `net.hasor.dataql.sqlproc.types` 的子包中。常规映射自动生效，按需处理器通过 `typeHandler` 或应用注册启用。

## 字符串

| 处理器 | JDBC 类型 | 脚本结果 |
| --- | --- | --- |
| string.StringTypeHandler | CHAR、VARCHAR、LONGVARCHAR | 完整字符串 |
| string.NStringTypeHandler | NCHAR、NVARCHAR、LONGNVARCHAR | Unicode 字符串 |
| string.ClobAsStringTypeHandler | CLOB | 完整文本 |
| string.NClobAsStringTypeHandler | NCLOB | 完整 Unicode 文本 |
| string.SqlXmlTypeHandler | SQLXML | XML 文本 |

`CHAR`、`NCHAR` 读取完整字段，定长字段的补空格行为由数据库决定。SQL NULL 返回 null，空字符串是否保留由数据库决定。

```javascript
var query = @@selectSql(text)<%
    SELECT CAST(#{text, jdbcType=NCHAR} AS NCHAR(4))
%>;
return query('中文测试');
```

H2 返回 `"中文测试"`。CLOB、NCLOB 和 SQLXML 都在查询结束前读取完整内容，不会向脚本暴露 JDBC Reader。

## 数值与布尔值

| 处理器 | 默认 JDBC 类型 | 说明 |
| --- | --- | --- |
| bool.BooleanTypeHandler | BIT、BOOLEAN | 布尔值 |
| number.ByteTypeHandler / ShortTypeHandler | TINYINT、SMALLINT | 小整数 |
| number.IntegerTypeHandler / LongTypeHandler | INTEGER、BIGINT | 整数 |
| number.FloatTypeHandler / DoubleTypeHandler | FLOAT、REAL / DOUBLE | 浮点数 |
| number.BigDecimalTypeHandler | NUMERIC、DECIMAL | 十进制数 |
| number.BigIntegerTypeHandler | 大整数参数 | 通过 BigDecimal 绑定 |
| number.NumberTypeHandler | 其他 Number 参数 | 通过 BigDecimal 绑定 |

数值读取时检查 SQL NULL，不会把空值误当成 0 或 false。数据库列的精度和范围决定最终存储能力。

```javascript
var query = @@selectSql(amount, enabled)<%
    SELECT CAST(#{amount} AS DECIMAL(12, 2)) AS "amount",
           CAST(#{enabled, jdbcType=BOOLEAN} AS BOOLEAN) AS "enabled"
%>;
return query(123.45, true);
```

H2 返回 `{"amount":123.45,"enabled":true}`。

以下处理器需按需指定：

- `number.IntegerAsBooleanTypeHandler`：将布尔值写为 1/0，读取整数时将非零视为 true。
- `number.StringAsBigIntegerTypeHandler`、`number.StringAsBigDecimalTypeHandler`：以字符串形式存大数，读取后还原为大数；非空写入值分别为 BigInteger、BigDecimal。
- `number.PgMoneyAsBigDecimalTypeHandler`：PostgreSQL money 文本与 BigDecimal 转换；金额文本格式受数据库区域设置影响。

```javascript
var query = @@selectSql(enabled)<%
    SELECT CAST(#{enabled, typeHandler=net.hasor.dataql.sqlproc.types.number.IntegerAsBooleanTypeHandler} AS INTEGER)
%>;
return query(true);
```

返回 `1`。参数处理器只改变写入；该结果列仍按 INTEGER 读取。

## 日期与时间

| jdbcType | 参数值 | 脚本结果 |
| --- | --- | --- |
| DATE | 毫秒时间戳、`yyyy-MM-dd` 文本 | 毫秒时间戳 |
| TIME | 毫秒时间戳、`HH:mm:ss` 文本 | 毫秒时间戳 |
| TIMESTAMP | 毫秒时间戳、`yyyy-MM-dd HH:mm:ss[.小数]` 文本 | 毫秒时间戳 |
| TIME_WITH_TIMEZONE | 毫秒时间戳、`12:34:56+08:00` | 带偏移量的 ISO 文本 |
| TIMESTAMP_WITH_TIMEZONE | 毫秒时间戳、`2026-10-05T12:34:56+08:00` | 带偏移量的 ISO 文本 |

DATE、TIME、TIMESTAMP 分别使用 `time.SqlDateTypeHandler`、`time.SqlTimeTypeHandler`、`time.SqlTimestampTypeHandler`；带时区类型使用 `time.OffsetTimeTypeHandler` 和 `time.OffsetDateTimeTypeHandler`。

时间戳处理器也接受文本中的 `T` 分隔符。不带时区的日期时间使用 JDBC 和应用本地时区语义；带时区处理器将数字按 UTC 时间点处理。DataQL 日期结果精度为毫秒，不保留 JDBC Timestamp 的额外纳秒位。

```javascript
var query = @@selectSql(createdAt)<%
    SELECT CAST(#{createdAt, jdbcType=TIMESTAMP_WITH_TIMEZONE} AS TIMESTAMP WITH TIME ZONE)
%>;
return query('2026-10-05T12:34:56+08:00');
```

H2 返回 `"2026-10-05T12:34:56+08:00"`。SQL NULL 返回 null，无法解析的日期文本使查询失败。

`time.PgDateTypeHandler` 是按需使用的 PostgreSQL 日期处理器，接受 ISO 日期字符串，支持公元前年份转换，读取结果也为 ISO 字符串。它不会自动替换常规 DATE 处理器。

## XML 文本

SQLXML 列使用 `SqlXmlTypeHandler`，写入时通过 JDBC 创建 SQLXML，读取为字符串并释放 SQLXML 资源：

```javascript
var save = @@insertSql(xml)<%
    INSERT INTO documents(xml_content)
    VALUES (#{xml, jdbcType=SQLXML})
%>;
return save('<person><name>Alice</name></person>');
```

此例要求数据库和驱动支持 SQLXML，且 `documents.xml_content` 已建为对应类型。若使用普通文本列，直接按字符串存储即可。

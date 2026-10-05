---
id: types
title: 6.7 类型处理
---

类型处理器将 DataQL 值绑定为 JDBC 参数，并将数据库结果转换回脚本可用的值。字符串、数值、布尔值和常见日期列已有默认处理器；JSON、向量及数据库专用类型可按参数指定处理器。

```javascript
var query = @@selectSql(createdAt)<%
    SELECT CAST(#{createdAt, jdbcType=TIMESTAMP} AS TIMESTAMP)
%>;
return query('2026-10-05 12:34:56');
```

上例适用于 H2：字符串按 `TIMESTAMP` 写入，查询返回毫秒时间戳。`jdbcType` 决定 JDBC 类型，`typeHandler` 可进一步指定转换实现。

## 使用指引

- [Java/JDBC 类型关系](types/mappings.md)：脚本值如何转换、处理器如何选择，以及枚举和地理信息的接入方式。
- [基础类型处理器](types/basic.md)：字符串、数值、布尔、日期、时间和 XML。
- [JSON 序列化处理器](types/json.md)：对象和列表存为 JSON 文本，读取后还原为脚本对象。
- [流与二进制](types/binary.md)：上传文件、二进制参数、BLOB 查询和资源生命周期。
- [数组类型处理器](types/arrays.md)：列表绑定 JDBC ARRAY，区分数组参数与 IN 条件。
- [向量类型处理器](types/vectors.md)：PostgreSQL pgvector 和 ClickHouse Float32 数组。
- [自定义类型处理器](../../dataway/engine/sql-types.md)：实现转换、注册默认映射，以及在 Dataway 中配置。

## 能力范围

SQL 执行器接收 DataQL 数据模型转换后的值。Java 枚举转换为名称字符串，普通 Java 数组转换为列表，业务对象转换为字段对象；实体注解、Java 枚举构造和 ORM 属性映射不属于此入口。当前没有内置 GIS 对象处理器，地理数据可使用数据库的 WKT/WKB 转换函数或应用处理器接入。

处理器支持转换不等于数据库支持对应列类型。示例标明所用数据库；接入其他数据库时，使用其 JDBC 驱动支持的字段类型和 SQL 语法。

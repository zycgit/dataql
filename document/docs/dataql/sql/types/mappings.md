---
id: mappings
title: 6.7.1 Java/JDBC 类型关系
---

DataQL 先将函数调用参数转换为脚本值，再将片段参数交给 SQL 执行器。类型处理器看到的是字符串、数值、布尔值、Map、List 或 `BinaryModel`，通常不是原始业务实体。

## 参数默认类型

以下是参数进入 JDBC 绑定时的默认关系。数字的具体 Java 类型由脚本字面量、计算结果或传入值决定。

| SQL 参数值 | 默认 JDBC 类型 | 说明 |
| --- | --- | --- |
| Boolean | BIT | 通过布尔处理器绑定 |
| Byte / Short / Integer / Long | TINYINT / SMALLINT / INTEGER / BIGINT | 保留整数类型 |
| Float / Double | FLOAT / DOUBLE | 浮点数 |
| BigInteger / BigDecimal | BIGINT / DECIMAL | 处理器使用 BigDecimal 绑定大数；列精度仍需足够 |
| String | VARCHAR | 日期文本需要指定日期 JDBC 类型 |
| List | ARRAY | 元素类型由非空元素推断，数据库需支持 SQL 数组 |
| BinaryModel | BLOB | 包括上传文件；可显式指定 VARBINARY 等类型 |
| Map | 无通用对象序列化 | 存 JSON 时显式使用 JsonTypeHandler |
| null | 无运行时类型 | 建议通过 jdbcType 指定目标列类型 |

SQL 执行器自身也接受 `byte[]`、JDBC 日期等 Java 值，但普通 Java 数组、日期和枚举经过 DataQL 时分别转换为列表、毫秒时间戳和枚举名称。应用 UDF 返回二进制时应返回 `BinaryModel`，见[流与二进制](binary.md)。

## 指定 JDBC 类型

```javascript
var query = @@selectSql(name, createdAt)<%
    SELECT CAST(#{name, jdbcType=VARCHAR} AS VARCHAR) AS "name",
           CAST(#{createdAt, jdbcType=TIMESTAMP} AS TIMESTAMP) AS "createdAt"
%>;
return query(null, '2026-10-05 12:34:56');
```

这个 H2 查询将第一个参数作为 SQL NULL 绑定，第二个参数解析为时间戳。`jdbcType` 支持 JDBC 名称或数值代码，例如 `VARCHAR` 或 `12`。它不会强制将任意值转换为目标类型，转换是否成立由选中的处理器和驱动决定。参数选项不提供 `javaType`。

## 选择顺序

参数绑定按下列顺序选择：

1. 使用 `typeHandler` 显式指定的处理器。
2. 非空参数带有 `jdbcType` 时，先查“实际 Java 类型 + JDBC 类型”的组合映射，再查可匹配的父类或接口组合映射。
3. 未找到组合映射时，使用 Java 类型的默认处理器；数组和已注册父类型也参与匹配。
4. null 参数提供了 `jdbcType` 时，使用该 JDBC 类型的处理器。
5. 没有可用处理器时，使用 JDBC 的通用对象绑定。

因此 `jdbcType=TIMESTAMP` 能让字符串、数字使用日期处理器，但仅为某个 JDBC 类型注册处理器不会覆盖所有非空参数已有的 Java 类型映射。

读取结果时，执行器查看 JDBC 列类型和驱动报告的 Java 类，依次选择组合映射、Java 类型映射和 JDBC 类型映射。驱动未提供可用类名或只报告通用 Object 时，按 JDBC 类型回退。MySQL YEAR 按整数处理，Oracle 专有类按对应 JDBC 类型处理。

## 枚举值

Java 枚举进入 DataQL 后使用 `name()` 对应的字符串。例如 `Status.ACTIVE` 对应 `"ACTIVE"`，不会自动取 `ordinal()` 或业务编码。

```javascript
var query = @@selectSql(status)<%
    SELECT id, name FROM people WHERE status = #{status}
%>;
return query('ACTIVE');
```

此例要求表中存在字符串 `status` 列。若数据库使用 `1`、`2` 等状态码，可在脚本中完成映射，或通过[自定义处理器](../../../dataway/engine/sql-types.md)将名称转换为编码。结果返回字符串或数字，由脚本决定对外含义。

## 地理信息

当前没有内置 Geometry、Point 等 Java GIS 类型处理器。可以让数据库把地理值与 WKT 文本互转，例如在已安装 PostGIS 的 PostgreSQL 中：

```javascript
var query = @@selectSql(point)<%
    SELECT ST_AsText(ST_GeomFromText(#{point}, 4326))
%>;
return query('POINT(120.15 30.28)');
```

返回 WKT 字符串 `POINT(120.15 30.28)`。需要 WKB 时，通过数据库二进制函数配合 `BinaryModel`；需要特定 JDBC 空间对象时，提供自定义处理器并在其中完成转换。

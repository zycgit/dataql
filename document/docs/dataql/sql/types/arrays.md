---
id: arrays
title: 6.7.5 数组类型处理器
---

`ArrayTypeHandler` 将 DataQL 列表绑定为一个 JDBC ARRAY 参数，查询结果转换为 DataQL 列表。数据库和驱动需支持 `Connection.createArrayOf`、`setArray` 和 `getArray`。

## 传入列表

以下脚本可在 H2 中直接执行：

```javascript
var query = @@selectSql(items)<%
    SELECT CAST(#{items, jdbcType=ARRAY} AS INTEGER ARRAY)
%>;
return query([1, 2, 3]);
```

返回 `[1,2,3]`。`jdbcType=ARRAY` 可以省略，非空列表本身会选择数组处理器；传 null 时建议保留该选项。

## 元素类型

| 列表元素 | 推断类型 |
| --- | --- |
| Boolean | BOOLEAN |
| Byte / Short / Integer / Long | TINYINT / SMALLINT / INTEGER / BIGINT |
| Float / Double | FLOAT / DOUBLE |
| BigInteger / BigDecimal | NUMERIC |
| String | VARCHAR |

null 元素不参与类型推断，但保留在原有位置。混合整数会扩大为 BIGINT，混合浮点数使用 DOUBLE，包含高精度数时使用 NUMERIC。字符串和数字混合会报错，Map 和嵌套列表也不能由通用写入处理器自动推断。

```javascript
var query = @@selectSql(items)<%
    SELECT CAST(#{items, jdbcType=ARRAY} AS DOUBLE ARRAY)
%>;
return query([1.25, null, 2]);
```

返回 `[1.25,null,2.0]`，DOUBLE 数组保持双精度。

## 空列表与 null

- `null` 表示 SQL NULL 数组。
- `[]` 表示没有元素的数组。
- `[null,null]` 表示包含两个空元素的数组。

空列表和全 null 列表无法推断元素类型，通用处理器向驱动请求 JAVA_OBJECT 数组。H2 可以结合上述 CAST 处理；其他驱动可能在 CAST 之前就拒绝该类型，此时需要能明确元素类型的处理器。

PostgreSQL 提供 `PgArrayTypeHandler`，构造时传入元素类型，例如 `new PgArrayTypeHandler("int4", 1)`。它不具备无参构造方法，不能仅写其类名让参数选项直接创建。应用应通过注册或无参子类提供实例，见[自定义类型处理器](../../../dataway/engine/sql-types.md#postgres-array)。

## 与 IN 条件的区别

ARRAY 将列表绑定为一个数据库数组值。`IN` 条件需要展开成多个参数时，使用 SQL 的 IN 规则：

```javascript
var find = @@selectSql(ids)<%
    SELECT id, name FROM people WHERE 1 = 1 @{in, AND id IN #{ids}}
%>;
return find([1, 2]);
```

这里生成多个 `?` 参数，不要求数据库支持 ARRAY。展开和空列表行为见 [SQL 规则](../rules.md)。

## 结果与资源

读取 JDBC Array 后，处理器取出元素并释放 Array；DataQL 拿到的是脱离连接的列表。带偏移量的日期元素返回 ISO 文本，常规日期时间元素返回毫秒时间戳。读取支持的数组形状取决于驱动，不能据此推断通用处理器支持任意多维数组写入。

写入时由处理器创建的 JDBC Array 在绑定后释放；应用自行提供的 JDBC Array 由应用负责释放。

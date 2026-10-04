---
id: types
title: 6.7 类型处理
---

SQL 执行器通过类型处理器写入 JDBC 参数、读取结果列。常规类型自动转换；自定义处理器的实现和注册见 [SQL 类型处理器](../../dataway/engine/sql-types.md)。

## 默认转换

内置处理器支持字符串、布尔值、数值、日期时间、字节数组和 JDBC Blob、Clob 等类型。参数按运行时 Java 类型选择处理器，结果按驱动返回的列类型选择处理器。

DataQL 会将部分 Java 值转换为脚本数据模型，因此处理器支持的 Java 类型与脚本值并不完全相同。当前默认枚举处理器未实现读写，枚举值可先转为字符串。

## 参数类型选项

```javascript
var add = @@insertSql(name)<%
    INSERT INTO people(name, age) VALUES (#{name, jdbcType=VARCHAR}, 20)
%>;
return add('carol');
```

`jdbcType` 指定传递给类型处理器的 JDBC 类型，支持 `VARCHAR`、`BIGINT` 等名称或类型数值。

应用提供自定义处理器后，通过 `typeHandler` 指定该参数的转换方式：

```sql
VALUES (#{name, jdbcType=VARCHAR, typeHandler=com.example.sql.UpperTextHandler}, 20)
```

`UpperTextHandler` 的完整实现见 [SQL 类型处理器](../../dataway/engine/sql-types.md)。该选项只影响参数写入，不改变查询结果的读取方式。

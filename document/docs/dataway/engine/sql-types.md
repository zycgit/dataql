---
title: "9.11 SQL 类型处理器"
---

:::info[依赖 SQL 执行器]

本扩展依赖 [SQL 执行器](../dataql-engine/sql.md)（`dataql-sqlproc`），使用前请完成 Dataway 的[数据源接入](../capabilities/datasources.md)。

:::

`TypeHandler` 控制 Java 值与 JDBC 参数、结果列之间的转换。可继承内置处理器调整某一步，也可继承 `AbstractTypeHandler` 实现参数写入和结果读取。

## 实现处理器

下面继承 `StringTypeHandler`，将写入数据库的字符串转为大写，结果读取保持不变。将该类放入应用工程。

```java
package com.example.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;

/** Converts a bound string to uppercase while keeping standard string reads. */
public class UpperTextHandler extends StringTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, Object value, Integer jdbcType) throws SQLException {
        statement.setString(index, value.toString().toUpperCase(Locale.ROOT));
    }
}
```

## 在 Dataway 中注册

`config` 表示应用注册的 `DatawayConfig`。在创建 Dataway 前加入以下配置，将处理器注册为字符串参数的默认处理器：

```java
import com.example.sql.UpperTextHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

config.configureHost(host -> {
    TypeHandlerRegistry.DEFAULT.register(String.class, new UpperTextHandler());
});
```

Spring、Solon、Hasor 均可使用此配置。`TypeHandlerRegistry.DEFAULT` 是共享注册表，注册会影响使用该注册表的字符串参数；应在首次执行 SQL 前完成。独立使用引擎时，在创建查询前直接调用同一 `register` 方法。

注册表还支持按 JDBC 类型，或 JDBC 类型与 Java 类型组合注册。处理器可能被多个查询共用，实现应保持线程安全。

## 脚本使用

在业务数据库中初始化 [SQL 执行](../../dataql/sql/execute.md)中的 `people` 表，再在控制台创建 DataQL 类型的 API：

```javascript
var add = @@insertSql(name)<%
    INSERT INTO people(name, age) VALUES (#{name}, 20)
%>;
var find = @@selectSql()<% SELECT name FROM people WHERE age = 20 %>;
run add('carol');
return find();
```

调试或发布后调用，写入的名称为 `CAROL`，脚本查询得到同一值。

## 按参数指定

仅某个参数需要转换时，将处理器类放入应用 classpath，并在占位符中指定类全名即可，无需注册为默认处理器：

```sql
INSERT INTO people(name, age)
VALUES (#{name, typeHandler=com.example.sql.UpperTextHandler}, 20)
```

显式指定的处理器优先于默认处理器，只负责该参数的绑定。参数为 null 时可通过 `jdbcType` 明确数据库类型，选项见[类型处理](../../dataql/sql/types.md)。

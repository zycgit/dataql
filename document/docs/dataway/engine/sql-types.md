---
title: "9.11 SQL 类型处理器"
---

:::info[依赖 SQL 执行器]

本扩展依赖 [SQL 执行器](../dataql-engine/sql.md)（`dataql-sqlproc`）。在 Dataway 中使用前，先完成[数据源接入](../capabilities/datasources.md)。

:::

`TypeHandler` 控制 JDBC 参数绑定和结果读取。适合处理特殊列格式、业务编码或数据库专有类型。常规类型的默认行为见[类型处理](../../dataql/sql/types.md)。

## 扩展接口

`TypeHandler` 没有泛型，定义四个方法：

| 方法 | 调用场景 |
| --- | --- |
| setParameter(PreparedStatement, int, Object, Integer) | 绑定 IN 参数，包括 null |
| getResult(ResultSet, String) | 按列名读取 |
| getResult(ResultSet, int) | 按列序号读取 |
| getResult(CallableStatement, int) | 读取存储过程 OUT 参数 |

通常继承 `AbstractTypeHandler<T>`。基类处理 null 参数绑定和异常包装，子类实现 `setNonNullParameter` 与三个 `getNullableResult`。读取 null 由子类负责；使用 `getInt`、`getBoolean` 等方法时，需要检查 `wasNull()`。

输入已经经过 DataQL 数据模型转换，常见值是 String、Number、Map、List、BinaryModel。不要将 Java 实体、枚举实例或 Reader 原样传入作为前提。返回值也应可转换为脚本值；JDBC 大字段和流需在查询关闭连接前读取完。

## 实现处理器

下面将字符串转为大写后写入，读取时保留数据库文本：

```java title="UpperTextHandler.java"
package com.example.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

public class UpperTextHandler extends AbstractTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index,
            String value, Integer jdbcType) throws SQLException {
        statement.setString(index, value.toUpperCase(Locale.ROOT));
    }

    @Override
    public String getNullableResult(ResultSet result, String columnName) throws SQLException {
        return result.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet result, int columnIndex) throws SQLException {
        return result.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return statement.getString(columnIndex);
    }
}
```

`getString` 在 SQL NULL 时返回 null。基类用 `jdbcType` 处理空参数；未提供时可根据泛型 String 推断 VARCHAR。自定义复杂类型最好显式指定空值的 JDBC 类型。

## 按参数使用

将处理器放入应用 classpath，在 Dataway 控制台创建 DataQL API：

```javascript
var query = @@selectSql(name)<%
    SELECT CAST(#{name, jdbcType=VARCHAR,
        typeHandler=com.example.sql.UpperTextHandler} AS VARCHAR)
%>;
return query('Alice');
```

该 H2 示例返回 `"ALICE"`。不需要全局注册，也不需要把处理器交给 Bean 容器；SQL 执行器通过类名创建实例。推荐提供公共无参构造方法。

`typeHandler` 只影响当前占位符的参数绑定。`SELECT name ...` 的结果读取仍通过注册表选择处理器，不会沿用某个 WHERE 参数的处理器。

## 注册默认映射

需要统一处理某类值时，在应用注册 `DatawayConfig` 的地方添加初始化配置：

```java title="在 Dataway 中注册"
import java.sql.Types;
import com.example.sql.UpperTextHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;
import net.hasor.dataway.service.DatawayConfig;

DatawayConfig config = new DatawayConfig();
config.configureHost(host -> {
    UpperTextHandler handler = new UpperTextHandler();
    TypeHandlerRegistry registry = TypeHandlerRegistry.DEFAULT;
    registry.register(String.class, handler);
    registry.register(Types.VARCHAR, String.class, handler);
});
```

此处同时配置普通 String 参数，以及显式 `jdbcType=VARCHAR` 的 String 参数。注册表中已有 CHAR、NVARCHAR 等组合映射，单独替换 String 默认处理器不会覆盖这些更具体的映射。

Spring、Solon、Hasor 都在各自配置中注册这个 `DatawayConfig`。独立使用 DataQL 引擎时，在首次执行 SQL 前直接调用同一组 `register` 方法。

三种注册形式：

| 注册方法 | 影响范围 |
| --- | --- |
| register(Class, handler) | 该 Java 类型的默认处理器 |
| register(int jdbcType, handler) | 按 JDBC 类型查找时的处理器，包括已指定类型的 null 参数 |
| register(int jdbcType, Class, handler) | Java 类型与 JDBC 类型的组合，优先于单一 Java 类型 |

结果列同时受驱动报告的 Java 类和 JDBC 类型影响。例如替换 VARCHAR 文本读取时，应考虑 `register(Types.VARCHAR, String.class, handler)`；只注册 JDBC 类型不能覆盖优先命中的 Java 映射。完整顺序见 [Java/JDBC 类型关系](../../dataql/sql/types/mappings.md)。

`TypeHandlerRegistry.DEFAULT` 由 SQL 执行器共享。注册会影响同一应用中使用该注册表的查询，应在执行查询前完成；需要局部行为时优先使用参数选项。当前注册使用显式 API，不支持处理器映射注解或实体属性扫描。

## PostgreSQL 数组 {#postgres-array}

空列表或专有数组类型需要固定元素名称时，可为 `PgArrayTypeHandler` 提供无参子类：

```java title="IntArrayHandler.java"
package com.example.sql;

import net.hasor.dataql.sqlproc.types.array.PgArrayTypeHandler;

public class IntArrayHandler extends PgArrayTypeHandler {
    public IntArrayHandler() {
        super("int4", 1);
    }
}
```

```javascript
var query = @@selectSql(items)<%
    SELECT #{items, jdbcType=ARRAY, typeHandler=com.example.sql.IntArrayHandler}
%>;
return query([]);
```

在 PostgreSQL 中绑定元素类型为 int4 的空数组。相同写法可传 `[1,null,3]`。若要作为默认配置，可在初始化时使用 `register(Types.ARRAY, Collection.class, new IntArrayHandler())`，但这会影响所有显式 ARRAY 的 Collection 参数，应仅在元素类型一致时使用。

## 实例与资源

按类名创建的处理器通常会缓存并在多次查询中复用。实现应无请求状态并支持并发调用，不要把当前参数、ResultSet、Connection 保存在字段中。

需根据参数类型构造时，注册器可调用 `ResolvableType` 或 `Class` 参数的公共构造方法。`@NoCache` 可禁止这种自动创建的实例缓存；显式 `register` 的实例仍按注册范围复用。普通转换无需使用这类构造方式。

处理器不要关闭调用方的 Statement 或 Connection。自身创建的 JDBC Array、SQLXML、Blob 等资源应在完成所需读写后释放；返回结果不得依赖已关闭的 JDBC 流。

---
slug: /dataway/dataql-engine/sql
title: "8.4 SQL 执行器"
---

SQL 执行器是 DataQL 引擎的[片段执行器扩展](../engine/fragments.md)。`dataql-sqlproc` 实现 `FragmentProcess`，通过 JDBC 执行脚本中的 SQL 片段，完成参数绑定和语句执行。

独立使用时，引入该模块，并通过 `HostConfiguration` 注册 `ConnectionProvider` 提供数据库连接。

## 引入 SQL 执行器

在引擎依赖之外添加以下模块，并引入所用数据库的 JDBC 驱动和连接池。

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs groupId="build-tool">
<TabItem value="maven" label="Maven">

```xml
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataql-sqlproc</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
```

</TabItem>
<TabItem value="gradle" label="Gradle">

```groovy
implementation 'net.hasor:dataql-sqlproc:@project.docsVersion@'
```

</TabItem>
</Tabs>

## 注册连接并执行 SQL

下面的 `source` 是应用已创建的 `DataSource`。在创建查询前注册连接提供者，再通过 `QueryManager → QueryBuilder → Query` 执行脚本。

```java
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;

ConnectionProvider provider = (name, hints) -> source.getConnection();
HostConfiguration host = new HostConfiguration();
host.addAttachment(ConnectionProvider.class, provider);

QueryManager manager = new QueryManager(host);
Query query = manager.newBuilder().createQuery("""
        var find = @@selectSql(value)<% SELECT #{value} AS result_value %>;
        return find(42);
        """);
Object value = query.execute().getData().unwrap(); // 42
```

`selectSql` 等片段由模块自动注册，无需手动添加。`#{value}` 绑定调用 `find` 时传入的参数，查询结果为 `42`。

未指定数据源时，传给 `ConnectionProvider` 的名称为空字符串。普通 SQL 执行结束后关闭本次连接；使用连接池时通常表示归还连接。连接池由应用创建和关闭。

## 多数据源

在创建 `QueryManager` 前，将单数据源的连接提供者替换为按名称查找。下面的 `primarySource` 为默认数据源，`reportingSource` 通过名称 `reporting` 访问。

```java
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;

Map<String, DataSource> sources = Map.of("", primarySource, "reporting", reportingSource);
ConnectionProvider provider = (name, hints) -> {
    DataSource selected = sources.get(name);
    if (selected == null) {
        throw new SQLException("Unknown data source: " + name);
    }
    return selected.getConnection();
};
host.addAttachment(ConnectionProvider.class, provider);
```

```javascript
hint FRAGMENT_SQL_DATA_SOURCE = 'reporting';
var find = @@selectSql(value)<% SELECT #{value} AS result_value %>;
return find(42);
```

`FRAGMENT_SQL_DATA_SOURCE` 将名称传给 `ConnectionProvider`。不同名称可以对应不同数据库和驱动，未知名称直接报错。

## 类型处理器

SQL 执行器通过 `TypeHandler` 完成 Java 值与 JDBC 参数、结果列之间的转换，内置字符串、数值、日期时间和二进制等处理器。应用可按参数指定处理器，或注册默认处理器统一转换；实现、注册和使用示例见 [SQL 类型处理器](../engine/sql-types.md)，脚本参数选项见[类型处理](../../dataql/sql/types.md)。

## 接入事务

多条 SQL 需要共同提交或回滚时，在创建查询前包装已配置的连接提供者，仍按 `ConnectionProvider` 类型注册。

```java
import net.hasor.dataql.sqlproc.execute.transaction.TransactionProvider;

TransactionProvider transactions = new TransactionProvider(provider);
host.addAttachment(ConnectionProvider.class, transactions);
```

脚本通过 `TransactionUdfSource` 执行事务回调，普通 SQL 和事务函数共用该连接提供者。包装对象由应用管理生命周期，传播行为和脚本用法见[事务](../../dataql/sql/transactions.md)。

SQL 语法、参数绑定和结果处理见 [SQL 执行](../../dataql/sql/execute.md)。执行扩展见 [SQL 拦截器](../engine/sql-interceptors.md)、[SQL 片段](../engine/sql-macros.md)、[SQL 规则](../engine/sql-rules.md)和 [SQL 方言](../engine/sql-dialects.md)。

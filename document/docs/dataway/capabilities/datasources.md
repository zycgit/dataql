---
title: "5.5 数据源接入"
description: "通过 ConnectionProvider 提供数据库连接，按需接入事务并组合多数据源查询。"
---

引入 `dataql-sqlproc`，通过 `ConnectionProvider` 提供 JDBC 连接，并注册到 `DatawayConfig`。以下三种配置按需选择。

## 不接入事务

`ConnectionProvider.findConnection(sourceName, hints)` 接收数据源名称和执行选项，返回 JDBC 连接。以下示例使用已有的 `primarySource`、`ds1`、`ds2`，未指定名称时选择主数据源：

```java title="直接提供数据库连接"
import java.util.Map;
import javax.sql.DataSource;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.service.DatawayConfig;

Map<String, DataSource> sources = Map.of("ds1", ds1, "ds2", ds2);
ConnectionProvider connections = (name, hints) -> {
    DataSource source = name == null || name.isBlank()
            ? primarySource : sources.get(name);
    return source == null ? null : source.getConnection();
};

DatawayConfig config = new DatawayConfig()
        .attachment(ConnectionProvider.class, connections);
```

SQL 执行后关闭连接，提交行为遵循连接设置，通常为自动提交。后续失败不会撤销已提交的修改；调用事务函数 `tran.*` 需要接入下列事务提供者。

## 接入事务

事务提供者实现 `TransactionalProvider`，统一提供连接和事务控制。

### DataQL 本地事务

用 `TransactionProvider` 包装上面的连接提供者：

```java title="接入 DataQL 本地事务"
import net.hasor.dataql.sqlproc.execute.transaction.TransactionProvider;

TransactionProvider transactions = new TransactionProvider(connections);
DatawayConfig config = new DatawayConfig()
        .attachment(ConnectionProvider.class, transactions);
```

`tran.required(...)` 等函数控制提交与回滚，同一线程、同一数据源名称共享事务连接。该事务独立于宿主框架；事务外的 SQL 遵循连接的提交设置。

### 宿主事务

框架提供者将 SQL、`tran.*` 和应用数据库操作接入同一套事务管理。以 Spring 为例：

```java title="接入 Spring 事务"
import net.hasor.dataway.spring.SpringTransactionProvider;

ConnectionProvider transactions = new SpringTransactionProvider(applicationContext);
DatawayConfig config = new DatawayConfig()
        .attachment(ConnectionProvider.class, transactions);
```

同一线程、同一数据源的 SQL 加入已有事务，`tran.required(...)` 加入或创建宿主事务。应用也需通过框架获取事务连接。具体配置见：

- [SpringTransactionProvider](../integration/spring.md#事务整合)：DataSource 与对应的 JDBC 事务管理器。
- [SolonTransactionProvider](../integration/solon.md#事务整合)：DataSource 与 `solon-data` 事务。
- [HasorTransactionProvider](../integration/hasor.md#事务整合)：DataSource 与同名 TransactionTemplate。

事务函数用法见[事务函数库](../../dataql/funx/transactions.md)。上述事务按数据源管理，跨库原子提交需另行接入分布式事务。

## 选择与组合数据源

`FRAGMENT_SQL_DATA_SOURCE` 指定传给连接提供者的数据源名称。下面从 ds1 查询人员、ds2 查询订单，合并为一个结果：

```javascript title="组合人员与订单"
{
    hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
    var findPerson = @@selectSql(id)<%
        SELECT id AS "id", name AS "name"
        FROM example_people WHERE id = #{id}
    %>;
    var person = findPerson(${id});
}
{
    hint FRAGMENT_SQL_DATA_SOURCE = "ds2"
    hint FRAGMENT_SQL_OPEN_PACKAGE = "off"
    var findOrders = @@selectSql(id)<%
        SELECT id AS "id", product AS "product", amount AS "amount"
        FROM example_orders WHERE person_id = #{id} ORDER BY id
    %>;
    var orders = findOrders(${id});
}
return {"person": person, "orders": orders};
```

`${id}` 接收请求参数，`#{id}` 绑定 SQL 值，`FRAGMENT_SQL_OPEN_PACKAGE = "off"` 保持订单结果为列表。完整配置见[样例工程](https://gitee.com/zycgit/dataql/tree/dev/example)。

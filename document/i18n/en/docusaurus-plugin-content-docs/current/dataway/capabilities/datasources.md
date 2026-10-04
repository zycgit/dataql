---
title: "5.5 Data source integration"
description: "Supply database connections through ConnectionProvider, integrate transactions and combine queries across sources."
---

Add `dataql-sqlproc`, supply JDBC connections through `ConnectionProvider`, and register it with `DatawayConfig`. Choose one of the three configurations below.

## Without transaction integration

`ConnectionProvider.findConnection(sourceName, hints)` receives the source name and execution options, and returns a JDBC connection. This example uses existing `primarySource`, `ds1` and `ds2` data sources; an absent name selects the primary source:

```java title="Supply database connections directly"
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

SQL execution closes the connection and follows its commit settings, usually auto-commit. Later failures do not undo committed changes. Transaction functions (`tran.*`) require a provider below.

## With transaction integration

Transaction providers implement `TransactionalProvider` to supply connections and transaction control.

### DataQL local transactions

Wrap the connection provider above with `TransactionProvider`:

```java title="Enable DataQL local transactions"
import net.hasor.dataql.sqlproc.execute.transaction.TransactionProvider;

TransactionProvider transactions = new TransactionProvider(connections);
DatawayConfig config = new DatawayConfig()
        .attachment(ConnectionProvider.class, transactions);
```

Functions such as `tran.required(...)` control commit and rollback. Calls on the same thread with the same source name share a transaction connection. These transactions are independent of the host framework; SQL outside a transaction follows the connection's commit settings.

### Host transactions

Framework providers integrate SQL, `tran.*` and application database operations with one transaction system. For Spring:

```java title="Integrate Spring transactions"
import net.hasor.dataway.spring.SpringTransactionProvider;

ConnectionProvider transactions = new SpringTransactionProvider(applicationContext);
DatawayConfig config = new DatawayConfig()
        .attachment(ConnectionProvider.class, transactions);
```

SQL on the same thread and source joins an existing transaction; `tran.required(...)` joins or starts a host transaction. Application code must also obtain transactional connections through the framework. See:

- [SpringTransactionProvider](../integration/spring.md#transaction-integration): DataSource and its JDBC transaction manager.
- [SolonTransactionProvider](../integration/solon.md#transaction-integration): DataSource and `solon-data` transactions.
- [HasorTransactionProvider](../integration/hasor.md#transaction-integration): DataSource and a TransactionTemplate with the same name.

See [Transaction functions](../../dataql/funx/transactions.md) for usage. These transactions apply per source; atomic cross-database commits require a distributed transaction solution.

## Select and combine sources

`FRAGMENT_SQL_DATA_SOURCE` selects the name passed to the connection provider. This script queries people in ds1 and orders in ds2, then combines the results:

```javascript title="Combine a person and their orders"
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

`${id}` reads the request parameter, `#{id}` binds its SQL value, and `FRAGMENT_SQL_OPEN_PACKAGE = "off"` keeps orders as a list. Complete configurations are available in [Example projects](https://gitee.com/zycgit/dataql/tree/dev/example).

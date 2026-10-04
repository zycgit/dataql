---
slug: /dataway/dataql-engine/sql
title: "8.4 SQL Executor"
---

The SQL executor is a [fragment processor extension](../engine/fragments.md) of the DataQL engine. `dataql-sqlproc` implements `FragmentProcess` to execute SQL fragments through JDBC, including parameter binding and statement execution.

For standalone use, add the module and register a `ConnectionProvider` with `HostConfiguration` to supply database connections.

## Add the SQL executor

Add the following module alongside the engine, plus the JDBC driver and connection pool for your database.

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

## Register a connection and execute SQL

`source` below is an application-managed `DataSource`. Register its connection provider before creating queries, then execute through `QueryManager → QueryBuilder → Query`.

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

The module registers fragments such as `selectSql` automatically. No manual fragment registration is required. `#{value}` binds the argument passed to `find`, and the query returns `42`.

Without a data source hint, the provider receives an empty name. An ordinary SQL call closes its connection when complete; a pool normally returns it to the pool. The application creates and closes the pool.

## Named data sources

Replace the single-source provider with a name lookup before creating `QueryManager`. Here `primarySource` is the default and `reportingSource` is selected by the `reporting` name.

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

`FRAGMENT_SQL_DATA_SOURCE` supplies the name to `ConnectionProvider`. Each name can refer to a different database and driver. Unknown names fail explicitly.

## Type handlers

The SQL executor uses `TypeHandler` to convert between Java values, JDBC parameters and result columns. Built-in handlers cover strings, numbers, dates, times and binary values. Applications can select a handler per parameter or register a default. See [SQL type handlers](../engine/sql-types.md) for implementation, registration and usage, and [Type handling](../../dataql/sql/types.md) for script parameter options.

## Transactions

To commit or roll back multiple fragments together, wrap the configured provider before creating queries and register the wrapper under the same `ConnectionProvider` type.

```java
import net.hasor.dataql.sqlproc.execute.transaction.TransactionProvider;

TransactionProvider transactions = new TransactionProvider(provider);
host.addAttachment(ConnectionProvider.class, transactions);
```

Use `TransactionUdfSource` to execute transaction callbacks; ordinary SQL and transaction functions share the provider. The application owns the wrapper lifecycle. See [Transactions](../../dataql/sql/transactions.md) for propagation and script examples.

SQL syntax, parameter binding and results are covered in [SQL execution](../../dataql/sql/execute.md). Further extensions include [SQL interceptors](../engine/sql-interceptors.md), [SQL fragments](../engine/sql-macros.md), [SQL rules](../engine/sql-rules.md) and [SQL dialects](../engine/sql-dialects.md).

---
title: "6.2 Transaction integration"
description: "Choose independent or host transactions based on whether metadata, audit logs and business changes must commit together."
---

Transaction integration lets API metadata commit or roll back together with application audit logs and business data. The choice depends on consistency requirements. Operations sharing the primary database can still use different connections and transactions.

For example, an administration interceptor saves an API and then writes an audit log: use a shared host transaction if logging failure must undo the API change; otherwise, default independent transactions suffice.

## Default independent transactions

Pass an ordinary connection pool. The built-in executor commits or rolls back metadata writes independently:

```java title="Default configuration"
ApiDataAccessLayer metadata = new JdbcDataAccessLayer(source);
```

Register the store as a bean or set it through `DatawayConfig.dataAccessLayer(metadata)`.

## Join host transactions

Pass the host transaction executor to `JdbcDataAccessLayer`. Start an outer transaction in the administration interceptor or application service around `chain.proceed()` and the log write. Both must use the same DataSource, join the host transaction on the same thread, and follow the configured rollback rules.

See metadata storage configuration for [Spring](../integration/spring.md#metadata-storage), [Solon](../integration/solon.md#metadata-storage) and [Hasor](../integration/hasor.md#metadata-storage).

## Scope

This section applies to JDBC metadata. Nacos uses snapshot CAS for atomic batches; coordinating those writes with application database audit records requires a separate mechanism. For script database transactions, see [Data source integration](../capabilities/datasources.md). Console saves and publications are separate HTTP requests with separate transactions.

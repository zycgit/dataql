---
slug: /dataway/metadata
title: "6. Metadata storage"
hide_table_of_contents: true
description: "Use ApiDataAccessLayer to store API definitions, scripts, options and release history in a database, Nacos or a custom store."
---

Metadata storage saves API definitions, scripts, parameter settings and release history to support editing, publishing, API calls and document generation. Saving updates the draft; publishing creates a snapshot. API calls and document generation read published, enabled APIs.

Dataway queries and writes metadata through `ApiDataAccessLayer`, with database and Nacos implementations available. Applications can implement this interface to connect other storage services.

## Storage configuration

Add the module for your chosen store and create an `ApiDataAccessLayer` instance:

- [Database](providers/jdbc.md): add `dataway-meta-jdbc` and use `JdbcDataAccessLayer` to store records in database tables.
- [Nacos](providers/nacos.md): add `dataway-meta-nacos` and use `NacosDataAccessLayer` to store records in a configuration snapshot.
- [Custom storage](providers/custom.md): implement `ApiDataAccessLayer` for an existing application storage service.

Set the store through `DatawayConfig.dataAccessLayer(...)`. This example uses JDBC storage; `source` is the application data source, and the tables must already exist:

```java title="Set metadata storage"
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;

ApiDataAccessLayer metadata = new JdbcDataAccessLayer(source);
DatawayConfig config = new DatawayConfig().dataAccessLayer(metadata);
Dataway dataway = config.createDataway();
```

The application creates and manages connection pools, Nacos clients and other storage resources.

## Usage guide

- [Table and field mappings](mapping.md): change database table and column names or Nacos entity and field names.
- [Transaction integration](transactions.md): commit or roll back metadata writes together with application operations.
- [Providers](providers/index.md): prepare database tables or a Nacos snapshot and create the data access layer.

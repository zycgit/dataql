---
slug: /dataway/metadata/providers
title: "6.3 Providers"
hide_table_of_contents: true
description: "Store Dataway API metadata in a database or Nacos."
---

The database provider reads and writes API records through JDBC. The Nacos provider stores metadata snapshots through its configuration service. The application creates and manages connections and clients.

## Usage guide

- [Database provider](jdbc.md): Prepare the schema and DataSource, create JdbcDataAccessLayer, and configure table prefixes and transaction executors.
- [Nacos provider](nacos.md): Prepare an initial snapshot and ConfigService, create NacosDataAccessLayer, and configure storage locations and name mappings.
- [Custom providers](custom.md): implement the storage contract or extend an existing provider.

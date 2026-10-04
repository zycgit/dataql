---
slug: /dataway/integration
title: "3. Framework integration"
hide_table_of_contents: true
description: "Choose framework adapters, metadata stores, SQL connections and identity integration for your application."
---

Dataway runs inside your application, using its web container, login identities and database connections. Select the adapter for your framework and a metadata store, then add SQL support as needed.

## Integration options

Framework adapters register web endpoints and read framework configuration. Each integration guide provides its dependencies and configuration.

- [Spring integration](spring.md): Use `dataway-spring` in Spring Boot MVC projects.
- [Solon integration](solon.md): Use `dataway-solon` in Solon Web projects.
- [Hasor integration](hasor.md): Use `dataway-hasor` in Hasor Web / Boot projects.

## Extension modules

- [dataway-meta-jdbc](../metadata/providers/jdbc.md): Use JDBC to store API definitions, drafts and release history in a database.
- [dataway-meta-nacos](../metadata/providers/nacos.md): Use Nacos configuration storage to save API definitions, drafts and release history.
- [dataql-sqlproc](../dataql-engine/sql.md): Query and update databases through SQL scripts or SQL fragments within DataQL.
- [Custom storage](../metadata/providers/custom.md): Implement `ApiDataAccessLayer` to save API metadata in a storage service of your choice.

## Usage guide

- Configuration ([Spring](spring.md#configure-and-assemble), [Solon](solon.md#configure-and-assemble), [Hasor](hasor.md#configure-and-assemble)): Add the modules, register services and enable request endpoints.
- Access authorization ([Spring](spring.md#access-authorization), [Solon](solon.md#access-authorization), [Hasor](hasor.md#access-authorization)): Register a login interceptor and provide the current user identity to Dataway.
- Business data sources ([Spring](spring.md#sql-data-sources), [Solon](solon.md#sql-data-sources), [Hasor](hasor.md#sql-data-sources)): Supply connections through `ConnectionProvider` and select one or more data sources by name.
- SQL transactions ([Spring](spring.md#sql-transactions), [Solon](solon.md#sql-transactions), [Hasor](hasor.md#sql-transactions)): Use script or host transactions to commit or roll back SQL operations on the same data source together.
- JDBC examples ([Spring](spring.md#jdbc-example), [Solon](solon.md#example), [Hasor](hasor.md#example)): Store metadata in a database and try SQL across two data sources, login, uploads and Swagger UI.
- [Nacos example](spring.md#nacos-example): Use Spring Boot with Nacos metadata storage. The test entry point starts local Nacos automatically and supports real HTTP tests.

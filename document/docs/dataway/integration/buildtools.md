---
slug: /dataway/integration
title: "3. 框架整合"
hide_table_of_contents: true
description: "按项目框架和使用场景，选择 Dataway 整合模块、元数据存储、SQL 数据源及身份鉴权方式。"
---

Dataway 嵌入应用，复用应用的 Web 容器、登录身份和数据库连接。按项目框架选择整合模块和元数据存储，按需扩展 SQL 能力。

## 接入方式

框架整合模块负责注册 Web 入口和读取框架配置。具体的依赖与配置方式见对应整合页。

- [Spring 整合](spring.md)：通过 `dataway-spring` 接入 Spring Boot MVC 项目。
- [Solon 整合](solon.md)：通过 `dataway-solon` 接入 Solon Web 项目。
- [Hasor 整合](hasor.md)：通过 `dataway-hasor` 接入 Hasor Web / Boot 项目。

## 扩展模块

- [dataway-meta-jdbc](../metadata/providers/jdbc.md)：通过 JDBC 在数据库中保存接口定义、草稿和发布历史。
- [dataway-meta-nacos](../metadata/providers/nacos.md)：通过 Nacos 配置服务保存接口定义、草稿和发布历史。
- [dataql-sqlproc](../dataql-engine/sql.md)：通过 SQL 脚本或 DataQL 中的 SQL 片段执行数据库查询与更新。
- [自定义存储](../metadata/providers/custom.md)：实现 `ApiDataAccessLayer`，将接口元数据保存到自选的存储服务。

## 使用指引

- 配置与装配（[Spring](spring.md#配置与装配)、[Solon](solon.md#配置与装配)、[Hasor](hasor.md#配置与装配)）：引入模块、注册服务并开放请求入口。
- 访问鉴权（[Spring](spring.md#访问鉴权)、[Solon](solon.md#访问鉴权)、[Hasor](hasor.md#访问鉴权)）：注册登录拦截器，向 Dataway 提供当前用户身份。
- 业务数据源（[Spring](spring.md#sql-数据源)、[Solon](solon.md#sql-数据源)、[Hasor](hasor.md#sql-数据源)）：通过 `ConnectionProvider` 提供连接，按名称选择单个或多个数据源。
- SQL 事务（[Spring](spring.md#sql-事务)、[Solon](solon.md#sql-事务)、[Hasor](hasor.md#sql-事务)）：使用脚本事务或宿主事务，让同一数据源的 SQL 操作共同提交或回滚。
- JDBC 示例（[Spring](spring.md#jdbc-example)、[Solon](solon.md#example)、[Hasor](hasor.md#example)）：使用数据库保存元数据，体验双数据源 SQL、登录、上传和 Swagger UI。
- [Nacos 示例](spring.md#nacos-example)：使用 Spring Boot 和 Nacos 保存元数据，测试入口自动启动本地 Nacos，支持直接运行 HTTP 测试。

---
title: "1. 介绍"
description: "Dataway 基于 DataQL 提供内嵌式接口开发能力，在界面中配置、调试和发布 API，连接应用中的数据库与服务。"
---

**Dataway 是基于 DataQL 的内嵌式 API 开发框架。** 应用引入 JAR 后，即可在控制台编写脚本、调试和发布 HTTP 接口。

DataQL 负责查询、转换和服务聚合，Dataway 提供接口管理与发布能力。开发者使用 DataQL 或 SQL 描述业务逻辑，减少重复编写数据访问和 Controller 代码。

## 开发方式

应用提供数据源、业务服务和登录身份。开发者在控制台编辑、调试并发布脚本，调用方通过 HTTP 访问已发布的 API。草稿中的修改在重新发布后才会对 API 调用生效。

![Dataway 连接应用数据与前端接口](/img/dataway/application-overview.png)

## 适用场景

| 场景 | 使用方式 |
| --- | --- |
| 取数据 | 为报表、看板、列表和详情页查询数据，按页面需要筛选字段、计算值和调整结构 |
| 存数据 | 接收表单或 JSON 参数，调用 SQL 或业务服务完成写入，由应用配置校验和事务 |
| 数据聚合 | 汇集多个数据库或服务的结果，整理成一次接口调用所需的数据结构 |

## 内嵌架构

Dataway 采用“核心 + 适配器”结构，支持 Spring、Solon、Hasor。宿主应用提供数据源、业务服务和认证。

| 组件 | 作用 |
| --- | --- |
| `dataway-embedded` | 管理 API 定义、执行脚本、检查权限，提供四个 HTTP Handler |
| `dataway-embedded-web` | 控制台资源 JAR，默认随核心引入，可独立替换 |
| `dataway-spring`、`dataway-solon`、`dataway-hasor` | 读取框架配置，将 Handler 注册到宿主 MVC |
| `dataway-meta-jdbc` | 在数据库中保存接口定义、草稿和发布历史 |
| `dataway-meta-nacos` | 在 Nacos 中保存接口定义、草稿和发布历史 |
| `dataql-sqlproc` | 可选 SQL 执行扩展，执行脚本中的数据库操作 |

四个入口注册到宿主 Web 容器，共享核心服务。元数据与业务数据源分别配置，调用链见[工作原理](../principles/index.md)。

## 开始使用

通过[快速开始](quickstart.md)体验 Spring Boot + JDBC，完成接口发布与调用。[Spring Boot + Nacos 示例](../integration/spring.md#nacos-example)提供自动启动本地 Nacos 的测试入口。

1. 选择 [Spring](../integration/spring.md)、[Solon](../integration/solon.md) 或 [Hasor](../integration/hasor.md) 接入。
2. 准备[元数据存储](../metadata/index.md)，接入[应用身份](../authorization/identity.md)。
3. 在控制台[编写脚本](../capabilities/development/script.md)，按[可视化操作](../capabilities/management.md#api-status)保存并发布。
4. 按需开放 [Swagger / OpenAPI 文档](../capabilities/document.md)。

本指南使用 `@project.docsVersion@`，运行基线为 JDK 17。框架示例分别使用 Spring Boot 4.1、Solon 4.1 和 Hasor 5.3。

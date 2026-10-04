---
slug: /dataway/metadata
title: "6. 元数据存储"
hide_table_of_contents: true
description: "通过 ApiDataAccessLayer 保存和读取 API 定义、脚本、配置及发布历史，接入数据库、Nacos 或自定义存储。"
---

元数据存储用于保存 API 定义、脚本、参数配置和发布历史，为接口编辑、发布、调用及文档生成提供数据支持。保存接口时更新草稿，发布时生成快照，调用 API 和生成文档时读取已发布且启用的接口。

Dataway 通过 `ApiDataAccessLayer` 接口统一查询和写入元数据，提供数据库、Nacos 两种实现，应用也可实现该接口接入其他存储服务。

## 存储配置

按存储方式引入对应模块，并创建一个 `ApiDataAccessLayer` 实例：

- [数据库](providers/jdbc.md)：引入 `dataway-meta-jdbc`，通过 `JdbcDataAccessLayer` 将记录保存到数据库表中。
- [Nacos](providers/nacos.md)：引入 `dataway-meta-nacos`，通过 `NacosDataAccessLayer` 将记录保存为配置快照。
- [自定义存储](providers/custom.md)：实现 `ApiDataAccessLayer`，接入应用已有的存储服务。

通过 `DatawayConfig.dataAccessLayer(...)` 指定访问层。以下使用 JDBC 存储，`source` 为应用的数据源，表结构需提前创建：

```java title="指定元数据存储"
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;

ApiDataAccessLayer metadata = new JdbcDataAccessLayer(source);
DatawayConfig config = new DatawayConfig().dataAccessLayer(metadata);
Dataway dataway = config.createDataway();
```

连接池、Nacos 客户端等资源由应用创建和管理。

## 使用指引

- [表与字段映射](mapping.md)：调整数据库表、列或 Nacos 实体与字段的名称。
- [事务整合](transactions.md)：让元数据写入与应用业务操作共同提交或回滚。
- [提供者](providers/index.md)：准备数据库表或 Nacos 快照，并创建访问层。

---
title: "6.2 事务整合"
description: "根据元数据与审计、业务操作是否需要共同提交，选择独立事务或宿主事务。"
---

事务整合用于让 API 元数据与应用的审计日志、业务数据共同提交或回滚。是否需要整合，取决于这些操作的一致性要求。共用主数据库时，各操作仍可能使用不同连接和事务。

例如，管理拦截器保存接口后写入审计日志：若日志失败时也要撤销接口修改，就整合宿主事务；若允许接口修改保留，默认独立事务即可。

## 默认独立事务

直接传入普通连接池数据源，元数据写入由内置执行器独立提交或回滚：

```java title="默认配置"
ApiDataAccessLayer metadata = new JdbcDataAccessLayer(source);
```

将访问层注册为 Bean，或通过 `DatawayConfig.dataAccessLayer(metadata)` 配置。

## 接入宿主事务

将宿主事务执行器传给 `JdbcDataAccessLayer`，并在管理拦截器或应用服务中开启外层事务，覆盖 `chain.proceed()` 和日志写入。两者使用同一 DataSource，在同一线程中加入宿主事务，并配置异常回滚规则。

具体配置见 [Spring](../integration/spring.md#元数据存储)、[Solon](../integration/solon.md#元数据存储)、[Hasor](../integration/hasor.md#元数据存储) 的元数据存储小节。

## 适用范围

本节适用于 JDBC 元数据。Nacos 通过快照 CAS 保证自身批次的原子性，与应用数据库的审计记录需另行协调。脚本访问业务数据库的事务配置见[数据源接入](../capabilities/datasources.md)。控制台的保存、发布是独立 HTTP 请求，分别执行事务。

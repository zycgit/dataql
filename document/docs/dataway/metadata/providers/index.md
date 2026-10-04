---
slug: /dataway/metadata/providers
title: "6.3 提供者"
hide_table_of_contents: true
description: "使用数据库或 Nacos 保存 Dataway 接口元数据。"
---

数据库提供者通过 JDBC 读写接口记录，Nacos 提供者通过配置服务读写元数据快照。连接和客户端由应用创建与管理。

## 使用指引

- [数据库提供者](jdbc.md)：准备建表脚本和 DataSource，创建 JdbcDataAccessLayer，配置表名前缀与事务执行器。
- [Nacos 提供者](nacos.md)：准备初始快照和 ConfigService，创建 NacosDataAccessLayer，配置存储位置与名称映射。
- [自定义提供者](custom.md)：实现访问层契约，或包装已有提供者增加能力。

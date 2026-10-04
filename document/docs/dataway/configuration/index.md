---
slug: /dataway/configuration
title: "10. 框架配置"
hide_table_of_contents: true
description: "DatawayConfig 完整配置、默认值与核心扩展。"
---

`DatawayConfig` 集中配置 Dataway 的元数据存储、身份鉴权、结果处理、上传缓存、文档信息和执行引擎。本章以 [DatawayConfig](core.md) 为入口，列出完整配置方法、默认值和约束，并介绍相关扩展的配置方式。

宿主配置文件负责入口开关与访问路径，配置格式和完整配置项分别见 [Spring](../integration/spring.md#入口配置)、[Solon](../integration/solon.md#入口配置)、[Hasor](../integration/hasor.md#入口配置)。

## 使用指引

- [DatawayConfig](core.md)：配置核心服务和应用默认值。
- [管理拦截器](admin-interceptors.md)：为管理操作配置审计、校验和事务。
- [API 拦截器](api-interceptors.md)：校验执行参数，扩展脚本执行与结果处理。
- [控制台部署](console.md)：独立部署页面，通过网关连接后端并配置初始化与登录。

---
id: about
sidebar_position: 1
title: a.SQL执行器
description: SQL 执行器是 DataQL 的一个 FragmentProcess 扩展，其作用是让 DataQL 可以执行 SQL。
---

# SQL执行器

SQL 执行器是 DataQL 的一个 `[FragmentProcess](../develop/fragment.md)` 扩展，其作用是让 DataQL 可以执行 SQL。

执行器的实现是 `FunctionX` 扩展包提供的。使用执行器只需要引入扩展包。

```xml
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>hasor-dataql-fx</artifactId>
    <version>4.2.5</version>
</dependency>
```

功能与特性
- 支持两种模式：`简单模式`、`分页模式`
- 简单模式下，使用原生SQL。100% 兼容所有数据库
- 分页模式下，自动改写分页SQL。并兼容多种数据库
- 支持参数化 SQL，更安全
- 支持 SQL 注入，更灵活
- 支持批量 `CURD`

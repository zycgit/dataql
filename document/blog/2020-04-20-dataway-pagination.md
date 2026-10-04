---
slug: dataway-pagination
title: "通过 Dataway 配置一个带有分页查询的接口"
description: "分页接口需要同时返回当前页记录和总数。本文使用 SQL 执行器完成数据库分页，接收从 1 开始的 page 和每页数量 size，并将两部分数据组成一个响应。"
authors: [zyc]
tags: [DataQL, Dataway, SpringBoot]
topics: [dataway]
language: zh-cn
updated: 2026-10-04
---

分页接口需要同时返回当前页记录和总数。本文使用 SQL 执行器完成数据库分页，接收从 1 开始的 page 和每页数量 size，并将两部分数据组成一个响应。

<!-- truncate -->

## 准备示例

[Spring Boot 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)内置 H2 数据、登录页面、控制台和本文接口。启动后打开 `http://127.0.0.1:8080/`，以 `admin` / `example-password` 登录。

```bash
mvn -f example/dataway-spring-example/pom.xml spring-boot:run
```

## 配置页码和边界

请求体使用 `{"page":1,"size":1}`。开启 FRAGMENT_SQL_QUERY_BY_PAGE 后，SQL 片段返回分页对象；FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET 设为 1，使前端的第一页与脚本约定一致。示例限制每页 1～100 条，并按唯一的 id 排序。

## 执行分页查询

本篇接口为 `POST /blog/pagination`，类型选择 DataQL，Parameters 内容如下：

```json title="请求参数"
{
  "page": 1,
  "size": 1
}
```

```javascript title="接口脚本"
hint FRAGMENT_SQL_DATA_SOURCE = 'ds1';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
hint FRAGMENT_SQL_QUERY_BY_PAGE = true;
hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1;
var findPeople = @@selectSql()<%
    SELECT id, name FROM example_people ORDER BY id
%>;
assert ${page} >= 1;
assert ${size} >= 1 && ${size} <= 100;
var query = findPeople();
run query.setPageInfo({'currentPage': ${page}, 'pageSize': ${size}});
var rows = query.data();
return {'items': rows, 'pagination': query.pageInfo()};
```

```json title="API 选项"
{
  "resultHandler": "raw"
}
```

示例启动时自动发布该接口。在控制台自行创建时，填写这些内容并点击 Save、Publish；修改已有脚本后也需重新发布。

## 发起 HTTP 请求

```bash title="登录示例应用"
curl -c cookies.txt -X POST http://127.0.0.1:8080/session/login \
  -d 'username=admin&password=example-password'
```

```bash
curl -i -b cookies.txt http://127.0.0.1:8080/api/blog/pagination \
  -H 'Content-Type: application/json' \
  -d '{"page":1,"size":1}'
```

第一页的关键结果如下，响应还包含其他分页信息：

```json
{
  "items": [{"id":1,"name":"Alice"}],
  "pagination": {
    "enable":true,
    "pageSize":1,
    "totalCount":2,
    "totalPage":2,
    "currentPage":1,
    "recordPosition":0
  }
}
```

将 page 改为 2 得到 Bob，totalCount 仍为 2。`data()` 读取本页，`pageInfo()` 获取总数等信息，可能额外执行计数查询。执行器根据 JDBC 数据库信息选择分页方言，也可通过 FRAGMENT_SQL_PAGE_DIALECT 指定。

## 源码与验证

[本篇脚本、参数和接口选项](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example/src/main/resources/blog/pagination)随示例提供；[BlogApiService](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/service/BlogApiService.java)负责发布，`BlogApiTest` 启动真实 Web 服务和 H2 验证请求。

```bash
mvn -f example/dataway-spring-example/pom.xml test
```

原文：[《通过 Dataway 配置一个带有分页查询的接口》](https://my.oschina.net/ta8210/blog/3277320)。可查阅[作者的同文发布](https://www.cnblogs.com/ta8210/p/12981098.html)。本文的配置和代码按当前仓库实现重写。

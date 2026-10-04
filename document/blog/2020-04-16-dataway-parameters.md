---
slug: dataway-parameters
title: "Dataway 配置数据接口时和前端进行参数对接"
description: "前端传入人员名称，接口返回匹配人员的编号和姓名。本文明确请求与响应的字段约定，再把 JSON 请求参数绑定到 SQL，最后转换为前端约定的结果结构。"
authors: [zyc]
tags: [DataQL, Dataway, SpringBoot]
topics: [dataway]
language: zh-cn
updated: 2026-10-04
---

前端传入人员名称，接口返回匹配人员的编号和姓名。本文明确请求与响应的字段约定，再把 JSON 请求参数绑定到 SQL，最后转换为前端约定的结果结构。

<!-- truncate -->

## 准备示例

[Spring Boot 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)内置 H2 数据、登录页面、控制台和本文接口。启动后打开 `http://127.0.0.1:8080/`，以 `admin` / `example-password` 登录。

```bash
mvn -f example/dataway-spring-example/pom.xml spring-boot:run
```

## 约定输入输出

请求体使用 `{"name":"Ali"}`，响应使用 `{"users":[{"userId":1,"userName":"Alice"}]}`。前端无需传入百分号；脚本在 SQL 内拼接模糊匹配条件。

## 查询并转换结果

本篇接口为 `POST /blog/parameters`，类型选择 DataQL，Parameters 内容如下：

```json title="请求参数"
{
  "name": "Ali"
}
```

```javascript title="接口脚本"
hint FRAGMENT_SQL_DATA_SOURCE = 'ds1';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
var findPeople = @@selectSql(name)<%
    SELECT id, name FROM example_people
    WHERE name LIKE CONCAT('%', #{name}, '%') ORDER BY id
%>;
var rows = findPeople(${name});
return {'users': rows => [{'userId': id, 'userName': name}]};
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
curl -i -b cookies.txt http://127.0.0.1:8080/api/blog/parameters \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ali"}'
```

返回结果：

```json
{"users":[{"userId":1,"userName":"Alice"}]}
```

`${name}` 读取本次请求参数，`findPeople(...)` 把值传给 SQL 片段，`#{name}` 由 JDBC 参数绑定。字段转换发生在查询后，数据库中的 id、name 分别输出为 userId、userName。

同源页面也可以使用 fetch：

```javascript
const response = await fetch('/api/blog/parameters', {
  method: 'POST',
  credentials: 'same-origin',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({name: 'Ali'})
});
const result = await response.json();
console.log(result.users);
```

返回空列表表示没有匹配记录。LIKE 中的 `%` 和 `_` 仍具有通配符含义；接口需要按字面搜索时，应由业务约定转义规则。

## 源码与验证

[本篇脚本、参数和接口选项](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example/src/main/resources/blog/parameters)随示例提供；[BlogApiService](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/service/BlogApiService.java)负责发布，`BlogApiTest` 启动真实 Web 服务和 H2 验证请求。

```bash
mvn -f example/dataway-spring-example/pom.xml test
```

原文：[《Dataway 配置数据接口时和前端进行参数对接》](https://my.oschina.net/ta8210/blog/3236659)。可查阅[作者的同文发布](https://www.cnblogs.com/ta8210/p/12981106.html)。本文的配置和代码按当前仓库实现重写。

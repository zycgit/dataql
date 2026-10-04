---
slug: whydataway
title: "让 Spring Boot 不再需要 Controller、Service、DAO、Mapper"
description: "查询类接口通常需要接收参数、执行 SQL 和返回 JSON。Dataway 把这些步骤放入可编辑、可调试、可发布的脚本中；应用负责提供数据源和身份识别。本文以人员查询为例，完成从 Spring 配置到 HTTP 调用的过程。"
authors: [zyc]
tags: [DataQL, Dataway, SpringBoot]
topics: [dataway]
language: zh-cn
updated: 2026-10-04
---

查询类接口通常需要接收参数、执行 SQL 和返回 JSON。Dataway 把这些步骤放入可编辑、可调试、可发布的脚本中；应用负责提供数据源和身份识别。本文以人员查询为例，完成从 Spring 配置到 HTTP 调用的过程。

<!-- truncate -->

## 准备示例

[Spring Boot 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)内置 H2 数据、登录页面、控制台和本文接口。启动后打开 `http://127.0.0.1:8080/`，以 `admin` / `example-password` 登录。

```bash
mvn -f example/dataway-spring-example/pom.xml spring-boot:run
```

## 接入 Spring

在 Spring Boot Web 应用中引入三个模块：

```xml title="pom.xml"
<!-- Spring MVC 入口与控制台 -->
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-spring</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
<!-- API 元数据存储 -->
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-meta-jdbc</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
<!-- SQL 片段执行器 -->
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataql-sqlproc</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
```

在应用的配置类中注册服务：

```java
@Bean
public DatawayConfig datawayConfig(IdentityProvider identityProvider,
        ConnectionProvider connections) {
    return new DatawayConfig()
            .identityProvider(identityProvider)
            .attachment(ConnectionProvider.class, connections);
}

@Bean
public ApiDataAccessLayer metadata(DataSource source) {
    return new JdbcDataAccessLayer(source);
}

@Bean
public IdentityProvider identityProvider() {
    return new RequestIdentityProvider("host.identity");
}

@Bean
public ConnectionProvider connectionProvider(ApplicationContext context) {
    return new SpringTransactionProvider(context);
}
```

示例的 LoginInterceptor 校验登录 Cookie，从用户表得到身份后写入请求属性 `host.identity`。IdentityProvider 读取该属性；SpringTransactionProvider 按 SQL Hint 中的名称查找 DataSource，未指定名称时使用主数据源。

DataSource、建表和初始数据位于 [DatabaseConfiguration](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/config/DatabaseConfiguration.java)。元数据表保存 API 定义和发布记录，业务库 ds1 中的 example_people 提供 Alice 和 Bob 两条记录。

```yaml title="application.yml"
dataway:
  api-enabled: true
  api-prefix: /api
  admin-enabled: true
  admin-prefix: /admin/api
  admin-ui: /admin
  docs-enabled: true
  docs-prefix: /docs
```

## 编写并发布查询

本篇接口为 `POST /blog/spring-query`，类型选择 DataQL，Parameters 内容如下：

```json title="请求参数"
{}
```

```javascript title="接口脚本"
hint FRAGMENT_SQL_DATA_SOURCE = 'ds1';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
var people = @@selectSql()<%
    SELECT id, name FROM example_people ORDER BY id
%>;
return people();
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
curl -i -b cookies.txt http://127.0.0.1:8080/api/blog/spring-query \
  -H 'Content-Type: application/json' \
  -d '{}'
```

返回两条人员记录：

```json
[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]
```

继续增加查询条件时，可在脚本中定义 SQL 片段参数。应用已有的业务校验和事务服务仍可通过 UDF 接入，脚本适用于能清晰表达的数据查询与组合逻辑。

## 源码与验证

[本篇脚本、参数和接口选项](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example/src/main/resources/blog/spring-query)随示例提供；[BlogApiService](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/service/BlogApiService.java)负责发布，`BlogApiTest` 启动真实 Web 服务和 H2 验证请求。

```bash
mvn -f example/dataway-spring-example/pom.xml test
```

原文：[《Dataway 让 Spring Boot 不再需要 Controller、Service、DAO、Mapper》](https://my.oschina.net/ta8210/blog/3234639)。可查阅[作者的同文发布](https://www.cnblogs.com/ta8210/p/12981140.html)。本文的配置和代码按当前仓库实现重写。

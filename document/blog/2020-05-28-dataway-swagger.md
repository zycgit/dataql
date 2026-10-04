---
slug: dataway-swagger2
title: "Dataway 整合 Swagger2，让 API 管理更顺畅"
description: "Dataway 可以把已发布且启用的接口生成为 Swagger 2.0 文档，Swagger UI 加载文档后便能展示参数并发起请求。本文让一个消息接口在控制台发布后，直接进入 Swagger UI 调试。"
authors: [zyc]
tags: [DataQL, Dataway, SpringBoot]
topics: [dataway]
language: zh-cn
updated: 2026-10-04
---

Dataway 可以把已发布且启用的接口生成为 Swagger 2.0 文档，Swagger UI 加载文档后便能展示参数并发起请求。本文让一个消息接口在控制台发布后，直接进入 Swagger UI 调试。

<!-- truncate -->

## 准备示例

[Spring Boot 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)内置 H2 数据、登录页面、控制台和本文接口。启动后打开 `http://127.0.0.1:8080/`，以 `admin` / `example-password` 登录。

```bash
mvn -f example/dataway-spring-example/pom.xml spring-boot:run
```

## 启用文档入口

```yaml title="application.yml"
dataway:
  api-enabled: true
  docs-enabled: true
  docs-prefix: /docs
```

Swagger 2.0 文档地址为 `/docs/swagger2.json`，OpenAPI 文档地址为 `/docs/openapi.json`。示例已将 Swagger UI 静态资源打入应用，并用 DatawayConfig.documentServer 设置业务 API 前缀 `/api`。

## 发布一个消息接口

本篇接口为 `POST /blog/swagger`，类型选择 DataQL，Parameters 内容如下：

```json title="请求参数"
{
  "message": "Hello Swagger"
}
```

```javascript title="接口脚本"
return {'message': ${message}};
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
curl -i -b cookies.txt http://127.0.0.1:8080/api/blog/swagger \
  -H 'Content-Type: application/json' \
  -d '{"message":"Hello Swagger"}'
```

## 在 Swagger UI 中调用

登录示例首页后，打开 `http://127.0.0.1:8080/swagger/index.html?spec=swagger2`。展开 **POST /blog/swagger**，点击 **Try it out**，输入 `{"message":"Hello Swagger"}` 并点击 **Execute**。请求发送到 `/api/blog/swagger`，返回：

```json
{"message":"Hello Swagger"}
```

![Swagger UI 的请求和响应区域](/img/dataway/quickstart-invoke.png)

这张图展示相同 Swagger UI 的操作区域；本文选择的接口为 `/blog/swagger`。

示例 initializer.js 根据 spec 参数选择文档，并复用登录 Cookie：

```javascript
const response = await fetch('../example/config', {
  method: 'POST', credentials: 'same-origin'
});
const configuration = await response.json();
const specification = new URLSearchParams(window.location.search).get('spec');
SwaggerUIBundle({
  url: specification === 'swagger2' ? configuration.swagger : configuration.openapi,
  dom_id: '#swagger-ui',
  withCredentials: true,
  validatorUrl: null,
  presets: [SwaggerUIBundle.presets.apis]
});
```

BlogApiService 为示例设置请求和响应 Schema。文档来自发布记录，修改接口后需要重新发布才能更新；接口停用后会从文档中移除。应用身份需具备 DOCUMENT 权限。

## 源码与验证

[本篇脚本、参数和接口选项](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example/src/main/resources/blog/swagger)随示例提供；[BlogApiService](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/service/BlogApiService.java)负责发布，`BlogApiTest` 启动真实 Web 服务和 H2 验证请求。

```bash
mvn -f example/dataway-spring-example/pom.xml test
```

原文：[《Dataway 整合 Swagger2，让 API 管理更顺畅》](https://my.oschina.net/ta8210/blog/4293622)。可查阅[作者的同文发布](https://www.cnblogs.com/ta8210/p/12981120.html)。本文的配置和代码按当前仓库实现重写。

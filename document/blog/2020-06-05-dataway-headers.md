---
slug: dataway-headers
title: "Dataway header 传参"
description: "请求头常用于传递链路编号、客户端版本等信息。本文读取 X-Trace-Id，把它放进 JSON 结果并写入响应头，让调用方能够关联一次请求与响应。"
authors: [zyc]
tags: [DataQL, Dataway, SpringBoot]
topics: [dataway]
language: zh-cn
updated: 2026-10-04
---

请求头常用于传递链路编号、客户端版本等信息。本文读取 X-Trace-Id，把它放进 JSON 结果并写入响应头，让调用方能够关联一次请求与响应。

<!-- truncate -->

## 准备示例

[Spring Boot 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)内置 H2 数据、登录页面、控制台和本文接口。启动后打开 `http://127.0.0.1:8080/`，以 `admin` / `example-password` 登录。

```bash
mvn -f example/dataway-spring-example/pom.xml spring-boot:run
```

## 读取请求头并回写

在控制台 Headers 面板增加 `X-Trace-Id: blog-001`；Parameters 面板填入 message。请求头与正文是两个独立通道，脚本分别使用 Web 函数和 `${message}` 读取。

本篇接口为 `POST /blog/headers`，类型选择 DataQL，Parameters 内容如下：

```json title="请求参数"
{
  "message": "Hello Header"
}
```

```javascript title="接口脚本"
import 'net.hasor.dataway.function.WebUdfSource' as web;
var trace = web.header('X-Trace-Id');
if (trace != null) {
    run web.setHeader('X-Trace-Id', trace);
}
return {'traceId': trace, 'message': ${message}};
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
curl -i -b cookies.txt http://127.0.0.1:8080/api/blog/headers \
  -H 'Content-Type: application/json' \
  -H 'x-trace-id: blog-001' \
  -d '{"message":"Hello Header"}'
```

响应正文如下，响应头中同时包含 `X-Trace-Id: blog-001`：

```json
{"traceId":"blog-001","message":"Hello Header"}
```

`web.header` 按忽略大小写的规则查找，传入 x-trace-id 同样有效；多值请求头可用 `web.headerValues` 读取列表。`setHeader` 覆盖同名响应头，追加多个值使用 `addHeader`。

## 在界面中验证

打开本篇接口，在 Headers 面板填写链路编号并执行。修改编号后重新执行，确认正文与响应头一起变化。普通脚本测试没有 HTTP 请求环境，完整源码通过真实 Spring MVC 请求验证这些函数。

Header 中的租户或用户编号属于客户端输入；身份验证由应用登录拦截器和 IdentityProvider 完成，不能直接把一个请求头值当作已认证身份。

## 源码与验证

[本篇脚本、参数和接口选项](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example/src/main/resources/blog/headers)随示例提供；[BlogApiService](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/service/BlogApiService.java)负责发布，`BlogApiTest` 启动真实 Web 服务和 H2 验证请求。

```bash
mvn -f example/dataway-spring-example/pom.xml test
```

原引用文章：[《Dataway header 传参》](https://my.oschina.net/ta8210/blog/4300558)，发布于 2020-06-05，转载自 [CSDN](https://blog.csdn.net/maple_son/article/details/105947993)。本文沿用原引用文章的日期，代码按当前请求头实现重写。

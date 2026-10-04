---
slug: dataway-response-format
title: "完美兼容老项目！Dataway 返回结构的全面控制"
description: "应用已经约定 ok、code、message、data 四个响应字段时，可以用 Structure 结果处理器配置同样的结构。本文让成功和脚本失败共用一份模板，并说明直接返回原始值的方式。"
authors: [zyc]
tags: [DataQL, Dataway, SpringBoot]
topics: [dataway]
language: zh-cn
updated: 2026-10-04
---

应用已经约定 ok、code、message、data 四个响应字段时，可以用 Structure 结果处理器配置同样的结构。本文让成功和脚本失败共用一份模板，并说明直接返回原始值的方式。

<!-- truncate -->

## 准备示例

[Spring Boot 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)内置 H2 数据、登录页面、控制台和本文接口。启动后打开 `http://127.0.0.1:8080/`，以 `admin` / `example-password` 登录。

```bash
mvn -f example/dataway-spring-example/pom.xml spring-boot:run
```

## 定义成功与失败

脚本成功时返回 message 对象；fail 为 true 时抛出带有 400 结果码的脚本异常。

本篇接口为 `POST /blog/response-format`，类型选择 DataQL，Parameters 内容如下：

```json title="请求参数"
{
  "message": "Hello Dataway",
  "fail": false
}
```

```javascript title="接口脚本"
if (${fail}) {
    throw 400, 'message rejected';
}
return {'message': ${message}};
```

```json title="API 选项"
{
  "resultHandler": "structure",
  "responseFormat": "{\"ok\":\"@resultStatus\",\"code\":\"@resultCode\",\"message\":\"@resultMessage\",\"data\":\"@resultData\"}"
}
```

示例启动时自动发布该接口。在控制台自行创建时，填写这些内容并点击 Save、Publish；修改已有脚本后也需重新发布。

## 发起 HTTP 请求

```bash title="登录示例应用"
curl -c cookies.txt -X POST http://127.0.0.1:8080/session/login \
  -d 'username=admin&password=example-password'
```

```bash
curl -i -b cookies.txt http://127.0.0.1:8080/api/blog/response-format \
  -H 'Content-Type: application/json' \
  -d '{"message":"Hello Dataway","fail":false}'
```

正常响应：

```json
{"ok":true,"code":0,"message":"OK","data":{"message":"Hello Dataway"}}
```

把请求中的 fail 改为 true，响应的 ok 为 false、code 为 400、message 为 `message rejected`。这里的 code 是脚本结果码，Structure 默认 HTTP 状态仍为 200。

## 调整模板与处理器

控制台选择 Structure，在 Structure 标签页填写：

```json
{
  "ok":"@resultStatus",
  "code":"@resultCode",
  "message":"@resultMessage",
  "data":"@resultData"
}
```

源码中的 options.json 使用 JSON 字符串保存 responseFormat，因此双引号需要转义。模板中的占位符保留布尔值、数字、对象的实际类型；普通字段值按字面输出。

选择 Raw Value 时直接输出脚本结果。需要设置 HTTP 状态、文件下载或其他响应协议时，实现 [ResultHandler](/docs/dataway/capabilities/result-handlers/custom) 并通过 DatawayConfig 注册。

## 源码与验证

[本篇脚本、参数和接口选项](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example/src/main/resources/blog/response-format)随示例提供；[BlogApiService](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/service/BlogApiService.java)负责发布，`BlogApiTest` 启动真实 Web 服务和 H2 验证请求。

```bash
mvn -f example/dataway-spring-example/pom.xml test
```

原文：[《完美兼容老项目！Dataway 返回结构的全面控制》](https://my.oschina.net/ta8210/blog/4275216)。可查阅[作者的同文发布](https://forum.springdoc.cn/t/topic/1894)。本文的配置和代码按当前仓库实现重写。

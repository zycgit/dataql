---
title: "5.4 文档生成"
description: "将已发布 API 导出为 OpenAPI 3.2.1 和 Swagger 2.0 标准文档。"
---

Dataway 将已发布且启用的 API 生成为标准接口文档，供 Swagger UI 等工具展示和调用。重新发布、停用或删除接口后，文档同步更新。

## 支持的标准

| 标准 | 默认文档地址 |
| --- | --- |
| [OpenAPI 3.2.1](https://spec.openapis.org/oas/v3.2.1.html) | `/docs/openapi.json` |
| [Swagger 2.0](https://spec.openapis.org/oas/v2.0.html) | `/docs/swagger2.json` |

两种文档均为 JSON，支持 GET、HEAD。Swagger 2.0 无法表达 `oneOf`、多类型 Schema、TRACE 等定义，包含这些定义时请使用 OpenAPI。

## 开启与获取

通过 `dataway.docs-enabled` 开启入口，`dataway.docs-prefix` 设置前缀。以 Spring 为例：

```yaml title="application.yml"
dataway:
  docs-enabled: true
  docs-prefix: /docs
```

请求需具备 `Operation.DOCUMENT` 权限。以下使用登录后保存的 Cookie 下载文档：

```bash title="下载 OpenAPI 文档"
curl -b cookies.txt http://127.0.0.1:8080/docs/openapi.json -o openapi.json
```

标题、业务版本和 API 对外地址见 [DatawayConfig](../configuration/core.md#api-documents)。

## Swagger UI 效果 {#swagger-ui}

三大框架的[示例工程](https://gitee.com/zycgit/dataql/tree/dev/example)均提供 `/swagger/index.html`。登录后展开接口，点击 Try it out，填写参数并点击 Execute，即可查看请求和响应。

![Swagger UI 调用效果：请求地址、HTTP 状态、JSON 正文与响应头](/img/dataway/quickstart-invoke.png)

---
title: "5.2.4 API 选项"
description: "配置 API 的参数包装、结果处理器、响应类型和响应模板。"
---

API 选项控制参数包装和结果格式。参数默认值由 [DatawayConfig](../../configuration/core.md#response) 提供，结果处理选项的默认值由所选处理器的构造配置提供，单个 API 的选项优先。

## 选项说明

| 选项 | 默认值 | 作用 |
| --- | --- | --- |
| `wrapAllParameters` | `false` | 将合并后的全部参数包装到一个对象中，对 DataQL 和 SQL 均生效 |
| `wrapParameterName` | `root` | 包装后的参数名 |
| `responseFormat` | Structure 构造配置或内置模板 | 定义响应字段及其取值，接收 JSON 对象字符串 |
| `resultHandler` | `structure` | 选择内置 `structure`、`raw`、`csv`、`text`、`verifyCode` 或应用注册的结果处理器 |

## 配置方式

在控制台 More Settings → API Options 中填写 JSON，点击 Apply Options。选项随 API 保存，发布后对外生效。界面设置见[可视化操作](../management.md#result-panel)。通过程序管理接口时，将选项写入 `ApiDefinition.options`，示例见[程序化管理](../programmatic.md)：

```json title="ApiDefinition.options"
{
  "wrapAllParameters": true,
  "wrapParameterName": "root",
  "resultHandler": "structure",
  "responseFormat": "{\"ok\":\"@resultStatus\",\"data\":\"@resultData\"}"
}
```

布尔选项使用 `true` 或 `false`。显式 `null`、错误类型、无效包装名和无效模板会被拒绝；省略选项即可使用应用默认值。

## 结果处理器 {#result-handler}

`resultHandler` 指定处理器名称，默认使用 `structure`。内置类型、Content-Type、使用示例和自定义扩展见[结果处理器](../result-handlers.md)。

## 参数包装 {#parameter-wrapping}

开启 `wrapAllParameters`，将 `wrapParameterName` 设置为 `root`。客户端仍提交原始参数：

```json title="请求参数"
{"id": 1, "name": "Ada"}
```

执行前，Dataway 将合并后的参数包装为：

```json title="脚本接收的参数"
{"root": {"id": 1, "name": "Ada"}}
```

DataQL 使用 `${root.id}` 读取，SQL 使用 `#{root.id}` 绑定。关闭包装时分别使用 `${id}`、`#{id}`。参数合并规则见[请求处理](../../principles/index.md#request-flow)。

包装名以英文字母或下划线开头，仅含英文字母、数字和下划线。修改包装名时，同步修改脚本中的参数访问路径。

## 结构化响应 {#response-structure}

`resultHandler=structure` 为默认处理器。脚本数据放入响应模板指定的字段，同时返回执行状态和耗时。

选择 Raw Value（`resultHandler=raw`）后，普通成功响应直接返回脚本数据。例如 `return {"message": "Hello Dataway"};` 返回：

```json title="Raw Value 响应"
{"message": "Hello Dataway"}
```

脚本错误、二进制和自定义响应的处理见[结果响应](response.md)。

## 响应模板 {#response-template}

`responseFormat` 接收 JSON 对象字符串，由 `structure` 处理器使用。默认模板为：

```json title="默认响应模板"
{
  "success": "@resultStatus",
  "message": "@resultMessage",
  "location": "@blockLocation",
  "code": "@resultCode",
  "lifeCycleTime": "@timeLifeCycle",
  "executionTime": "@timeExecution",
  "value": "@resultData"
}
```

字段名由模板决定，占位符提供字段值：

| 占位符 | 内容 |
| --- | --- |
| `@resultStatus` | 执行是否成功 |
| `@resultMessage` | 执行消息 |
| `@blockLocation`、`@codeLocation` | 错误位置 |
| `@resultCode` | 执行结果码 |
| `@timeLifeCycle` | 本次执行总耗时，单位为毫秒 |
| `@timeExecution` | 脚本执行耗时，单位为毫秒 |
| `@resultData` | 脚本返回值或错误数据 |

例如，使用 `ok` 和 `data` 作为响应字段：

```json title="自定义响应模板"
{
  "ok": "@resultStatus",
  "message": "@resultMessage",
  "data": "@resultData"
}
```

模板仅替换顶层占位符，嵌套对象和其他值作为固定内容保留。JSON 响应省略值为 `null` 的字段。

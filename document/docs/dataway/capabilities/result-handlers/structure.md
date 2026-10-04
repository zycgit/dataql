---
title: "5.3.1 Structure"
description: "Structure 按响应模板包装脚本执行结果，是 Dataway 默认的结果处理器。"
---

## 介绍

Structure 按响应模板包装脚本执行结果，是 Dataway 默认的结果处理器。

## 作用

普通结果使用 `application/json; charset=utf-8`，默认包含成功标识、结果码、耗时和 `value`。脚本失败时按同一模板输出错误，默认 HTTP 状态为 200。

成功返回二进制或 `ResultInfo` 时直接输出，详见[二进制响应](../development/response.md#binary-response)。

## 用法

示例 `POST /result-structure` 的参数为 `{"message":"Hello Dataway"}`：

```javascript
return {"message": ${message}};
```

响应示例中，`value` 为脚本结果，耗时单位为毫秒：

```json title="响应正文"
{
  "success": true,
  "message": "OK",
  "code": 0,
  "lifeCycleTime": 2,
  "executionTime": 1,
  "value": {"message": "Hello Dataway"}
}
```

## 如何配置

控制台选择 Structure，或设置以下 API 选项，保存并发布：

```json title="接口选项"
{"resultHandler": "structure"}
```

`responseFormat` 配置响应模板，可在控制台 Structure 标签页中修改。默认模板和占位符见 [API 选项](../development/options.md#response-template)。

应用默认模板通过处理器构造方法设置：

```java
import java.util.Map;
import net.hasor.dataway.result.structure.StructureResultHandler;

String template = """
        {"ok":"@resultStatus","data":"@resultData"}
        """;
StructureResultHandler handler = new StructureResultHandler(Map.of("responseFormat", template));
config.resultHandler("structure", handler);
```

API 的 `responseFormat` 优先于构造默认值；Structure 标签页留空时沿用默认模板。

---
title: "5.3.6 自定义结果处理器"
description: "应用通过实现 `ResultHandler` 自定义结果格式、HTTP 状态和响应头。"
---

## 介绍

应用通过实现 `ResultHandler` 自定义结果格式、HTTP 状态和响应头。

## 作用

处理器接收 `ResultContext`，包含返回值、成功标识、结果码、消息、错误位置、耗时、异常和最终配置，通过 `ResultInfo` 返回响应。

处理器抛出异常或返回 null 时，Dataway 输出 Structure 失败结构，处理器不会被再次调用。

| 设置方式 | 作用 |
| --- | --- |
| `ResultInfoUtils.json(status, data)` | 设置 HTTP 状态，按 JSON 输出数据 |
| `ResultInfoUtils.toResult(binaryModel)` | 输出二进制模型，上传对象附带文件名与内容类型 |
| `ResultInfoUtils.binary(contentType, bytes)` | 原样输出字节，并设置 Content-Type |
| `ResultInfoUtils.stream(contentType, stream)` | 输出输入流，完成后关闭流 |
| `response.setStatus(status)` | 设置 HTTP 状态，默认 200 |
| `response.getHeaders().put(name, value)` | 设置响应头 |

## 用法

以下处理器在成功时输出 JSON，从最终配置中读取 HTTP 状态，并添加 `X-Result: created`；失败时交给 Structure：

```java title="CreatedResultHandler.java"
package example;

import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.ResultInfoUtils;
import java.util.Map;
import net.hasor.dataway.result.AbstractResultHandler;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.result.structure.StructureResultHandler;

public class CreatedResultHandler extends AbstractResultHandler {
    public CreatedResultHandler(Map<String, ?> defaults) {
        super(defaults);
    }

    @Override
    public ResultInfo handle(ResultContext context) {
        if (!context.isSuccess()) {
            return new StructureResultHandler().handle(context);
        }
        int status = ((Number) context.getOptions().get("status")).intValue();
        ResultInfo response = ResultInfoUtils.json(status, context.getValue());
        response.getHeaders().put("X-Result", "created");
        return response;
    }
}
```

## 如何配置

在应用的 `DatawayConfig` 中将处理器注册为 `created`：

```java title="注册结果处理器"
Map<String, Object> defaults = Map.of("status", 201);
config.resultHandler("created", new CreatedResultHandler(defaults));
```

在 API 的 `resultHandler` 选项中选择 `created`，该接口的执行结果就会交给 `CreatedResultHandler`：

```json title="接口选项"
{"resultHandler": "created", "status": 202}
```

注册后可在控制台下拉框选择。本例 API 将默认状态 201 覆盖为 202。同名注册替换已有处理器，`default` 为保留名称。

`AbstractResultHandler` 负责配置合并和隔离。需要校验时，重写 `prepareOptions`，检查 `super.prepareOptions(options)` 返回的配置。

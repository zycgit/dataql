---
title: "9.1 自定义函数"
---

自定义 `Udf` 将应用能力提供给 DataQL，适合单个查询函数、业务服务调用和数据转换。脚本通过注册名称直接调用函数。

## 实现函数

实现 `call(Hints, UdfParams)`，通过 `allParams()` 读取脚本实参。下面的函数接收一个字符串并返回问候语。
```java title="GreetingUdf.java"
package com.example.dataway;

import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.UdfParams;

public class GreetingUdf implements Udf {
    @Override
    public Object call(Hints hints, UdfParams params) {
        Object[] arguments = params.allParams();
        if (arguments.length != 1 || !(arguments[0] instanceof String name)) {
            throw new IllegalArgumentException("greeting requires one string argument");
        }
        return "Hello " + name;
    }
}
```


## 注册配置

`config` 表示应用注册的 `DatawayConfig`，以下配置在创建 Dataway 前完成。
```java
import com.example.dataway.GreetingUdf;

config.function("greeting", new GreetingUdf());
```


## 脚本调用


```javascript
return greeting('Dataway');
```
返回 `"Hello Dataway"`，最终响应由所选结果处理器输出。

## 使用说明

`Hints` 提供本次执行选项，`UdfParams` 包含按位置排列的实参。函数可返回字符串、数值、集合等 DataQL 支持的值；二进制用法见[结果响应](../capabilities/development/response.md#binary-response)。

注册的函数实例可被多个查询使用，应避免在实例字段中保存当前请求数据。需要按命名空间组织多个函数时，使用[函数库](libraries.md)。

## 读取执行上下文 {#execution-context}

应用可通过 Hint 向 UDF 传递对象，在 `call` 中用 `hints.getHint("app.context")` 读取。设置方式见[核心接口：Hint](../dataql-engine/core.md#hint)。UDF 收到的 Hint 不允许修改键值，其中的对象仍是原引用，使用时需注意共享状态。

Dataway 使用 `DATAWAY_REQUEST`、`DATAWAY_RESPONSE` 传递本次请求和响应，执行完成后移除。应用应使用独立的 Hint 名称。Header、Cookie 等脚本操作见 [Web 函数库](../../dataql/funx/web.md)。

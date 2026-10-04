---
title: "10.3 API 拦截器"
description: "通过 ApiInterceptor 校验脚本参数、统计执行和处理返回结果。"
---

`ApiInterceptor` 在 DataQL 执行前后工作，可用于参数校验、执行统计、事务和结果处理。HTTP、[Java 调用](../capabilities/development/java.md)与管理端调试共用此拦截链，SQL 类型脚本也会经过它。

## 注册与使用

通过 `DatawayConfig.apiInterceptor(...)` 注册。下面为 `/person` 接口检查必填参数：

```java title="校验调用参数"
config.apiInterceptor((context, chain) -> {
    if ("/person".equals(context.definition().getPath())
            && context.parameters().get("id") == null) {
        throw new IllegalArgumentException("id is required");
    }
    return chain.proceed(context);
});
```

`chain.proceed(context)` 将上下文传给下一个拦截器，最后执行脚本。此例使用未包装的参数；开启 Wrap All Parameters 时，从包装对象中读取，见 [API 选项](../capabilities/development/options.md)。

## 上下文

脚本完成编译和参数准备后进入拦截链，`ApiInterceptorContext` 提供以下信息：

| 方法 | 内容 |
| --- | --- |
| `definition()` | 当前执行的 API 定义，可读取标识、请求方法、路径和脚本类型；已发布 API 使用发布快照 |
| `releaseId()` | 本次执行的发布 ID；编辑内容和草稿调试为 `null` |
| `parameters()` | 本次执行参数，已合并 CustomizeScope 默认值并应用参数包装 |
| `operation()` | 业务调用为 `INVOKE`，Perform、Smoke 调试为 `DEBUG` |
| `identity()` | 当前用户身份及属性；Java 调用为匿名身份 |
| `source()` | 调用来源：`PROGRAMMATIC` 程序调用、`DEBUG` 调试、`HTTP` API 请求 |

调用来源使用 `ApiCallSource` 枚举，与身份和操作类型独立。Perform、Smoke 为 `DEBUG`；Interface 列表页调用和外部 API 请求均为 `HTTP`。

```java title="识别程序调用"
import net.hasor.dataway.service.script.ApiCallSource;

config.apiInterceptor((context, chain) -> {
    if (context.source() == ApiCallSource.PROGRAMMATIC) {
        System.out.println("Programmatic API call: " + context.definition().getId());
    }
    return chain.proceed(context);
});
```

## 执行与返回

多个拦截器按注册顺序进入，按相反顺序返回：`A → B → 脚本 → B → A`。在 `proceed` 前后可加入校验、计时及事务逻辑，`finally` 用于释放资源。

正常脚本执行返回 `QueryResult`，随后交给所选结果处理器生成响应。拦截器直接返回普通对象时，将该对象转为 JSON；返回 `ResultInfo` 时使用其状态码和内容。这两种返回方式均跳过结果处理器。

```java title="直接返回响应"
import java.util.Map;
import net.hasor.dataway.service.ResultInfoUtils;

config.apiInterceptor((context, chain) -> {
    return ResultInfoUtils.json(202, Map.of("accepted", true));
});
```

此例返回 HTTP 202，后续拦截器与脚本均不执行。拦截链或脚本抛出的未处理执行异常，由 `DatawayQuery` 转为失败结果并交给结果处理器。路由、鉴权和编译发生在拦截链外。

管理端调试在进入此链之前，还会经过[管理拦截器](admin-interceptors.md)。控制台静态资源和文档请求不经过 API 拦截链。

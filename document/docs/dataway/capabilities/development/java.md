---
title: "5.2.5 Java 调用"
description: "通过 ApiService 按路径或 API 标识调用已发布的接口。"
---

`ApiService` 供应用服务、定时任务等 Java 代码直接调用 Dataway API，无需发送 HTTP 请求。

:::info 调用范围
只能调用已发布且启用的 API。草稿修改在再次发布后生效；未发布、已停用或不存在的 API 无法调用。
:::

## 准备接口

以下示例使用已发布的 `POST /hello`，API 标识为 `hello`，脚本如下。创建和发布步骤见[程序化管理](../programmatic.md)。

```javascript title="接口脚本"
return {"message": ${message}};
```

## 按路径调用

从框架容器获取已配置的 `Dataway`，通过 `getApiService()` 取得调用接口。`invokeByPath` 接收请求方法、API 路径和参数。

```java title="按请求方法和路径调用"
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.script.ApiService;

ApiService api = dataway.getApiService();
Map<String, Object> parameters = Map.of("message", "Hello Dataway");

ResultInfo result = api.invokeByPath("POST", "/hello", parameters);
```

路径使用 API 定义中的 `/hello`，不包含宿主的 `/api` 等入口前缀。请求方法不区分大小写，路径区分大小写。

Java 调用属于应用内部调用，无需传递用户身份，也不经过 `IdentityProvider` 和 `AuthorizationCheck`。HTTP 入口仍负责身份识别和权限校验。

## 按 API 标识调用

`invokeById` 根据 API 标识查找当前启用的发布版，无需提供请求方法和路径。该标识对应 `ApiDefinition.id`，不是发布记录或历史记录的 ID。

```java title="按 API 标识调用同一接口"
String apiID = "hello";
ResultInfo resultById = api.invokeById(apiID, parameters);
```

两种方式共用 [API 拦截器](../../configuration/api-interceptors.md)和脚本执行流程，参数包装及结果处理器使用已发布 API 的配置。拦截器中的身份为匿名身份，`context.source()` 返回 `ApiCallSource.PROGRAMMATIC`，可据此识别程序调用。

## 获取结果

`ResultInfo` 保存响应状态、响应头和处理后的数据。`getData()` 返回 Java 对象，数据形态由[结果处理器](../result-handlers.md)决定。

```java title="读取默认 Structure 结果"
int status = result.getStatus();
Map<String, String> headers = result.getHeaders();
Map<?, ?> body = (Map<?, ?>) result.getData();

if (status >= 400 || !Boolean.TRUE.equals(body.get("success"))) {
    throw new IllegalStateException(String.valueOf(body.get("message")));
}

Object value = body.get("value");
System.out.println(value); // {message=Hello Dataway}
```

此例使用默认 Structure 模板。选择 Raw、Text 或自定义结果处理器时，按其返回类型读取数据；输入流及二进制资源由调用方读取并关闭，详见[结果响应](response.md)。

找不到启用的发布版时，抛出状态码为 `404` 的 `DatawayException`。脚本执行失败按结果处理器生成失败结果，使用 Structure 时检查 `success`。

Java 调用不绑定 HTTP 请求和响应，Header、Cookie 等 Web 函数没有宿主 HTTP 上下文。

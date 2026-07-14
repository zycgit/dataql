---
id: preexecutechainspi
sidebar_position: 6
title: f.PreExecuteChainSpi
description: DataQL Dataway SPI,PreExecuteChainSpi
---
# PreExecuteChainSpi

当一个API发起调用之后，可以通过API请求拦截器处理一些特殊的逻辑。比方说下面这些场景：
- 所有 API 请求都加入某个固定的参数
- 通过拦截器实现接口权限控制
- 配合 ResultProcessChainSpi 对接口进行结果缓存

```js
// 处理逻辑
public class MyPreExecuteChainSpi implements PreExecuteChainSpi {
    public void preExecute(ApiInfo apiInfo, BasicFuture<Object> future) {
        ...
    }
}

// 注册接口拦截器
public class ExampleModule implements Module {
    public void loadModule(ApiBinder apiBinder) throws Throwable {
        apiBinder.bindSpiListener(PreExecuteChainSpi.class, new MyPreExecuteChainSpi());
    }
}
```

:::tip
查询执行拦截器接口名为 net.hasor.dataway.spi.PreExecuteChainSpi 它是一个 ChainSpi。这意味着 PreExecuteChainSpi 可以串成一个串来执行。
:::

所有 API 请求都加入某个固定的参数

```js
apiBinder.bindSpiListener(PreExecuteChainSpi.class, (apiInfo, future) -> {
    apiInfo.getParameterMap().put("self", "me");
});
```

没有权限抛出异常

```js
apiBinder.bindSpiListener(PreExecuteChainSpi.class, (apiInfo, future) -> {
    String apiPath = apiInfo.getApiPath();
    String apiMethod = apiInfo.getMethod()
    if (...) {
        // （方式1）通过 future 设置异常信息
        future.failed(new StatusMessageException(401, "not power"));
        // （方式2）或者直接 throw 一个异常
        throw new StatusMessageException(401, "not power");
    }
});

// Result
// {
//   "success": false,
//   "message": "not power",
//   "code": 401,
//   "lifeCycleTime": 42,
//   "executionTime": -1,
//   "value": "not power"
// }
```

返回预先准备好的数据

```js
apiBinder.bindSpiListener(PreExecuteChainSpi.class, (apiInfo, future) -> {
    String apiPath = apiInfo.getApiPath();
    String apiMethod = apiInfo.getMethod()
    if (...) {
        future.completed(...);
    }
});

// Result
// {
//   "success": true,
//   "message": "OK",
//   "code": 0,
//   "lifeCycleTime": 22,
//   "executionTime": 21,
//   "value": ...
// }
```
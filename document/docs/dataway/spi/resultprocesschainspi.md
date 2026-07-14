---
id: resultprocesschainspi
sidebar_position: 7
title: g.ResultProcessChainSpi
description: DataQL Dataway SPI,ResultProcessChainSpi
---
# ResultProcessChainSpi

一个已经发布的接口被调用之后，一定会触发这个拦截器。而 ResultProcessChainSpi 拦截器的处理有两个方法，分别应对了两个不同的情况：
- callAfter：结果拦截，用于处理 Query 正确执行之后的二次结果处理。
- callError：异常拦截，当 Query 执行发生异常时。

:::tip
异常拦截器十分强大，除了 DataQL 执行异常之外。它还能拦截 PreExecuteChainSpi 的异常。甚至它还可以拦截自己 callAfter 过程引发的异常。

查询执行拦截器接口名为 net.hasor.dataway.spi.ResultProcessChainSpi 它也是一个 ChainSpi。
:::

```js title='响应结果改写'
// 所有返回的结果，都把 API 的 Method 和 path 返回
apiBinder.bindSpiListener(ResultProcessChainSpi.class, new ResultProcessChainSpi() {
    public Object callAfter(boolean formPre, ApiInfo apiInfo, Object result) {
        return new HashMap<String, Object>() {{
            put("method", apiInfo.getMethod());
            put("path", apiInfo.getApiPath());
            put("result", result);
        }};
    }
});

// DataQL 查询
//   return 123

// Result
// {
//   "success": true,
//   "message": "OK",
//   "code": 0,
//   "lifeCycleTime": 14,
//   "executionTime": 8,
//   "value": {
//     "method": "POST",
//     "path": "/api/demos/find_user_by_name_post",
//     "result": 123
//   }
// }
```

```js title='异常统一处理'
// 所有返回的结果，都把 API 的 Method 和 path 返回
apiBinder.bindSpiListener(ResultProcessChainSpi.class, new ResultProcessChainSpi() {
    public Object callError(boolean formPre, ApiInfo apiInfo, Throwable e) {
        return new HashMap<String, Object>() {{
            put("method", apiInfo.getMethod());
            put("path", apiInfo.getApiPath());
            put("errorMessage", e.);
        }};
    }
});

// DataQL 查询
//   throw 123
//
// Result
// {
//   "success": false,
//   "message": "0 : 123",
//   "code": 0,
//   "lifeCycleTime": 320,
//   "executionTime": 39,
//   "value": {
//     "path": "/api/demos/find_user_by_name_post",
//     "method": "POST",
//     "errorMessage": "0 : 123"
//   }
// }
```

```js title='实现调用缓存'
public class ApiCacheSpi implements PreExecuteChainSpi, ResultProcessChainSpi {
    private Map<String,Object> cacheMap = ... // for example

    public void preExecute(ApiInfo apiInfo, BasicFuture<Object> future) {
        String cacheKey = ...
        if (this.cacheMap.containsKey(cacheKey)) {
            Object cacheValue = cacheMap.get(cacheKey);
            future.completed(cacheValue);
            return;
        }
    }

    public Object callAfter(boolean formPre, ApiInfo apiInfo, Object result) {
        // formPre 为 true，表示 preExecute 已经处理过。
        // apiInfo.isPerform() 为 true 表示，API 调用是从 UI 界面发起的。
        if (formPre || apiInfo.isPerform()) {
            return result;
        }
        //
        String cacheKey = ...
        this.cacheMap.put(cacheKey, result);
        return result;
    }
}
```

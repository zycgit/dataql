---
id: use-in-program
sidebar_position: 2
title: a.在程序中调用
description: DataQL Dataway 在程序中调用
---
# 在程序中调用

Dataway 在 4.1.4 版本开始提供了 DatawayService 接口。通过这个接口，允许应用通过这个接口来调用界面中配置的 DataQL 查询。

使用这个功能首先需要获取到 DatawayService 接口：

```js
AppContext appContext  = ...; // 如果环境中已经存在 Hasor 上下文，那么就通过一来注入来获取。
DatawayService dataway = appContext.getInstance(DatawayService.class);
```

接着通过 API 的 Method 和 Path 就可以直接调用它了，需要注意的是被调用的 DataQL API 必须是处于发布状态的。

```js
// 参数
Map<String, Object> paramData = new HashMap<>() {{
    put("userName", "1");
}}

// 结果
Map<String, Object> result = dataway.invokeApi("post", "/api/demos/find_user_by_name", paramData);
```
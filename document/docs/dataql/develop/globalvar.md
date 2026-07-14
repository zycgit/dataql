---
id: globalvar
sidebar_position: 2
title: b.全局变量
description: DataQL 开发手册，全局变量。添加全局变量有两种方式，两种方式没有分别。
---
# 全局变量

添加全局变量有两种方式，两种方式没有分别。

```js title='方式1：在 QueryModule 中初始化环节添加'
AppContext appContext = Hasor.create().build((QueryModule) apiBinder -> {
    apiBinder.addShareVarInstance("global_var", "g1");
});
```

```js title='方式2：通过 DataQL 接口添加'
DataQL dataQL = appContext.getInstance(DataQL.class);
dataQL.addShareVarInstance("global_var", "g2");
```

```js title'执行 DataQL 查询获取全局变量'
return global_var;
```
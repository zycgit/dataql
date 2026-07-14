---
id: udf
sidebar_position: 5
title: e.UDF
description: DataQL 数据模型、UdfModel模型
---

# UdfModel

当 DataQL 查询返回一个 Udf 函数或者 Lambda 函数时，就会得到一个 `UdfModel`，而它事实上就是一个 UDF。

通过 `UdfModel` 允许我们在程序中调用 DataQL 中定义的函数。例如：

```js
var foo = ()-> { return 123; }
return foo;
```

:::tip
需要提示的是通过 `UdfModel` 形式调用 UDF 的返回值得到的将会是 `DataModel` 类型结果。
:::

---
id: flow
title: 3. 流程与函数
---

本章说明如何控制执行顺序和复用逻辑。脚本函数通过 `->` 定义，库和资源通过 `import` 导入，其他语言的代码通过 `@@` 片段调用。

- [控制流程](statements.md)：执行、分支、断言、返回和退出。
- [函数](function.md)：参数、匿名函数、外层变量和递归。
- [导入与复用](imports.md)：调用函数库与其他 DataQL 脚本。
- [代码片段](fragment.md)：声明执行器名称、传递参数及批量调用。

```js
var shipping = (amount) -> {
    if (amount >= 100) {
        return 0;
    }
    return 10;
};
return shipping(80);
```

结果为 `10`。对列表中的各项应用同一处理逻辑时，可使用[结果转换](transform.md)或集合函数。

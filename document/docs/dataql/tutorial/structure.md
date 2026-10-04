---
id: structure
title: 1.2 脚本结构
---

脚本由可选的执行选项、可选的导入和执行语句组成，顺序为 `hint → import → 执行语句`。

```js
hint INDEX_OVERFLOW = 'null';
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;

var values = [10, 20];
return {"count": collect.size(values), "missing": values[5]};
```

结果对象中 `count` 为 `2`，`missing` 为 `null`。JSON 输出是否保留空值字段由宿主的序列化方式决定。

## 声明与执行

- `hint` 配置执行选项，位于脚本或语句块开头，详见[执行选项](../hints/index.md)。
- `import` 导入函数库、对象或脚本资源，位于顶层 Hint 之后，详见[导入与复用](../syntax/imports.md)。
- `var` 保存表达式结果，`run` 执行表达式并忽略结果，`return` 返回结果。
- `{...}` 将多条语句组成一个代码块，用于分支和函数体。

脚本按顺序执行，函数和代码片段在调用时执行。`return` 结束当前函数，顶层 `return` 结束查询；`exit` 从函数内部结束整个查询，详见[控制流程](../syntax/statements.md)。

## 名称与格式

关键字和变量名区分大小写，对象键使用字符串。示例统一保留分号，分支和函数体使用完整花括号。注释、转义和标识符规则见[词法与书写规则](../syntax/lexical.md)。

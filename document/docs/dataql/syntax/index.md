---
id: index
slug: /dataql/syntax
title: 2. 语言基础
---

本章说明脚本的基本组成：记号如何书写、值有哪些类型、变量如何绑定，以及表达式如何计算。

- [词法与书写规则](lexical.md)：注释、关键字、标识符和字符串转义。
- [类型与字面量](types.md)：布尔、数值、字符串、空值、对象和列表。
- [数值与精度](numbers.md)：进制、数值宽度与舍入。
- [变量与赋值](setter.md)：声明、重新绑定和构造新数据。
- [表达式与运算符](expression.md)：算术、比较、逻辑、位运算和条件选择。

```js
var price = 20;
var quantity = 3;
var total = price * quantity;
return {"total": total, "discounted": total >= 50};
```

结果为 `{"total":60,"discounted":true}`。变量保存值，表达式计算新值，对象组织结果字段。

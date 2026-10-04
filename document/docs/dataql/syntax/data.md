---
id: data
title: 4. 数据访问与转换
---

DataQL 使用路径读取数据，用 `=>` 将输入整理为需要的结果结构。外部参数、当前转换元素和本地变量各有对应的访问方式。

- [字段与下标](getter.md)：对象字段、列表索引、缺失值与越界处理。
- [参数与上下文](valuescope.md)：`${name}` 等参数域，以及转换中的 `$`、`@`、`#`。
- [结果转换](transform.md)：字段筛选、改名、计算和嵌套转换。

```js
var order = {"id": 10, "customer": {"name": "Alice"}, "items": [{"price": 20, "count": 3}]};
return order => {
    "id",
    "customerName": customer.name,
    "lines": items => [{"amount": price * count}]
};
```

```json
{"id":10,"customerName":"Alice","lines":[{"amount":60}]}
```

对象模板控制字段，列表模板逐项转换。筛选、排序和分组使用[集合函数](../funx/collect.md)。

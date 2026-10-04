---
id: setter
title: 2.4 Variables and assignment
description: DataQL 赋值的语法与用法。
---

`var` 可以定义变量，也可以将已有变量重新绑定到一个值。语言不提供对象字段或列表下标的直接赋值语句。

## 重新绑定变量

```js
var value = [1, 2, 3];
var value = {"count": 3};
var value = value.count + 1;
return value;
```

结果为 `4`，赋值右侧可以访问该变量之前的值。

## 修改数据结构

通过[结果转换](./transform.md)生成新的结构，保留需要的字段并替换指定值。

```js
var user = {"name": "Alice", "age": 20};
var updated = user => {"name", "age": age + 1};
return [user, updated];
```

结果中的两个对象分别包含 `age: 20` 和 `age: 21`。UDF 可以提供修改宿主数据的能力，其副作用由具体函数决定。

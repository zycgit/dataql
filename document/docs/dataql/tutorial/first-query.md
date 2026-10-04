---
id: first-query
title: 1.1 第一个脚本
---

下面的脚本可以分别执行，不依赖数据库或应用自定义函数。在 Dataway 编辑页选择 DataQL，粘贴脚本并点击执行。文中的结果为脚本返回值；控制台选择 Structure 时，返回值位于 `value` 字段。

## 返回一个值

```js
return 'Hello, DataQL';
```

结果为 `"Hello, DataQL"`。字符串使用引号，`return` 指定脚本结果，分号分隔语句。

## 使用变量和表达式

```js
var price = 20;
var quantity = 3;
return {"quantity": quantity, "total": price * quantity};
```

```json
{"quantity":3,"total":60}
```

`var` 定义变量，`*` 计算乘积，`{...}` 构造对象。对象键使用字符串，值可以是变量或表达式。

## 转换列表

```js
var people = [{"name": "Alice", "age": 25}, {"name": "Bob", "age": 30}];
return people => [{"name", "nextAge": age + 1}];
```

```json
[{"name":"Alice","nextAge":26},{"name":"Bob","nextAge":31}]
```

`=> [...]` 为每个元素生成结果。`"name"` 保留同名字段，`"nextAge": age + 1` 计算一个新字段。

## 定义函数

```js
var label = (name) -> {
    return 'Hello, ' + name;
};
return label('Alice');
```

结果为 `"Hello, Alice"`。`->` 定义函数，括号中的名称接收调用参数。

## 导入函数库

```js
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
var people = [{"name": "Alice", "age": 25}, {"name": "Bob", "age": 30}];
var selected = collect.filter(people, (person) -> {
    return person.age >= 30;
});
return selected => [{"name"}];
```

结果为 `[{"name":"Bob"}]`。`import` 为函数库指定名称，`filter` 使用传入的函数筛选数据，再由转换模板选择返回字段。

接下来阅读[脚本结构](structure.md)，了解语句的排列方式；外部参数的写法见[参数与上下文](../syntax/valuescope.md)。

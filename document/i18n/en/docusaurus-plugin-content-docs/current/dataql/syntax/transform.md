---
id: transform
title: 4.3 Result transformation
description: DataQL 结果转换的语法与用法。
---

`=>` 按指定结构生成结果，可选择字段、改名、计算新字段和转换嵌套数据。右侧的 `{...}` 表示对象结构，`[...]` 表示列表结构。

## 组装对象与列表

```js
var name = 'Alice';
var age = 20;
return {"user": {"name": name, "age": age}, "tags": ["java", "dataql"]};
```

## 对象转换

省略字段的值表示读取同名变量或当前对象字段；明确读取当前字段时可写 `#.name`。

```js
var user = {"name": "Alice", "age": 20, "enabled": true};
return user => {
    "name",
    "nextAge": age + 1,
    "status": enabled ? 'enabled' : 'disabled'
};
```

结果为 `{"name":"Alice","nextAge":21,"status":"enabled"}`。

## 列表转换

列表模板为每个输入元素生成结果，`#` 表示当前元素。

```js
var users = [{"name": "Alice", "age": 20}, {"name": "Bob", "age": 30}];
return users => [{"userName": name, "age"}];
```

结果为 `[{"userName":"Alice","age":20},{"userName":"Bob","age":30}]`。

```js
var users = [{"name": "Alice"}, {"name": "Bob"}];
return users => [name];
```

结果为 `["Alice", "Bob"]`。普通值列表也可以转换成对象列表：

```js
var names = ['Alice', 'Bob'];
return names => [{"name": #}];
```

列表使用对象模板时只转换第一项，例如 `users => {"name"}`；对象使用列表模板时可生成单元素列表，例如 `user => [#]`。

## 嵌套转换

```js
var matrix = [[1, 2], [3, 4]];
return matrix => [# => [# * 10]];
```

结果为 `[[10,20],[30,40]]`。访问外层数据的方式见[值域](./valuescope.md)。

转换模板本身不提供过滤条件，筛选数据可以先调用[集合函数](../funx/collect.md)的 `filter`。

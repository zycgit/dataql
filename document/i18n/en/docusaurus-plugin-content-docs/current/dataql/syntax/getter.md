---
id: getter
title: 4.1 Fields and indexes
description: DataQL 取值的语法与用法。
---

对象字段可以通过点号或字符串下标读取，列表通过数字下标读取。下标也可以使用变量和表达式。

## 对象与列表

```js
var users = [{"name": "Alice"}, {"name": "Bob"}];
var key = 'name';
return [users[0].name, users[1][key], users[-1].name];
```

结果为 `["Alice", "Bob", "Bob"]`。正向下标从 `0` 开始，负向下标从 `-1` 开始，`-1` 表示最后一个元素。

```js
var values = [10, 20, 30];
var index = 1;
return values[index + 1];
```

## 函数结果

函数返回值后可继续访问字段、下标，也可以继续调用返回的函数。

```js
var user = () -> {
    return {"name": "Alice", "tags": ["java", "dataql"]};
};
return [user().name, user().tags[0]];
```

直接写出的对象或列表需要先存入变量，再访问其字段或下标。

## 缺失值与越界

对象中不存在的字段返回 `null`，继续访问该空值的字段仍返回 `null`。列表越界由 `INDEX_OVERFLOW` 控制：

| 配置 | 行为 |
| --- | --- |
| `near`（默认） | 正向越界取最后一项，负向越界取第一项 |
| `null` | 返回 `null` |
| `throw` | 抛出执行异常 |

```js
hint INDEX_OVERFLOW = 'null';
var values = [10, 20];
return values[100];
```

空列表在 `near` 或 `null` 模式下返回 `null`，在 `throw` 模式下抛出异常。

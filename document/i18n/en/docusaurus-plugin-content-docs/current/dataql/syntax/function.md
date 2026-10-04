---
id: function
title: 3.2 Functions
description: DataQL 函数的语法与用法。
---

函数使用 `(参数列表) -> { 语句 }` 定义，可存入变量、传给其他函数或作为结果返回。导入的 UDF 与代码片段也使用相同的调用方式。

## 定义与调用

```js
var label = (enabled) -> {
    return enabled ? 'enabled' : 'disabled';
};
return label(true);
```

参数按位置传入，缺少的参数为 `null`，多出的参数不会绑定到具名参数。需要限制参数时可以使用 `assert`。

```js
var multiply = (value, factor) -> {
    assert value != null && factor != null;
    return value * factor;
};
return multiply(3, 2);
```

## 匿名函数

函数可以直接作为参数传入，例如筛选列表：

```js
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
var people = [{"name": "Alice", "age": 18}, {"name": "Bob", "age": 28}];
return collect.filter(people, (person) -> {
    return person.age > 20;
});
```

结果为 `[{"name":"Bob","age":28}]`。

## 外层变量与函数返回值

函数可以访问外层变量，也可以返回函数。

```js
var createPrefix = (prefix) -> {
    return (value) -> {
        return prefix + value;
    };
};
return createPrefix('Hello, ')('Alice');
```

结果为 `"Hello, Alice"`。

## 递归

函数可以通过名称调用自身。递归需要结束条件，大集合处理宜优先使用集合函数和结果转换。

```js
var sum = (n) -> {
    if (n <= 0) {
        return 0;
    }
    return n + sum(n - 1);
};
return sum(3);
```

结果为 `6`。

Import libraries and script files with [import](imports.md). See [Built-in libraries](../funx/index.md) for the function reference.

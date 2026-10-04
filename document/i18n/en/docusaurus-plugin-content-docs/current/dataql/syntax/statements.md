---
id: statements
title: 3.1 Control flow
description: DataQL 语句的语法与用法。
---

脚本按语句顺序执行，使用条件分支选择路径，用断言检查输入，通过 `return`、`throw` 或 `exit` 结束执行。

## var 与 run

`var name = value` 定义或重新绑定变量；`run value` 执行表达式并忽略返回值，适合调用产生副作用的函数。

```js
var total = 10 + 20;
var total = total + 5;
run total;
return total;
```

## return、throw 与 exit

| 语句 | 行为 |
| --- | --- |
| `return value` | 返回当前函数的结果；顶层使用时结束查询 |
| `throw value` | 抛出异常，停止查询 |
| `exit value` | 直接结束整个查询，包括外层函数 |

三者均可在值前指定整数状态码，例如 `return 200, value`，未指定时为 `0`。状态码属于查询结果，不直接表示 HTTP 状态。

```js
var check = (value) -> {
    if (value < 0) {
        throw 400, "value must be non-negative";
    }
    return value * 2;
};
return check(3);
```

```js
var stop = () -> {
    exit 10, "stopped";
};
run stop();
return "unreachable";
```

## if 与 else

分支按顺序判断，可使用多个 `else if` 和一个 `else`。

```js
var score = 85;
if (score >= 90) {
    return "A";
} else if (score >= 60) {
    return "B";
} else {
    return "C";
}
```

建议条件表达式返回布尔值，并完整书写花括号。语言未提供 `for`、`while` 和 `switch` 语句；集合遍历可使用[结果转换](./transform.md)和[函数](./function.md)。

## assert

`assert expression` 要求表达式结果为布尔值 `true`。结果为 `false` 或非布尔类型时抛出异常，停止查询。

```js
var quantity = 2;
assert quantity > 0;
return quantity * 10;
```

See [Variables and assignment](setter.md) and [Script structure](../tutorial/structure.md).

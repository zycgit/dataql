---
id: index
slug: /dataql/hints
title: 5. 执行选项
---

Hint 为脚本设置执行选项，例如列表越界行为、数值精度和舍入方式。使用 `hint 名称 = 值` 配置，名称区分大小写，值为字符串、数字、布尔值或 `null`。

## 设置选项

Hint 放在脚本或代码块开头：

```js
hint INDEX_OVERFLOW = 'null';
var values = [10, 20];
return values[5];
```

结果为 `null`。`hint NAME;` 仅声明，保留外部配置；`hint NAME = null;` 删除当前作用域的配置，恢复该选项的默认行为。

## 作用范围

代码块继承进入时的 Hint，块内修改在离开时恢复。函数在调用时继承当前执行环境，并应用函数体开头的 Hint；返回或异常退出后恢复调用方选项。

```js
hint INDEX_OVERFLOW = 'near';
var values = [10, 20];
var optional = () -> {
    hint INDEX_OVERFLOW = 'null';
    return values[5];
};
return [optional(), values[5]];
```

结果为 `[null, 20]`。函数内使用 `null`，返回后仍使用外层的 `near`。

## 选项参考

- [引擎 Hint](hint_core.md)：索引、数值计算和片段类型。
- [SQL Hint](hint_sql.md)：SQL 模块的数据源、执行、返回结果和分页选项。

应用侧默认值设置见[核心接口：Hint](../../dataway/dataql-engine/core.md#hint)。

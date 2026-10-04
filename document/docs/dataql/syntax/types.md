---
id: types
title: 2.2 类型与字面量
description: DataQL 类型系统的语法与用法。
---

DataQL 无需声明变量类型，同一变量可以重新绑定不同类型的值。

## 数据类型

| 类型 | 表示方式 | 说明 |
| --- | --- | --- |
| 布尔 | `true`、`false` | 条件结果 |
| 数值 | `123`、`-1.5`、`1e2` | 整数、小数，详见[数值](./numbers.md) |
| 字符串 | `'text'`、`"text"` | Unicode 文本 |
| 空值 | `null` | 关键字只有小写形式；`NULL` 是普通标识符 |
| 列表 | `[1, 2]`、`[]` | 有序元素，可嵌套 |
| 对象 | `{"name": "Alice"}`、`{}` | 字符串键与值组成的数据 |
| 函数 | `(x) -> { return x; }` | 脚本函数、导入的 UDF 和代码片段 |
| 二进制 | 由函数或宿主返回 | 没有专用字面量，可由函数返回 |

对象键使用引号，列表和对象的元素可以是不同类型。数组形式的数字集合与二进制对象是两种数据。

```js
return {
    "enabled": true,
    "amount": 12.5,
    "name": "Alice",
    "tags": ["java", "dataql"],
    "extra": null
};
```

## 类型转换

数值运算会根据参与计算的类型和 Hint 设置选择计算宽度。字符串拼接使用 `+`；需要明确转换时，使用[转换函数](../funx/convert.md)。DataQL 保留自己的数值和类型转换规则，运算符优先级见[表达式](./expression.md)。

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return convert.textToByte('Hello');
```

二进制可作为查询结果返回。Dataway 中的 HTTP 输出方式见[结果响应](../../dataway/capabilities/development/response.md)。

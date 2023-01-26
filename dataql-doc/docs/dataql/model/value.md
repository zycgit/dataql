---
id: value
sidebar_position: 2
title: b.ValueModel
description: DataQL 数据模型、ValueModel模型
---
# ValueModel

用于表示 `String`、`Number`、`Boolean`、`Null` 四种基本类型数据。

| 满足条件                  | 表示的类型     | 获取对应值         |
|-----------------------|-----------|---------------|
| `isNull() == true`    | `Null`    | `asOri()`     |
| `isNumber() == true`  | `Number`  | `asNumber()`  |
| `isBoolean() == true` | `Boolean` | `asBoolean()` |
| `isString() == true`  | `String`  | `asString()`  |

对于 `Number` 类型数据还可以进一步使用下列方法，来获取更加精确的类型值。

| 满足条件                     | 表示的类型        | 获取对应值            |
|--------------------------|--------------|------------------|
| `isNull() == true`       | `Null`       | `asOri()`        |
| `isByte() == true`       | `byte`       | `asByte()`       |
| `isShort() == true`      | `short`      | `asShort()`      |
| `isInt() == true`        | `int`        | `asInt()`        |
| `isLong() == true`       | `long`       | `asLong()`       |
| `isBigInteger() == true` | `BigInteger` | `asBigInteger()` |
| `isFloat() == true`      | `float`      | `asFloat()`      |
| `isDouble() == true`     | `double`     | `asDouble()`     |
| `isBigDecimal() == true` | `BigDecimal` | `asBigDecimal()` |

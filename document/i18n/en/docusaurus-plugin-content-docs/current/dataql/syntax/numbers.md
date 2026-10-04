---
id: numbers
title: 2.3 Numbers and precision
description: DataQL 数值的语法与用法。
---

DataQL 支持整数、小数及科学计数法。负号可用于十进制、二进制、八进制和十六进制数值。

## 数值写法

```js
return [123, -12, 0b1010, 0o12, 0x0A, -0x0A, 1.5, .5, 1e0, 10e2, 1.5e-2];
```

`0b`、`0o`、`0x` 和指数标记 `e` 均可使用大写。小数点后需要数字，例如使用 `1.0`。

## 数值宽度

| 类型 | 位数 | 范围或含义 |
| --- | --- | --- |
| `byte` | 8 | -128 ～ 127 |
| `short` | 16 | -32768 ～ 32767 |
| `int` | 32 | -2147483648 ～ 2147483647 |
| `long` | 64 | -9223372036854775808 ～ 9223372036854775807 |
| `float` | 32 | 单精度浮点数 |
| `double` | 64 | 双精度浮点数 |
| `BigInteger` | 可变 | 任意精度整数 |
| `BigDecimal` | 可变 | 任意精度十进制数 |

字面量按数值范围选择存储类型，计算时可通过 Hint 指定最小宽度；返回值的 Java 类型不应仅由脚本中的数字写法推断。

## 计算选项

未配置宽度时，整数最小为 `byte`，小数最小为 `float`，较大值和较宽操作数会使用更宽类型。需要十进制计算时可使用 `big`。

```js
hint MIN_INTEGER_WIDTH = 'long';
hint MIN_DECIMAL_WIDTH = 'big';
hint MAX_DECIMAL_DIGITS = 2;
hint NUMBER_ROUNDING = 'HALF_UP';
return 1.0 / 3.0;
```

结果为 `0.33`。`MAX_DECIMAL_DIGITS` 控制计算结果的小数位数，默认 `20`；`NUMBER_ROUNDING` 默认 `HALF_UP`。

| 舍入方式 | 含义 |
| --- | --- |
| `UP` | 向远离零的方向舍入 |
| `DOWN` | 向零舍入 |
| `CEILING` | 向正无穷舍入 |
| `FLOOR` | 向负无穷舍入 |
| `HALF_UP` | 取最近值，等距时远离零 |
| `HALF_DOWN` | 取最近值，等距时靠近零 |
| `HALF_EVEN` | 取最近值，等距时保留偶数 |
| `UNNECESSARY` | 要求结果精确，需要舍入时抛错 |

完整配置见[核心 Hint](../hints/hint_core.md)。

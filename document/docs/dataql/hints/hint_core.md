---
id: hint_core
title: 5.1 引擎 Hint
---

以下选项用于 DataQL 执行过程。设置方法和作用域见 [Hint 参考手册](index.md)。

## INDEX_OVERFLOW {#INDEX_OVERFLOW}

控制脚本列表索引越界，默认 `near`。索引从 0 开始，负索引从尾部计算；长度为 n 时，`-n` 仍是合法的首元素位置。

| 值 | 越界行为 |
| --- | --- |
| `near` | 取最近的边界元素；空列表返回 null |
| `null` | 返回 null |
| `throw` | 抛出索引越界异常，由查询运行异常向调用方传播 |

```javascript
hint INDEX_OVERFLOW = 'near';
var values = [10,20,30];
return [values[-3], values[-1], values[-9], values[9]];
// [10,30,10,30]
```

本选项作用于脚本取元素，不改变 Java ListModel 的索引规则。

## MIN_INTEGER_WIDTH {#MIN_INTEGER_WIDTH}

整数的最小类型宽度，可选 `byte`、`short`、`int`、`long`、`big`，其中 big 表示 BigInteger。未设置时不额外提升宽度，最小为 byte；较大字面量按实际值选择更宽类型。

```javascript
hint MIN_INTEGER_WIDTH = 'big';
return 9223372036854775807 + 1;
// 9223372036854775808
```

宽度选项用于数字字面量和数值运算，不会缩窄已有的较宽类型。需要避免固定宽度整数溢出时使用 big。

## MIN_DECIMAL_WIDTH {#MIN_DECIMAL_WIDTH}

小数的最小类型宽度，可选 `float`、`double`、`big`，其中 big 表示 BigDecimal。未设置时保留解析器或操作数选定的类型，最小为 float。

```javascript
hint MIN_DECIMAL_WIDTH = 'big';
return 0.1 + 0.2;
// 0.3
```

使用 double 可提高浮点精度，使用 big 可进行十进制计算。该选项不把整数直接改成小数。

## MAX_DECIMAL_DIGITS {#MAX_DECIMAL_DIGITS}

数值运算结果最多保留的小数位数，默认 20。配合 NUMBER_ROUNDING 舍入；它不修改输入参数，也不规定输出 JSON 的文本宽度。应配置非负整数，精确小数计算同时使用 `MIN_DECIMAL_WIDTH = 'big'`。

```javascript
hint MIN_DECIMAL_WIDTH = 'big';
hint MAX_DECIMAL_DIGITS = 2;
hint NUMBER_ROUNDING = 'HALF_UP';
return 1 / 6;
// 0.17
```

Float、Double 仍受二进制浮点精度限制，增加小数位数不会增加其有效精度。

## NUMBER_ROUNDING {#NUMBER_ROUNDING}

数值舍入模式，默认 HALF_UP，名称忽略大小写。

| 值 | 规则 | 2.5 舍入为整数 | -2.5 舍入为整数 |
| --- | --- | --- | --- |
| `UP` | 远离零 | 3 | -3 |
| `DOWN` | 接近零 | 2 | -2 |
| `CEILING` | 向正无穷 | 3 | -2 |
| `FLOOR` | 向负无穷 | 2 | -3 |
| `HALF_UP` | 取最近值，正好一半时远离零 | 3 | -3 |
| `HALF_DOWN` | 取最近值，正好一半时接近零 | 2 | -2 |
| `HALF_EVEN` | 取最近值，正好一半时取偶数 | 2 | -2 |
| `UNNECESSARY` | 要求无需舍入，否则抛出算术异常 | 异常 | 异常 |

## FRAGMENT_TYPE {#FRAGMENT_TYPE}

调用片段时由引擎传入的注册名，例如 `@@selectSql` 对应 `selectSql`。供 FragmentProcess 识别当前入口，脚本无需设置。片段调用中的值由触发点决定，使用方式见[片段扩展](../../dataway/engine/fragments.md)。

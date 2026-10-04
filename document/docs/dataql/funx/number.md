---
id: number
title: 7.3 数值函数
---

:::info 依赖模块
本页函数由 `dataql-engine` 提供。
:::

`NumberUdfSource` 提供整数范围约束，适合把页大小、数量等限制在允许范围内。

## inRange

`number.inRange(value, min, max)` 将整数限制在闭区间 `[min, max]` 内，返回整数。

| 参数 | 含义 |
| --- | --- |
| `value` | 待约束的整数 |
| `min` | 允许的最小值，包含该值 |
| `max` | 允许的最大值，包含该值 |

小于 `min` 返回 `min`，大于 `max` 返回 `max`，其余返回原值。调用时保证 `min <= max`；上下界相等时返回该边界值。

```javascript
import 'net.hasor.dataql.host.function.basic.NumberUdfSource' as number;
return {
    'below': number.inRange(-1, 0, 100),
    'inside': number.inRange(50, 0, 100),
    'above': number.inRange(120, 0, 100),
    'lowerBound': number.inRange(0, 0, 100),
    'upperBound': number.inRange(100, 0, 100),
    'singleValue': number.inRange(50, 10, 10)
};
// {"below":0,"inside":50,"above":100,"lowerBound":0,"upperBound":100,"singleValue":10}
```

三个参数按 32 位有符号整数处理，应传入该范围内的整数。此函数不负责小数舍入；数值运算的精度和舍入设置见[引擎 Hint](../hints/hint_core.md)。

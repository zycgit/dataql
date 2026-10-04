---
id: number
title: 7.3 Number functions
---

:::info Module dependency
These functions are provided by `dataql-engine`.
:::

`NumberUdfSource` constrains integers to a range, for example to keep a page size or quantity within allowed limits.

## inRange

`number.inRange(value, min, max)` clamps an integer to the inclusive range `[min, max]` and returns an integer.

| Argument | Meaning |
| --- | --- |
| `value` | The integer to constrain |
| `min` | The minimum allowed value, inclusive |
| `max` | The maximum allowed value, inclusive |

Values below `min` return `min`; values above `max` return `max`; all other values remain unchanged. Ensure `min <= max`. When both bounds are equal, the function returns that boundary value.

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

All three arguments are handled as signed 32-bit integers; provide integers within that range. This function does not round decimal values. For numeric precision and rounding settings, see [Engine hints](../hints/hint_core.md).

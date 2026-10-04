---
id: datetime
title: 7.4 时间与日期函数
---

:::info 依赖模块
本页函数由 `dataql-engine` 提供。
:::

导入 `net.hasor.dataql.host.function.basic.DateTimeUdfSource` 后，通过别名调用函数。除 `now()` 和 `parser()` 外，函数的 `time` 入参都是毫秒时间戳；`now()` 和 `parser()` 也返回毫秒时间戳。

日期字段、格式化和未指定时区的日期解析使用 JVM 默认时区；周序号、月份名称等还受默认地区影响。本页固定结果以默认时区 `Asia/Shanghai`、地区 `en-US` 为前提。`1767337445000` 对应当地时间 `2026-01-02 15:04:05`。

## now

`time.now()` 不接收参数，返回执行时的当前毫秒时间戳。保存一次结果，可以让后续字段取自同一时刻。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
var current = time.now();
return [current, time.format(current, 'yyyy-MM-dd HH:mm:ss')];
// 例如 [1767337445000, "2026-01-02 15:04:05"]，实际值随执行时间变化。
```

## year

`time.year(time)` 返回年份。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.year(1767337445000);
// 2026
```

## month

`time.month(time)` 返回月份，范围 1～12。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.month(1767337445000);
// 1
```

## day、dayOfMonth

`time.day(time)` 和 `time.dayOfMonth(time)` 都返回当月第几天，从 1 开始。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return [time.day(1767337445000), time.dayOfMonth(1767337445000)];
// [2, 2]
```

## hour

`time.hour(time)` 返回 12 小时制的 0～11，不区分上午、下午。需要 24 小时制时使用 `format(time, 'HH')`，返回字符串。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return [time.hour(1767337445000), time.format(1767337445000, 'HH')];
// [3, "15"]
```

## minute

`time.minute(time)` 返回分钟，范围 0～59。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.minute(1767337445000);
// 4
```

## second

`time.second(time)` 返回秒，范围 0～59。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.second(1767337445000);
// 5
```

## dayOfYear

`time.dayOfYear(time)` 返回当年第几天，从 1 开始；闰年最多为 366。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.dayOfYear(time.parser('2024-03-01', 'yyyy-MM-dd'));
// 61
```

## dayOfWeek

`time.dayOfWeek(time)` 返回星期编号：周日为 1，周一为 2，依次至周六为 7。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.dayOfWeek(1767337445000);
// 6，即星期五。
```

## weekOfMonth

`time.weekOfMonth(time)` 返回当月周序号。默认地区决定一周从哪天开始、首周至少要有几天；月初不足一周时，某些地区可能返回 0。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.weekOfMonth(time.parser('2021-01-01', 'yyyy-MM-dd'));
// 默认地区 en-US 时为 1；de-DE 时为 0。
```

## format

`time.format(time, pattern)` 将毫秒时间戳格式化为字符串。`pattern` 使用 `SimpleDateFormat` 规则：`MM` 是月份，`mm` 是分钟，`HH` 为 24 小时制，`hh` 为 12 小时制。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return [
    time.format(1767337445000, 'yyyy-MM-dd HH:mm:ss'),
    time.format(1767337445000, 'yyyy/MM/dd hh:mm:ss a')
];
// ["2026-01-02 15:04:05", "2026/01/02 03:04:05 PM"]
```

## parser

`time.parser(text, pattern)` 按指定格式解析日期字符串，返回毫秒时间戳。`pattern` 的含义与 `format` 相同。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.parser('2026-01-02 15:04:05', 'yyyy-MM-dd HH:mm:ss');
// 1767337445000
```

解析使用宽松日期规则，会调整超出范围的日期；不能作为严格的输入校验器。无法解析的内容会抛出异常。

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
var instant = time.parser('2026-02-30', 'yyyy-MM-dd');
return time.format(instant, 'yyyy-MM-dd');
// "2026-03-02"
```

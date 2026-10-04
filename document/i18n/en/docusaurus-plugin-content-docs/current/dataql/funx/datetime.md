---
id: datetime
title: 7.4 Date and time functions
---

:::info Module dependency
These functions are provided by `dataql-engine`.
:::

Import `net.hasor.dataql.host.function.basic.DateTimeUdfSource` and call its functions through an alias. Except for `now()` and `parser()`, the `time` argument is a timestamp in milliseconds. `now()` and `parser()` also return millisecond timestamps.

Date fields, formatting, and parsing of dates without an explicit time zone use the JVM default time zone. Week numbers and names such as month names also depend on the default locale. Fixed results on this page assume the `Asia/Shanghai` time zone and `en-US` locale. The timestamp `1767337445000` represents local time `2026-01-02 15:04:05`.

## now

`time.now()` takes no arguments and returns the current timestamp in milliseconds. Save the result once to derive multiple fields from the same instant.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
var current = time.now();
return [current, time.format(current, 'yyyy-MM-dd HH:mm:ss')];
// For example: [1767337445000, "2026-01-02 15:04:05"]. Values change with execution time.
```

## year

`time.year(time)` returns the year.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.year(1767337445000);
// 2026
```

## month

`time.month(time)` returns the month, from 1 to 12.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.month(1767337445000);
// 1
```

## day、dayOfMonth

`time.day(time)` and `time.dayOfMonth(time)` both return the day of the month, starting at 1.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return [time.day(1767337445000), time.dayOfMonth(1767337445000)];
// [2, 2]
```

## hour

`time.hour(time)` returns the hour on a 12-hour clock, from 0 to 11, without distinguishing AM from PM. For a 24-hour value, use `format(time, 'HH')`, which returns a string.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return [time.hour(1767337445000), time.format(1767337445000, 'HH')];
// [3, "15"]
```

## minute

`time.minute(time)` returns the minute, from 0 to 59.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.minute(1767337445000);
// 4
```

## second

`time.second(time)` returns the second, from 0 to 59.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.second(1767337445000);
// 5
```

## dayOfYear

`time.dayOfYear(time)` returns the day of the year, starting at 1 and reaching 366 in a leap year.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.dayOfYear(time.parser('2024-03-01', 'yyyy-MM-dd'));
// 61
```

## dayOfWeek

`time.dayOfWeek(time)` returns a weekday number: Sunday is 1, Monday is 2, and so on through Saturday, which is 7.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.dayOfWeek(1767337445000);
// 6, meaning Friday.
```

## weekOfMonth

`time.weekOfMonth(time)` returns the week number within the month. The default locale determines the first day of a week and the minimum number of days in the first week. Some locales can return 0 for an incomplete week at the start of a month.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.weekOfMonth(time.parser('2021-01-01', 'yyyy-MM-dd'));
// 1 with the en-US default locale; 0 with de-DE.
```

## format

`time.format(time, pattern)` formats a millisecond timestamp as a string. The `pattern` follows `SimpleDateFormat` rules: `MM` is the month, `mm` the minute, `HH` a 24-hour clock, and `hh` a 12-hour clock.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return [
    time.format(1767337445000, 'yyyy-MM-dd HH:mm:ss'),
    time.format(1767337445000, 'yyyy/MM/dd hh:mm:ss a')
];
// ["2026-01-02 15:04:05", "2026/01/02 03:04:05 PM"]
```

## parser

`time.parser(text, pattern)` parses a date string with the given pattern and returns a millisecond timestamp. The `pattern` uses the same rules as `format`.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
return time.parser('2026-01-02 15:04:05', 'yyyy-MM-dd HH:mm:ss');
// 1767337445000
```

Parsing is lenient and normalizes out-of-range dates, so it is not a strict input validator. Unparseable input throws an exception.

```javascript
import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;
var instant = time.parser('2026-02-30', 'yyyy-MM-dd');
return time.format(instant, 'yyyy-MM-dd');
// "2026-03-02"
```

---
id: state
title: 7.8 状态函数
---

:::info 依赖模块
本页函数由 `dataql-engine` 提供。
:::

导入 `net.hasor.dataql.host.function.basic.StateUdfSource` 后，可以创建计数器或生成随机标识符。

计数器的初值 `initValue` 是整数，计数使用 64 位整数。`decNumber` 实际递增，`incNumber` 实际递减，名称与通常的 inc/dec 缩写含义相反。两者返回可调用的函数；保存返回值后调用，才会推进同一个计数器。

## decNumber

`state.decNumber(initValue)` 创建递增计数器。返回的函数不需要参数；第一次调用返回 `initValue + 1`，后续每次加 1。每次创建的计数器相互独立。

```javascript
import 'net.hasor.dataql.host.function.basic.StateUdfSource' as state;
var next = state.decNumber(10);
var other = state.decNumber(10);
return [next(), next(), other(), next()];
// [11, 12, 11, 13]
```

## incNumber

`state.incNumber(initValue)` 创建递减计数器。返回的函数不需要参数；第一次调用返回 `initValue - 1`，后续每次减 1，可以减到负数。

```javascript
import 'net.hasor.dataql.host.function.basic.StateUdfSource' as state;
var previous = state.incNumber(2);
return [previous(), previous(), previous()];
// [1, 0, -1]
```

## uuid

`state.uuid()` 不接收参数，返回随机 UUID 字符串，长度为 36，包含 4 个连字符。结果随调用变化，下面只展示格式。

```javascript
import 'net.hasor.dataql.host.function.basic.StateUdfSource' as state;
return state.uuid();
// 可能的结果："bc4b0433-0427-4d0f-9f0b-5e9b7a0a281e"
```

## uuidToShort

`state.uuidToShort()` 不接收参数，生成新的随机 UUID，返回移除连字符后的 32 位十六进制字符串。它不会读取或缩短上一次 `uuid()` 的结果。

```javascript
import 'net.hasor.dataql.host.function.basic.StateUdfSource' as state;
return state.uuidToShort();
// 可能的结果："9023d8de0ce54b8382e5ec1748e6c1a9"
```

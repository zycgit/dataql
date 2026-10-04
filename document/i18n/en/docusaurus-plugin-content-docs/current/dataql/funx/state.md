---
id: state
title: 7.8 State functions
---

:::info Module dependency
These functions are provided by `dataql-engine`.
:::

Import `net.hasor.dataql.host.function.basic.StateUdfSource` to create counters or generate random identifiers.

The counter's `initValue` is an integer, and counting uses 64-bit integers. `decNumber` actually increments and `incNumber` actually decrements, the reverse of the usual inc/dec abbreviations. Both return a callable function; save it and invoke it to advance the same counter.

## decNumber

`state.decNumber(initValue)` creates an incrementing counter. The returned function takes no arguments: its first call returns `initValue + 1`, and each subsequent call adds 1. Each newly created counter has independent state.

```javascript
import 'net.hasor.dataql.host.function.basic.StateUdfSource' as state;
var next = state.decNumber(10);
var other = state.decNumber(10);
return [next(), next(), other(), next()];
// [11, 12, 11, 13]
```

## incNumber

`state.incNumber(initValue)` creates a decrementing counter. The returned function takes no arguments: its first call returns `initValue - 1`, and each subsequent call subtracts 1. Values can become negative.

```javascript
import 'net.hasor.dataql.host.function.basic.StateUdfSource' as state;
var previous = state.incNumber(2);
return [previous(), previous(), previous()];
// [1, 0, -1]
```

## uuid

`state.uuid()` takes no arguments and returns a random UUID string of 36 characters, including 4 hyphens. Its value changes on each call; the example illustrates the format only.

```javascript
import 'net.hasor.dataql.host.function.basic.StateUdfSource' as state;
return state.uuid();
// One possible result: "bc4b0433-0427-4d0f-9f0b-5e9b7a0a281e"
```

## uuidToShort

`state.uuidToShort()` takes no arguments, generates a new random UUID, and returns a 32-character hexadecimal string with the hyphens removed. It does not read or shorten the result of a previous `uuid()` call.

```javascript
import 'net.hasor.dataql.host.function.basic.StateUdfSource' as state;
return state.uuidToShort();
// One possible result: "9023d8de0ce54b8382e5ec1748e6c1a9"
```

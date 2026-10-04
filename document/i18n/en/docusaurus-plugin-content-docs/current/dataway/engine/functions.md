---
title: "9.1 Custom functions"
---

A custom `Udf` exposes application capabilities to DataQL, such as lookups, service calls and data conversion. Scripts call it by its registered name.

## Implement the function

Implement `call(Hints, UdfParams)` and read script arguments through `allParams()`. This function accepts one string and returns a greeting.
```java title="GreetingUdf.java"
package com.example.dataway;

import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.UdfParams;

public class GreetingUdf implements Udf {
    @Override
    public Object call(Hints hints, UdfParams params) {
        Object[] arguments = params.allParams();
        if (arguments.length != 1 || !(arguments[0] instanceof String name)) {
            throw new IllegalArgumentException("greeting requires one string argument");
        }
        return "Hello " + name;
    }
}
```


## Register the function

`config` is the application’s `DatawayConfig`; apply these settings before creating Dataway.
```java
import com.example.dataway.GreetingUdf;

config.function("greeting", new GreetingUdf());
```


## Call from a script


```javascript
return greeting('Dataway');
```
Returns `"Hello Dataway"`; the selected result handler produces the final response.

## Usage notes

`Hints` exposes execution options; `UdfParams` holds positional arguments. Return DataQL-supported values such as strings, numbers and collections. For binary values, see [Result responses](../capabilities/development/response.md#binary-response).

The registered instance can serve multiple queries. Keep request state out of instance fields. Use [function libraries](libraries.md) to group functions under a namespace.

## Read execution context {#execution-context}

Applications can pass objects to a UDF through Hints. Read them in `call` with `hints.getHint("app.context")`; see [Core APIs: Hints](../dataql-engine/core.md#hint) for configuration. UDFs receive read-only Hint entries, while contained objects retain their original references and may be shared.

Dataway uses `DATAWAY_REQUEST` and `DATAWAY_RESPONSE` for the current request and response, removing them after execution. Use distinct application Hint names. For script-level Header and Cookie operations, see [Web functions](../../dataql/funx/web.md).

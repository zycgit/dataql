---
title: "9.2 Function libraries"
---

A function library groups related functions under a namespace imported by scripts. Define functions as Java methods or register a `Map<String, Udf>`.

## Implement a library

Extend `AbstractUdfSource` and use `@UdfName` for script-visible names. This example converts text case.
```java title="TextFunctions.java"
package com.example.dataway;

import java.util.Locale;
import net.hasor.dataql.host.function.AbstractUdfSource;
import net.hasor.dataql.host.function.UdfName;

public class TextFunctions extends AbstractUdfSource {
    @UdfName("upper")
    public String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    @UdfName("lower")
    public String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
```


## Register the library

`config` is the application’s `DatawayConfig`; apply these settings before creating Dataway.
```java
import com.example.dataway.TextFunctions;

config.importSource("app.text", TextFunctions::new);
```
This stateless implementation has a no-argument constructor. `AbstractUdfSource` creates method targets by reflection by default. Use [Application imports](imports.md) to expose data such as application settings directly to scripts.

## Call from a script


```javascript
import 'app.text' as text;
return {
    'upper': text.upper('Dataway'),
    'lower': text.lower('Dataway')
};
```
Returns `{"upper":"DATAWAY","lower":"dataway"}`.

## Register a function Map


```java
import java.util.Map;
import com.example.dataway.GreetingUdf;

config.library("app.tools", Map.of("greeting", new GreetingUdf()));
```

```javascript
import 'app.tools' as tools;
return tools.greeting('Dataway');
```
See [Custom functions](functions.md) for `GreetingUdf`. `library` copies the Map during configuration; later Map changes do not alter registered functions. Method-based functions do not support overloading; use distinct names.

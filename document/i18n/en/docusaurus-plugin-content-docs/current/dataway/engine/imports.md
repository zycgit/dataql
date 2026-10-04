---
title: "9.3 Application imports"
---

`importSource` registers an application object under a script import name. Executing `import` resolves that object, allowing scripts to read configuration or lookup data and obtain registered functions or libraries.

A UDF defines callable behavior; `importSource` provides an object by name. They can be combined. This guide demonstrates data imports using a plain Java object.

## Prepare an application object

This object exposes the application name and default page size through getters. Its `pageOffset` method calculates the starting offset for a page:

```java title="ApplicationSettings.java"
package com.example.dataway;

public class ApplicationSettings {
    private final String name;
    private final int defaultPageSize;

    public ApplicationSettings(String name, int defaultPageSize) {
        this.name = name;
        this.defaultPageSize = defaultPageSize;
    }

    public String getName() {
        return this.name;
    }

    public int getDefaultPageSize() {
        return this.defaultPageSize;
    }

    public int pageOffset(int pageNumber) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        return (pageNumber - 1) * this.defaultPageSize;
    }
}
```

It is a plain POJO and implements no DataQL interface. An existing application configuration object can serve the same purpose.

## Register the object

Register it as `app.settings` in the application's `DatawayConfig`:

```java title="Register application settings"
import com.example.dataway.ApplicationSettings;

ApplicationSettings settings = new ApplicationSettings("orders", 20);
config.importSource("app.settings", () -> settings);
```

`app.settings` is the lookup name; `() -> settings` supplies the existing instance. An application container may also create the object and pass it in. The application chooses the registration name.

## Use it in a script

```javascript
import 'app.settings' as settings;
return {
    'application': settings.name,
    'pageSize': settings.defaultPageSize
};
```

`settings` is a local script name. Its properties correspond to Java's `getName()` and `getDefaultPageSize()`. The script result is:

```json
{
    "application": "orders",
    "pageSize": 20
}
```

A Map can supply the same data without changing the script:

```java title="Provide settings as a Map"
import java.util.Map;

config.importSource("app.settings", () -> Map.of(
        "name", "orders",
        "defaultPageSize", 20));
```

## Call application object methods

Application objects can provide methods through UDF adapters. Plain property access such as `settings.name` reads data; importing an object does not automatically expose business methods such as `pageOffset` as callable functions.

Register the `pageOffset` method of the same `settings` instance in a function library:

```java title="Register an object method"
import java.util.Map;

config.library("app.paging", Map.of(
        "pageOffset", (hints, params) -> {
            Number pageNumber = (Number) params.allParams()[0];
            return settings.pageOffset(pageNumber.intValue());
        }));
```

Import the configuration object and method library to read properties and call methods:

```javascript
import 'app.settings' as settings;
import 'app.paging' as paging;
return {
    'application': settings.name,
    'offset': paging.pageOffset(3),
    'limit': settings.defaultPageSize
};
```

```json
{
    "application": "orders",
    "offset": 40,
    "limit": 20
}
```

The call uses the registered `settings` instance and its page size. The application class needs no DataQL interface. Multiple methods can also be organized into a library with [AbstractUdfSource](libraries.md).

## Using imported objects

| Object type | Script usage |
| --- | --- |
| Plain POJO or Map | Read properties, such as `settings.name` |
| `Udf` | Call a function, such as `lookup('u1')`; see [Custom functions](functions.md) |
| `UdfSource` | Call library functions, such as `text.upper('hello')`; see [Function libraries](libraries.md) |

## Lookup and lifecycle

Executing `import 'app.settings'` resolves the registered name, invokes the supplier and makes the object available to the script. Explicit imports take precedence over a custom [Finder](finder.md), which handles unresolved names.

`() -> settings` reuses one object; suppliers can also return new objects. The application manages concurrent access and resource cleanup. Registrations belong to application configuration; pass per-request data through API parameters.

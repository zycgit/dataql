---
title: "9.7 Engine and query configuration"
---

Use `DatawayConfig` to configure engine and query creation:

- `configureHost`: runs when Dataway is created to register imported objects, fragment processors and other engine settings.
- `configureQuery`: runs for each new query to set shared variables, Hints and compiler options.
- `attachment`: stores an application object for Java extensions to retrieve by type, such as the `ConnectionProvider` used for SQL execution.

## Supply application settings to queries

This example stores application settings with `attachment`, then uses `configureQuery` to expose the application name as the script variable `application`.
```java title="ApplicationInfo.java"
package com.example.dataway;

public class ApplicationInfo {
    private final String name;

    public ApplicationInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
```

```java title="ApplicationQueryCustomizer.java"
package com.example.dataway;

import java.util.function.Consumer;
import net.hasor.dataql.host.QueryBuilder;

public class ApplicationQueryCustomizer implements Consumer<QueryBuilder> {
    @Override
    public void accept(QueryBuilder builder) {
        ApplicationInfo application = builder.getHostContext().getAttachment(ApplicationInfo.class);
        builder.addShareVar("application", application::getName);
    }
}
```

## Register the extensions

`config` is the application’s `DatawayConfig`; apply these settings before creating Dataway.
```java
import com.example.dataway.ApplicationInfo;
import com.example.dataway.ApplicationQueryCustomizer;
import com.example.dataway.GreetingUdf;

config.attachment(ApplicationInfo.class, new ApplicationInfo("orders"))
        .configureHost(host -> host.addImport("app.greeting", GreetingUdf::new))
        .configureQuery(new ApplicationQueryCustomizer());
```

## Call from a script

```javascript
import 'app.greeting' as greeting;
return greeting(application);
```
Returns `"Hello orders"`. See [Custom functions](functions.md) for `GreetingUdf`.

## Configuration timing

- `configureHost`: runs in registration order whenever Dataway is created, registering imported objects, fragment processors and other engine settings.
- `configureQuery`: runs in registration order whenever a query builder is created; configure variables, Hints and compiler options here.
- `attachment`: retrieve application objects with `HostContext.getAttachment(type)`. The first registration for a type is retained. The application manages object creation and resource cleanup.

Convenience methods such as `function`, `library` and `importSource` already wrap these registrations. Use callbacks for additional native configuration. SQL obtains connections through `attachment(ConnectionProvider.class, provider)`; see [Data source integration](../capabilities/datasources.md).

---
title: "9.5 Finder"
---

`Finder` resolves imported objects and fragment processors, and provides resource and class loaders. The default `DatawayFinder` loads classpath resources and constructs objects by reflection. Extend it to connect application lookup rules.

## Implement a Finder

This implementation checks an application object Map before falling back to default behavior. Type lookup uses the class name as its Map key.
```java title="ApplicationFinder.java"
package com.example.dataway;

import java.util.Map;
import net.hasor.dataway.service.DatawayFinder;

public class ApplicationFinder extends DatawayFinder {
    private final Map<String, ?> beans;

    public ApplicationFinder(Map<String, ?> beans) {
        this.beans = beans;
    }

    @Override
    public Object findBean(String name) throws ClassNotFoundException {
        Object bean = this.beans.get(name);
        if (bean != null) {
            return bean;
        }
        return super.findBean(name);
    }

    @Override
    public Object findBean(Class<?> type) {
        Object bean = this.beans.get(type.getName());
        if (bean != null) {
            return type.cast(bean);
        }
        return super.findBean(type);
    }
}
```


## Configure lookup and loaders


```java
import java.util.Map;
import com.example.dataway.ApplicationFinder;
import com.example.dataway.GreetingUdf;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;

GreetingUdf greeting = new GreetingUdf();
ApplicationFinder finder = new ApplicationFinder(Map.of(
        "app.greeting", greeting,
        GreetingUdf.class.getName(), greeting));

config.finder(finder)
        .resourceLoader(ClassPathResourceLoader.INSTANCE)
        .classLoader(ApplicationFinder.class.getClassLoader());
```


## Call from a script


```javascript
import 'app.greeting' as greeting;
return greeting('Dataway');
```
Returns `"Hello Dataway"`. See [Custom functions](functions.md) for `GreetingUdf`.

## Lookup order and loaders

Explicit `importSource`, `library` and `fragment` registrations take precedence; missing registrations delegate to Finder. The default Finder rejects unknown fragments.

`resourceLoader` handles resources such as scripts; `classLoader` loads Java types and SPI implementations. Pass a custom `ResourceLoader` to `resourceLoader(...)`. Both setters apply to `DatawayFinder` and subclasses. Direct `Finder` implementations supply their own loaders.

Replacing Finder also switches loaders; DatawayConfig getters read the current Finder. Set Finder before configuring its loaders.

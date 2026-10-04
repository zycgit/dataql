---
title: "9.5 查找器"
---

`Finder` 统一查找导入对象、片段执行器，并提供资源加载器和类加载器。默认 `DatawayFinder` 使用类路径资源，并根据类名反射创建对象。接入应用容器时，可扩展该实现。

## 实现查找器

下例先查找应用提供的对象 Map，未找到时使用默认行为。按类型查找使用类全名作为 Map 的键。
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


## 注册配置


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


## 脚本调用


```javascript
import 'app.greeting' as greeting;
return greeting('Dataway');
```
返回 `"Hello Dataway"`，`GreetingUdf` 的实现见[自定义函数](functions.md)。

## 查找顺序与加载器

显式注册的 `importSource`、`library` 和 `fragment` 优先于 Finder；没有匹配注册时，才委托 Finder 查找。默认 Finder 遇到未注册片段会抛出异常。

`resourceLoader` 负责脚本等资源访问，`classLoader` 负责 Java 类型和 SPI 加载。可将自定义 `ResourceLoader` 传给 `resourceLoader(...)`。这两个配置方法用于 `DatawayFinder` 及其子类；直接实现 `Finder` 时，由实现自身提供加载器。

替换 Finder 会同时切换加载器，`DatawayConfig` 的相关 getter 读取当前 Finder。需要调整默认加载器时，先设置 Finder，再设置加载器。

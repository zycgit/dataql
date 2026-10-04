---
title: "9.2 函数库"
---

函数库将相关函数放入同一命名空间，脚本通过 `import` 导入后调用。可使用普通 Java 方法定义函数，也可直接注册 `Map<String, Udf>`。

## 实现函数库

继承 `AbstractUdfSource`，通过 `@UdfName` 指定脚本中的函数名。示例提供大小写转换。
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


## 注册配置

`config` 表示应用注册的 `DatawayConfig`，以下配置在创建 Dataway 前完成。
```java
import com.example.dataway.TextFunctions;

config.importSource("app.text", TextFunctions::new);
```
该实现无请求状态，使用无参构造方法创建。`AbstractUdfSource` 默认会反射创建方法调用对象。应用配置等数据可直接通过[应用对象导入](imports.md)提供给脚本。

## 脚本调用


```javascript
import 'app.text' as text;
return {
    'upper': text.upper('Dataway'),
    'lower': text.lower('Dataway')
};
```
返回 `{"upper":"DATAWAY","lower":"dataway"}`。

## 使用函数 Map


```java
import java.util.Map;
import com.example.dataway.GreetingUdf;

config.library("app.tools", Map.of("greeting", new GreetingUdf()));
```

```javascript
import 'app.tools' as tools;
return tools.greeting('Dataway');
```
`GreetingUdf` 见[自定义函数](functions.md)。`library` 在配置时复制函数 Map，随后修改原 Map 不会改变注册内容。方法式函数不支持重载，应使用不同函数名。

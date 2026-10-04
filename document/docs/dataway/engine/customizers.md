---
title: "9.7 引擎与查询配置"
---

通过 `DatawayConfig`，可以在创建引擎和查询时补充配置：

- `configureHost`：创建 Dataway 时执行，用于注册导入对象、片段执行器等引擎配置。
- `configureQuery`：每次创建查询时执行，用于设置共享变量、Hint 和编译选项。
- `attachment`：保存应用提供的对象，供 Java 扩展代码按类型获取，例如 SQL 执行所需的 `ConnectionProvider`。

## 向查询提供应用配置

下面通过 `attachment` 保存应用配置，再由 `configureQuery` 将应用名称添加为脚本变量 `application`。
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

## 注册配置

`config` 表示应用注册的 `DatawayConfig`，以下配置在创建 Dataway 前完成。
```java
import com.example.dataway.ApplicationInfo;
import com.example.dataway.ApplicationQueryCustomizer;
import com.example.dataway.GreetingUdf;

config.attachment(ApplicationInfo.class, new ApplicationInfo("orders"))
        .configureHost(host -> host.addImport("app.greeting", GreetingUdf::new))
        .configureQuery(new ApplicationQueryCustomizer());
```

## 脚本调用

```javascript
import 'app.greeting' as greeting;
return greeting(application);
```
返回 `"Hello orders"`。`GreetingUdf` 实现见[自定义函数](functions.md)。

## 配置时机

- `configureHost`：每次创建 Dataway 时按注册顺序执行，完成导入对象、片段执行器等注册。
- `configureQuery`：每次创建查询构建器时按注册顺序执行，可设置共享变量、Hint 和编译选项。
- `attachment`：通过 `HostContext.getAttachment(type)` 获取应用对象；同类型重复注册保留首次实例，对象的创建和资源释放由应用负责。

`function`、`library`、`importSource` 等便捷方法已封装相应注册。需要更多原生配置时再使用回调。SQL 使用 `attachment(ConnectionProvider.class, provider)` 提供数据库连接，见[数据源接入](../capabilities/datasources.md)。

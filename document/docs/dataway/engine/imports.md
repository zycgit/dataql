---
title: "9.3 应用对象导入"
---

`importSource` 将应用中的对象注册为脚本可导入的名称。脚本执行 `import` 时取得该对象，可读取配置、字典等数据，也可取得已注册的函数或函数库。

UDF 定义函数的调用行为；`importSource` 负责按名称提供对象。两者可以组合使用，本节用普通 Java 对象展示数据导入。

## 准备应用对象

下面的对象通过 getter 提供应用名称和默认分页大小，`pageOffset` 方法计算指定页的起始偏移量：

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

它是普通 POJO，无需实现 DataQL 接口。实际项目可直接使用已有的应用配置对象。

## 注册配置

在应用的 `DatawayConfig` 中，将对象注册为 `app.settings`：

```java title="注册应用配置对象"
import com.example.dataway.ApplicationSettings;

ApplicationSettings settings = new ApplicationSettings("orders", 20);
config.importSource("app.settings", () -> settings);
```

`app.settings` 是脚本查找名称，`() -> settings` 提供已有实例。对象也可由应用容器创建后传入，注册名称由应用自行约定。

## 脚本使用

```javascript
import 'app.settings' as settings;
return {
    'application': settings.name,
    'pageSize': settings.defaultPageSize
};
```

`settings` 是脚本中的局部名称，属性分别对应 Java 的 `getName()`、`getDefaultPageSize()`。脚本结果为：

```json
{
    "application": "orders",
    "pageSize": 20
}
```

也可直接注册 Map，脚本保持不变：

```java title="使用 Map 提供配置"
import java.util.Map;

config.importSource("app.settings", () -> Map.of(
        "name", "orders",
        "defaultPageSize", 20));
```

## 调用应用对象的方法

应用对象可以提供方法，通过 UDF 适配后供脚本调用。普通对象的 `settings.name` 用于读取属性，直接导入不会将 `pageOffset` 等业务方法转换为可调用函数。

将上面同一个 `settings` 实例的 `pageOffset` 方法注册到函数库：

```java title="注册对象方法"
import java.util.Map;

config.library("app.paging", Map.of(
        "pageOffset", (hints, params) -> {
            Number pageNumber = (Number) params.allParams()[0];
            return settings.pageOffset(pageNumber.intValue());
        }));
```

脚本导入配置对象和方法库，读取属性并调用方法：

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

调用使用已注册的 `settings` 实例，分页大小来自该对象。应用类无需实现 DataQL 接口；多个方法也可通过 [AbstractUdfSource](libraries.md) 组织为函数库。

## 导入对象的使用方式

| 对象类型 | 脚本用法 |
| --- | --- |
| 普通 POJO、Map | 读取属性，例如 `settings.name` |
| `Udf` | 调用函数，例如 `lookup('u1')`，实现见[自定义函数](functions.md) |
| `UdfSource` | 调用库内函数，例如 `text.upper('hello')`，实现见[函数库](libraries.md) |

## 查找与生命周期

执行 `import 'app.settings'` 时，Dataway 根据注册名称调用供应器，将取得的对象交给脚本。直接注册的导入优先于自定义 Finder，未找到时才委托 [Finder](finder.md)。

`() -> settings` 复用同一对象；供应器也可每次返回新对象。应用负责共享对象的并发访问和资源释放。注册内容属于应用配置，单次请求参数通过 API 参数传入。

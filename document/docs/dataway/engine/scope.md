---
title: "9.6 自定义作用域"
---

Dataway 已将 `${...}` 用于读取请求参数。通过 `CustomizeScope`，应用可以补充请求参数的默认值，并为 `@{...}`、`#{...}` 提供自定义数据。API 执行和控制台调试使用相同规则。

三种符号的扩展权限如下：

- `$`：只能提供默认参数。请求中的同名值会覆盖默认值，Dataway 仍负责参数合并和包装；`CustomizeScope` 无法接管或关闭这套处理。
- `@`、`#`：参数名和值完全由应用定义，Dataway 没有预设字段，也不会自动合并请求参数。

可自定义的是这三组参数的内容。符号及访问语法由 DataQL 固定，不能通过此接口增加新符号。

## 配置作用域

Dataway 调用 `findCustomizeEnvironment(symbol)`，传入 `$`、`@` 或 `#`，使用返回 Map 的键作为参数名。下面将分页默认值放入 `$`，应用名称放入 `@`，区域放入 `#`：

```java title="ApplicationScope.java"
package com.example.dataway;

import java.util.Map;
import net.hasor.dataql.kernel.CustomizeScope;

public class ApplicationScope implements CustomizeScope {
    @Override
    public Map<String, ?> findCustomizeEnvironment(String symbol) {
        return switch (symbol) {
            case "$" -> Map.of("pageSize", 20);
            case "@" -> Map.of("application", "orders");
            case "#" -> Map.of("region", "cn");
            default -> Map.of();
        };
    }
}
```

在创建 Dataway 前注册：

```java title="注册作用域"
import com.example.dataway.ApplicationScope;

config.customizeScope(new ApplicationScope());
```

作用域实例由查询共享，应保证并发安全。未配置或回调返回 `null` 时，不提供自定义值，`$` 仍可读取请求参数。

## 脚本使用

```javascript
return {
    'pageSize': ${pageSize},
    'application': @{application},
    'region': #{region}
};
```

不传请求参数时，`pageSize` 使用默认值 `20`。向该 API 提交以下 JSON：

```json title="请求正文"
{
    "pageSize": 50,
    "application": "client-app",
    "region": "us"
}
```

```json title="脚本结果"
{
    "pageSize": 50,
    "application": "orders",
    "region": "cn"
}
```

`${pageSize}` 使用请求值 `50`，覆盖默认值 `20`；`@{application}` 和 `#{region}` 使用应用提供的值，不受同名请求参数影响。请求中的 `client-app`、`us` 可通过 `${application}`、`${region}` 读取。默认 Structure 响应将上述结果放在 `value` 中。

## 使用规则

- `$` 同名参数优先级：请求正文 → URL 查询参数 → 默认值；显式 `null` 也会覆盖默认值。可信身份数据应放到 `@` 或 `#`。
- 开启 `wrapAllParameters` 且包装名为 `root` 时，使用 `${root}.pageSize`；`@`、`#` 不受影响。配置见 [API 选项](../capabilities/development/options.md)。
- `@`、`#` 每次读取时调用作用域实现；表达式中的 `$.name`、`#.name`、`@[0]` 属于[环境栈访问](../../dataql/syntax/valuescope.md)，不经过此接口。

SQL 占位符和作用域传参见 [SQL 执行器](../../dataql/sql/execute.md#sql-parameters-scope)。

---
title: "7.1 身份接入"
---

`IdentityProvider` 为每次请求提供应用已验证的 `UserIdentity`。可通过请求属性接入，也可从应用安全上下文获取。

## 选择身份

| 创建方法 | 权限 |
| --- | --- |
| `anonymous(attributes)` | 无操作权限 |
| `authenticated(id, attributes)` | 调用已发布 API、读取文档 |
| `consoleReadOnly(id, attributes)` | API、文档及控制台只读操作 |
| `consoleAdmin(id, attributes)` | 全部操作，包括调试与写入 |

根据应用已验证的用户角色创建身份：

```java title="创建用户身份"
import java.util.Map;
import net.hasor.dataway.authorization.UserIdentity;

Map<String, Object> attributes = Map.of("tenant", tenantId);
UserIdentity identity = switch (role) {
    case "admin" -> UserIdentity.consoleAdmin(userId, attributes);
    case "reader" -> UserIdentity.consoleReadOnly(userId, attributes);
    case "api" -> UserIdentity.authenticated(userId, attributes);
    default -> UserIdentity.anonymous(Map.of());
};
```

权限由创建方法决定。`identityId()` 返回用户标识，`attributes()` 返回创建时复制的只读属性 Map，嵌套对象由应用保持不可变。

## 接入请求身份

登录拦截器校验用户后，将身份写入请求属性：

```java title="Spring / Hasor"
request.setAttribute("host.identity", identity);
```

```java title="Solon"
context.attrSet("host.identity", identity);
```

```java title="Dataway 读取请求身份"
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.service.DatawayConfig;

DatawayConfig config = new DatawayConfig()
        .identityProvider(new RequestIdentityProvider("host.identity"));
```

属性缺失或类型不符时返回匿名身份。完整配置见 [Spring](../integration/spring.md#访问鉴权)、[Solon](../integration/solon.md#访问鉴权)、[Hasor](../integration/hasor.md#访问鉴权)。

## 自定义身份获取

实现 `IdentityProvider.resolve(WebRequest)` 并通过 `DatawayConfig.identityProvider(...)` 注册，可从应用安全上下文获取身份。返回值须为非空 `UserIdentity`，未登录时返回 `UserIdentity.anonymous(Map.of())`。未配置提供者时使用 `WebRequest` 的身份，初始为匿名。

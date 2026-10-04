---
title: "7.1 Identity integration"
---

`IdentityProvider` supplies an application-verified `UserIdentity` for each request, using a request attribute or the application's security context.

## Choose an identity

| Factory | Permissions |
| --- | --- |
| `anonymous(attributes)` | No operations |
| `authenticated(id, attributes)` | Published APIs and documents |
| `consoleReadOnly(id, attributes)` | APIs, documents and read-only management |
| `consoleAdmin(id, attributes)` | All operations, including debugging and writes |

Create an identity from the user's verified application role:

```java title="Create a user identity"
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

The factory determines permissions. `identityId()` returns the user identifier; `attributes()` returns a copied, read-only Map. The application keeps nested values immutable.

## Connect request identities

After validating the user, the login interceptor stores the identity in a request attribute:

```java title="Spring / Hasor"
request.setAttribute("host.identity", identity);
```

```java title="Solon"
context.attrSet("host.identity", identity);
```

```java title="Read the request identity in Dataway"
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.service.DatawayConfig;

DatawayConfig config = new DatawayConfig()
        .identityProvider(new RequestIdentityProvider("host.identity"));
```

A missing or incorrectly typed attribute produces an anonymous identity. See the complete setup for [Spring](../integration/spring.md#access-authorization), [Solon](../integration/solon.md#access-authorization) and [Hasor](../integration/hasor.md#access-authorization).

## Custom identity resolution

Implement `IdentityProvider.resolve(WebRequest)` and register it with `DatawayConfig.identityProvider(...)` to use an application security context. Return a non-null `UserIdentity`, using `UserIdentity.anonymous(Map.of())` when unauthenticated. Without a configured provider, Dataway uses the identity on `WebRequest`, initially anonymous.

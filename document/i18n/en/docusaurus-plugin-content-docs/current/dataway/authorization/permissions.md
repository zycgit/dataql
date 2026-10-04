---
title: "7.2 Authorization"
---

The default `DefaultAuthorizationCheck` calls `UserIdentity.checkOperation(operation)`. Identity presets require only an `IdentityProvider` integration.

## Operation permissions

| Operation | Use | API caller | Console reader | Console administrator |
| --- | --- | --- | --- | --- |
| `INVOKE` | Published API calls | Yes | Yes | Yes |
| `DOCUMENT` | Swagger / OpenAPI documents | Yes | Yes | Yes |
| `LIST`, `READ`, `HISTORY` | Lists, details, history | No | Yes | Yes |
| `SAVE`, `PUBLISH`, `DISABLE`, `DELETE` | Save, publish, disable, delete | No | No | Yes |
| `DEBUG` | Editor debugging and Smoke Test | No | No | Yes |

Anonymous identities have no permissions. API calls require a published, enabled interface. Debugging executes scripts and requires administrator permissions.

## Verify access

Start the [Spring example](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example) and use the read-only `reader` account to compare read and write access:

```bash title="Read-only access example"
# Log in and save the Cookie.
curl -i -c cookies.txt -X POST http://localhost:8080/session/login \
  -d 'username=reader&password=example-password'

# Listing is allowed.
curl -i -b cookies.txt http://localhost:8080/admin/api/api-list

# Saving is denied.
curl -i -b cookies.txt -X POST http://localhost:8080/admin/api/save-api \
  -H 'Content-Type: application/json' -d '{}'
```

## Extend checks

Replace the default checker with `DatawayConfig.authorizationCheck(...)`. For example, pass `Map.of("active", true)` in identity attributes and check both account status and operation permission:

```java title="Check account status and operation permission"
config.authorizationCheck((identity, operation) -> {
    return identity.checkOperation(operation)
            && Boolean.TRUE.equals(identity.attributes().get("active"));
});
```

Use [API interceptors](../configuration/api-interceptors.md) for API-specific and parameter rules.

## Page and service access

- Host login interceptors authorize console pages and assets, which bypass admin and API interceptor chains.
- Document entries check `DOCUMENT` and export published, enabled APIs without filtering by user.
- Applications authorize direct `AdminService` calls, which bypass Web permission checks and admin interceptor chains.

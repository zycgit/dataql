---
title: "7.2 权限检查"
---

默认检查器 `DefaultAuthorizationCheck` 调用 `UserIdentity.checkOperation(operation)`。使用预设身份时，接入 `IdentityProvider` 即可。

## 操作权限

| Operation | 场景 | API 访问 | 控制台只读 | 开发管理 |
| --- | --- | --- | --- | --- |
| `INVOKE` | 已发布 API 调用 | ✓ | ✓ | ✓ |
| `DOCUMENT` | Swagger / OpenAPI 文档 | ✓ | ✓ | ✓ |
| `LIST`、`READ`、`HISTORY` | 列表、详情、历史 | — | ✓ | ✓ |
| `SAVE`、`PUBLISH`、`DISABLE`、`DELETE` | 保存、发布、停用、删除 | — | — | ✓ |
| `DEBUG` | 编辑调试、Smoke Test | — | — | ✓ |

匿名身份无操作权限。API 调用要求接口已发布且启用；调试会执行脚本，需要开发管理权限。

## 验证访问效果

启动 [Spring 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)，使用只读账户 `reader` 验证查看与保存的权限差异：

```bash title="只读账户访问示例"
# 登录并保存 Cookie。
curl -i -c cookies.txt -X POST http://localhost:8080/session/login \
  -d 'username=reader&password=example-password'

# 允许查看列表。
curl -i -b cookies.txt http://localhost:8080/admin/api/api-list

# 拒绝保存。
curl -i -b cookies.txt -X POST http://localhost:8080/admin/api/save-api \
  -H 'Content-Type: application/json' -d '{}'
```

## 扩展检查规则

通过 `DatawayConfig.authorizationCheck(...)` 替换默认检查器。例如，在身份属性中传入 `Map.of("active", true)`，同时检查账户状态和操作权限：

```java title="检查账户状态与操作权限"
config.authorizationCheck((identity, operation) -> {
    return identity.checkOperation(operation)
            && Boolean.TRUE.equals(identity.attributes().get("active"));
});
```

针对具体 API、参数的规则可使用 [API 拦截器](../configuration/api-interceptors.md)。

## 页面与服务访问

- 控制台页面和静态资源由宿主登录拦截器鉴权，不进入管理和 API 拦截链。
- 文档入口检查 `DOCUMENT` 权限，导出已发布且启用的接口，不按用户筛选。
- 直接调用 `AdminService` 时由应用鉴权，不经过 Web 权限检查与管理拦截链。

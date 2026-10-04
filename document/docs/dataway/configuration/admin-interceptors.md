---
title: "10.2 管理拦截器"
description: "通过 AdminInterceptor 为管理操作配置审计、校验和事务。"
---

`AdminInterceptor` 在管理 Controller 调用前后执行，可用于操作审计、管理限制和事务控制。列表、保存、发布、停用、删除及调试等管理请求通过权限检查后进入此拦截链。

## 注册与使用

通过 `DatawayConfig.adminInterceptor(...)` 注册。下面在管理操作成功后记录目标接口、动作和用户：

```java title="记录管理操作"
import java.util.logging.Logger;
import net.hasor.dataway.model.ApiDefinition;

Logger audit = Logger.getLogger("dataway.audit");
config.adminInterceptor((context, chain) -> {
    Object result = chain.proceed();
    ApiDefinition definition = context.definition();
    String target = definition == null ? "collection" : definition.getMethod() + " " + definition.getPath();
    audit.info(() -> context.operation() + " " + target + " by " + context.identity().identityId());
    return result;
});
```

`chain.proceed()` 调用下一个拦截器，最后进入 Controller。可在调用前校验、调用后处理结果，也可用 `try/catch/finally` 处理失败和释放资源。元数据写入与审计需要一起提交、回滚时，配置见[事务整合](../metadata/transactions.md)。

## 上下文

`AdminInterceptorContext` 提供以下信息：

| 方法 | 内容 |
| --- | --- |
| `operation()` | 本次管理动作，如 `SAVE`、`PUBLISH`、`DEBUG` |
| `identity()` | `IdentityProvider` 解析的当前用户身份及属性 |
| `definition()` | 被操作的 API 定义，可读取标识、请求方法、路径等信息；列表及结果处理器查询为 `null` |
| `releaseId()` | 读取指定历史快照时的发布 ID，其余操作为 `null` |
| `parameters()` | 解码后的 URL 查询参数与提交正文，正文同名字段优先 |

已有 API 操作提供存储中的草稿；新建保存和未保存接口调试提供提交的定义。读取指定历史记录时，提供对应的发布快照。修改请求中的新路径、脚本等内容可通过 `parameters()` 读取，`definition()` 保留已有目标，便于校验操作权限。

## 执行与返回

多个拦截器按注册顺序进入，按相反顺序返回：`A → B → Controller → B → A`。

返回 `chain.proceed()` 的结果会保留管理操作的响应；直接返回其他结果会跳过后续拦截器和 Controller。普通对象转为 JSON 响应，`ResultInfo` 可指定状态码和响应内容。链内未处理的异常交给宿主异常处理机制。

管理端 Perform、Smoke 调试先经过管理拦截器，执行脚本时再进入 [API 拦截器](api-interceptors.md)。业务 API、文档和页面资源不经过管理拦截链。直接调用 `AdminService` 时，由应用自行安排校验和审计。

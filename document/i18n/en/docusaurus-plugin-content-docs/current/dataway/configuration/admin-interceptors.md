---
title: "10.2 Management interceptors"
description: "Use AdminInterceptor for auditing, validation and transactions around management operations."
---

`AdminInterceptor` surrounds management Controller calls for auditing, restrictions and transaction control. Management requests, including listing, saving, publishing, disabling, deleting and debugging, enter this chain after authorization.

## Registration and usage

Register through `DatawayConfig.adminInterceptor(...)`. This example logs the target API, operation and user after a successful call:

```java title="Audit management operations"
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

`chain.proceed()` invokes the next interceptor and ultimately the Controller. Validate before the call, process results after it, and use `try/catch/finally` for failures and cleanup. To commit or roll back metadata and audit writes together, see [Transaction integration](../metadata/transactions.md).

## Context

`AdminInterceptorContext` provides:

| Method | Content |
| --- | --- |
| `operation()` | Management action, such as `SAVE`, `PUBLISH` or `DEBUG` |
| `identity()` | Current identity and attributes resolved by `IdentityProvider` |
| `definition()` | Target API definition, including its ID, method and path; `null` for API and result-handler lists |
| `releaseId()` | Release ID when reading a selected history snapshot; otherwise `null` |
| `parameters()` | Decoded URL query parameters merged with submitted body fields; body fields take precedence |

Operations on existing APIs receive the stored draft. Saving a new API or debugging an unsaved API supplies the submitted definition. Reading a selected history record supplies its release snapshot. Submitted changes such as a new path or script remain available through `parameters()`, while `definition()` preserves the existing target for permission checks.

## Execution and results

Interceptors enter in registration order and return in reverse order: `A → B → Controller → B → A`.

Returning `chain.proceed()` preserves the management response. Returning another result directly skips later interceptors and the Controller. Plain objects become JSON responses; `ResultInfo` can specify status and content. Unhandled chain exceptions propagate to the host exception mechanism.

Perform and Smoke debugging pass through management interceptors first, then [API interceptors](api-interceptors.md) when executing scripts. Business APIs, documents and page assets bypass this management chain. Applications arrange validation and auditing for direct `AdminService` calls.

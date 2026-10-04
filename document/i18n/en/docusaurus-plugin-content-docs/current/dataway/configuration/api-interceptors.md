---
title: "10.3 API interceptors"
description: "Use ApiInterceptor to validate parameters, measure execution and process results."
---

`ApiInterceptor` surrounds DataQL execution for parameter validation, timing, transactions and result processing. HTTP calls, [Java calls](../capabilities/development/java.md) and management debugging share this chain, including SQL-type scripts.

## Registration and usage

Register through `DatawayConfig.apiInterceptor(...)`. This example checks a required parameter for `/person`:

```java title="Validate call parameters"
config.apiInterceptor((context, chain) -> {
    if ("/person".equals(context.definition().getPath())
            && context.parameters().get("id") == null) {
        throw new IllegalArgumentException("id is required");
    }
    return chain.proceed(context);
});
```

`chain.proceed(context)` passes the context to the next interceptor and ultimately executes the script. This example uses unwrapped parameters. With Wrap All Parameters enabled, read from the wrapper object; see [API options](../capabilities/development/options.md).

## Context

Scripts are compiled and parameters prepared before entering the chain. `ApiInterceptorContext` provides:

| Method | Content |
| --- | --- |
| `definition()` | Executed API definition, including its ID, method, path and script type; published calls use the release snapshot |
| `releaseId()` | Executed release ID; `null` for editor and draft debugging |
| `parameters()` | Execution parameters after merging CustomizeScope defaults and applying parameter wrapping |
| `operation()` | `INVOKE` for business calls; `DEBUG` for Perform and Smoke |
| `identity()` | Current identity and attributes; Java calls use an anonymous identity |
| `source()` | `PROGRAMMATIC` for Java calls, `DEBUG` for debugging, `HTTP` for API requests |

`ApiCallSource` identifies the invocation source independently of the identity and operation. Perform and Smoke use `DEBUG`. Calls from the Interface list and external API requests both use `HTTP`.

```java title="Identify programmatic calls"
import net.hasor.dataway.service.script.ApiCallSource;

config.apiInterceptor((context, chain) -> {
    if (context.source() == ApiCallSource.PROGRAMMATIC) {
        System.out.println("Programmatic API call: " + context.definition().getId());
    }
    return chain.proceed(context);
});
```

## Execution and results

Interceptors enter in registration order and return in reverse order: `A → B → script → B → A`. Add validation, timing and transactions around `proceed`, and release resources in `finally`.

Normal script execution returns a `QueryResult`, which the selected result handler converts to a response. A plain object returned by an interceptor becomes JSON; `ResultInfo` supplies its own status and content. Both bypass the result handler.

```java title="Return a response directly"
import java.util.Map;
import net.hasor.dataway.service.ResultInfoUtils;

config.apiInterceptor((context, chain) -> {
    return ResultInfoUtils.json(202, Map.of("accepted", true));
});
```

This returns HTTP 202 without invoking later interceptors or the script. `DatawayQuery` converts unhandled chain or script execution exceptions into failure results for the result handler. Routing, authorization and compilation occur outside the chain.

Management debugging also passes through [management interceptors](admin-interceptors.md) before entering this chain. Console assets and document requests bypass API interceptors.

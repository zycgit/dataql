---
slug: /dataway/principles
title: "4. How it works"
description: "Dataway execution flow, request processing and API publication."
---

Dataway executes scripts from published API definitions and returns business data in HTTP responses.

## Architecture {#architecture}

![Dataway technical architecture: request context, published APIs, UDFs, fragment processors and responses](/img/dataway/architecture.svg)

The application sends an HTTP request. Context carries its parameters, identity, headers and cookies. Dataway finds the published API by request method and path, then executes its script with DataQL.

Scripts obtain data through two extension points:

- UDFs invoke application functions to access HTTP, RPC or local business services.
- `FragmentProcess` executes SQL or custom script fragments. SQL uses application-configured data sources and transactions.

DataQL combines results, filters data, performs calculations and transforms structures. Dataway returns JSON or binary data according to response settings.

## Request processing {#request-flow}

Host MVC handles routing and authentication. Adapters supply `WebRequest` and `WebResponse`. `WebHandler` resolves the current identity through `IdentityProvider`.

![Request processing: management uses Controllers and AdminService; API calls find, compile and execute a release; console debugging reuses the execution engine](/img/dataway/request-flow.svg)

### API invocation

1. The API handler checks `INVOKE` permission and parses query parameters and the body. Body parsing follows Content-Type and its charset, defaulting to UTF-8.
2. The handler finds the active release by `(method, path)` and reads its script, request sample and options.
3. `DatawayEngine` compiles the script and creates a `DatawayQuery`.
4. `DatawayQuery` merges parameters with precedence request body → URL query → [CustomizeScope](../engine/scope.md) defaults, applies wrapping from [API options](../capabilities/development/options.md#parameter-wrapping), and invokes API interceptors and DataQL.
5. The query processes the result. `WebHandler` writes the response, releases upload caches and closes returned streams.

Invalid JSON or an invalid root raises a `DatawayException` with status 400. Unsupported media types or charsets, and non-empty bodies without Content-Type, produce status 415. The host web framework handles the exception response.

DataQL scripts compile directly. The engine represents SQL as a fragment-call syntax tree for DataQL compilation and `dataql-sqlproc` execution. SQL parameter names come from the request sample; values arrive with each invocation.

Individual API options override parameter wrapping and response templates in [DatawayConfig](../configuration/core.md). Query hints provide the current request and response to Web functions and are cleared after execution.

### Management and debugging

Management requests pass permission checks, `AdminInterceptor` and a Controller before `AdminService` accesses the DAL. Controllers convert console data; `AdminService` provides independent management methods.

Perform and Smoke use `DEBUG`. After admin interception, they reuse the query engine to execute editor contents and saved drafts, respectively.

Applications use `dataway.getAdminService()` for [programmatic management](../capabilities/programmatic.md). Callers provide access control.

## Publication and revisions {#lifecycle}

One API retains an editable draft and multiple release snapshots. The active snapshot serves API calls and documents. Previous snapshots remain in release history.

![API states and versions: saving updates the draft, publishing creates the active snapshot, previous releases enter history, and historical content can be restored to the draft](/img/dataway/api-versions.svg)

- Draft: Saving updates its editable content for development and debugging. API calls continue to use the current release.
- Published: Each publication creates an independent snapshot from the draft and switches API calls to that version.
- History: Release records retain the content of each publication. Restoring copies selected content into the draft; publishing creates a new snapshot.

The diagram shows v2 as the current release and v1 in history. Publishing again creates v3 and moves v2 into history. Publishing a restored copy of v1 also creates a new release. The labels v1, v2 and v3 indicate publication order.

Disabling retains the draft and release history while stopping API calls and document exposure. Publishing again serves the API from a new snapshot.

### Concurrent updates {#concurrent-updates}

`revision` controls concurrent updates and increments on saves, publications and disabling. Release numbers increment with publications. The two counters advance independently.

Creation uses `version = 0`; subsequent writes use the current `revision`. A version conflict raises a `DatawayException` with status 409. Callers reload the latest revision before resubmitting.

Publication updates state, disables the previous snapshot and creates the new one in an atomic batch. JDBC uses transactions; Nacos uses snapshot CAS. Details are in [metadata storage](../metadata/index.md).

## Interceptors {#interceptors}

Interceptors add application logic around management operations and script execution.

![Two interceptor chains: AdminInterceptor surrounds Controller calls, ApiInterceptor surrounds DataQL execution, and results return through the chain](/img/dataway/interceptors.svg)

- `AdminInterceptor` runs after management authorization. It receives the operation and identity for auditing, management restrictions and result processing.
- `ApiInterceptor` runs after parameter preparation. It receives the API definition, parameters, operation and identity for parameter validation, timing and transaction control.

Perform and Smoke enter the management chain first, then the API chain during query execution.

### Invocation model

Dataway connects interceptors in configuration order. Each interceptor receives the current context and the next execution entry:

1. The call enters the interceptor and runs its logic before `proceed`.
2. The management chain continues through `chain.proceed()`. The API chain passes its context through `chain.proceed(context)`. The final entry calls the Controller or DataQL.
3. Results return through the chain in reverse order. Each interceptor runs its logic after `proceed`.

Registering A and B produces this order: A before → B before → target call → B after → A after. A direct return ends subsequent execution and supplies the result.

Interceptors can use `try/catch/finally` around `proceed` for rollback and resource cleanup. Management-chain exceptions propagate to the host. Outside the API chain, `DatawayQuery` converts execution exceptions into failure results using [API options](../capabilities/development/options.md).

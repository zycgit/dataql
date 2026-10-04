---
title: "10.1 DatawayConfig"
---

`DatawayConfig` stores core options; `createDataway()` assembles services. Entry switches and paths are configured in the host; see [Spring](../integration/spring.md#entry-settings), [Solon](../integration/solon.md#entry-settings) and [Hasor](../integration/hasor.md#entry-settings).

```java title="Create Dataway"
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;

DatawayConfig config = new DatawayConfig()
        .dataAccessLayer(metadata)
        .identityProvider(identityProvider)
        .documentTitle("Order API");
Dataway dataway = config.createDataway();
```

Framework adapters create Dataway from the supplied DatawayConfig and DAL.

## Options

### Metadata storage

| Method | Default | Purpose and constraints |
| --- | --- | --- |
| `dataAccessLayer(layer)` | Empty | Supply an `ApiDataAccessLayer`, taking precedence over container lookup; required before core creation |
| `tableMapping(entity, name)` | Empty | Map an `EntityType` to a database table or Nacos entity name; unset entries use provider defaults. See [name mappings](../metadata/mapping.md) |
| `fieldMapping(entity, field, name)` | Empty | Map an entity's `FieldDef` to a storage field; unspecified fields keep provider defaults |

### Identity and interceptors

| Method | Default | Purpose and constraints |
| --- | --- | --- |
| `identityProvider(provider)` | Empty | Register an `IdentityProvider`; when unset, use `WebRequest::getIdentity`. Requests default to an anonymous identity without permissions |
| `authorizationCheck(check)` | Empty | Register an `AuthorizationCheck`; when unset, use `DefaultAuthorizationCheck`, which checks permissions through `UserIdentity.checkOperation` |
| `adminInterceptor(interceptor)` | Empty | Append an `AdminInterceptor` for management operations, in registration order |
| `apiInterceptor(interceptor)` | Empty | Append an `ApiInterceptor` for script execution, in registration order |

### Results and parameters

| Method | Default | Purpose and constraints |
| --- | --- | --- |
| `defaultResultHandler(name)` | `structure` | Select the default result handler: `structure`, `raw`, `csv`, `text`, `verifyCode` or a registered name |
| `resultHandler(name, handler)` | Five built-in handlers | Register a named `ResultHandler`; names start with a letter and contain letters, digits, `_`, `-` or `.`. Built-in `structure`, `raw`, `csv`, `text` and `verifyCode` can be replaced; `default` is reserved |
| `wrapAllParameters(enabled)` | `false` | Group all call parameters under `wrapParameterName`, for both DataQL and SQL |
| `wrapParameterName(name)` | `root` | Wrapper name; starts with a letter or underscore and contains only letters, digits and underscores |

### Upload cache

| Method | Default | Purpose and constraints |
| --- | --- | --- |
| `uploadTempDirectory(path)` | Empty | Set the upload spill directory using a `Path`; when unset, use the system temporary directory. Temporary files are cleaned up after the request |
| `uploadMemoryThreshold(bytes)` | `51200` bytes | Per-file memory threshold before spilling to disk; nonnegative, with `0` spilling every nonempty file |

### Document metadata

| Method | Default | Purpose and constraints |
| --- | --- | --- |
| `documentTitle(title)` | `Dataway API` | Swagger and OpenAPI document title; must not be blank |
| `documentVersion(version)` | `1.0` | Application API version in the document; must not be blank |
| `documentServer(server)` | `/api` | Public API base in the document; a root-relative path or HTTP(S) URL without query, fragment or credentials |

### Engine and extensions

| Method | Default | Purpose and constraints |
| --- | --- | --- |
| `finder(finder)` | `DatawayFinder` | Set the `Finder` for objects, resources and fragments; `null` restores the default implementation |
| `resourceLoader(loader)` | `ClassPathResourceLoader.INSTANCE` | Set the resource loader on `DatawayFinder`; `null` restores the default |
| `classLoader(loader)` | The ClassLoader that loaded `DatawayFinder` | Set the class loader on `DatawayFinder`; `null` restores the default |
| `customizeScope(scope)` | Empty | Provide `$` defaults and `@` / `#` environments; call parameters override `$` defaults. When unset, use an implementation returning an empty Map |
| `configureHost(customizer)` | Empty | Register a `Consumer<HostConfiguration>`, called during core initialization in registration order |
| `configureQuery(customizer)` | Empty | Register a `Consumer<QueryBuilder>`, called for every new query in registration order |
| `function(name, udf)` | Empty | Register a `Udf` callable directly from queries |
| `library(namespace, functions)` | Empty | Register a `Map<String, Udf>` library loaded through `import` |
| `importSource(name, provider)` | Empty | Register a `Supplier<?>` for an imported object |
| `fragment(name, provider)` | Empty | Register a supplier of `FragmentProcess` for a named fragment |
| `attachment(type, object)` | Empty | Register an engine resource by type; register SQL connection and transaction providers with `ConnectionProvider.class` |

Configuration takes effect during initialization. The host owns external resources; use separate DAL instances for different mappings.

Custom `Finder` implementations own their loaders. The `resourceLoader` and `classLoader` methods apply to `DatawayFinder` and its subclasses. See [engine extensions](../engine/index.md) for function, loader and scope examples.

Extension guides: [functions](../engine/functions.md), [libraries](../engine/libraries.md), [application imports](../engine/imports.md), [fragments](../engine/fragments.md), [Finder](../engine/finder.md), [custom scopes](../engine/scope.md) and [engine and query configuration](../engine/customizers.md).

## Response and parameter configuration {#response}

`defaultResultHandler`, `wrapAllParameters` and `wrapParameterName` set application defaults that API options can override. Pass handler defaults through its constructor, such as `new StructureResultHandler(Map.of("responseFormat", template))`, then register it using `resultHandler("structure", handler)`.

See [API options](../capabilities/development/options.md) for editor controls, parameter wrapping examples and the complete response template.

Creating `DatawayConfig` registers Structure, Raw Value, CSV, Text and VerifyCode. Registering the same name replaces its handler; the engine uses this configured collection.

APIs select registered handlers using the `resultHandler` option. See [custom result handlers](../capabilities/result-handlers/custom.md).

## File uploads {#upload}

```java title="Upload cache configuration"
import java.nio.file.Path;

config.uploadTempDirectory(Path.of("/var/tmp/dataway"))
        .uploadMemoryThreshold(64 * 1024);
```

Files cache up to 50 KiB by default, then spill to temporary storage. Thresholds are nonnegative; zero writes directly to disk. Requests clean up caches and streams on completion, so persist retained content beforehand.

WebFile provides metadata and file streams. The host sets upload limits. Filters that consume bodies must retain replayable content.

## API document output {#api-documents}

```java title="API document configuration"
config.documentTitle("Order API")
        .documentVersion("1.0")
        .documentServer("/gateway/api");
```

documentVersion identifies the application API version. documentServer is a root-relative path or HTTP(S) URL for the public API base.

Set the public API address explicitly for proxies and context paths. `documentServer` controls the URL in generated documents; entry registration follows the host settings.

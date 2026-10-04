---
title: "8.3 Core APIs"
---

The engine uses these objects for configuration, compilation and execution:

```text
HostConfiguration → QueryManager → QueryBuilder → Query → QueryResult
```

## Responsibilities

| Type | Responsibility | Common entry points |
| --- | --- | --- |
| `HostConfiguration` | Implements `HostContext`; holds loaders, imports, fragments and application objects | `addImport`, `addFragment`, `addAttachment` |
| `HostContext` | Supplies object lookup, loaders and application objects registered by type | `findBean`, `findFragmentProcess`, `getAttachment` |
| `QueryManager` | Shares one `HostContext` across new query builders | `newBuilder()` |
| `QueryBuilder` | Configures queries, parses scripts and creates QIL or Query instances | `parserQuery`, `compilerQuery`, `createQuery` |
| `Query` | Executes compiled scripts with supplied parameters | `execute` |
| `QueryResult` | Holds the return value, result code and duration | `getData`, `getCode`, `executionTime` |

`QueryModel` is the parsed syntax tree; `QIL` contains compiled instructions. Use `createQuery(script)` for ordinary calls. For separate compilation steps, see [Standalone usage](execute.md).

## Shared variables

Declare shared variables before compilation and reference them by name. Supply call parameters during execution and read them through `${...}`:

```java
import java.util.Map;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;

QueryBuilder builder = new QueryManager(new HostConfiguration()).newBuilder();
builder.addShareVar("application", () -> "orders");
Query query = builder.createQuery("return application + ':' + ${name};");
System.out.println(query.execute(Map.of("name", "Alice")).getData().unwrap());
```

The output is `orders:Alice`. Suppliers run when a Query is created. Update an existing variable through `query.addShareVar(name, value)`; adding a new variable name requires recompilation.

## Hints {#hint}

`QueryBuilder.setHint(name, value)` supplies defaults for new queries. `Query.setHint(name, value)` overrides them, and `removeHint(name)` removes an option. Explicit script settings take precedence during execution.

```java
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;

QueryBuilder builder = new QueryManager(new HostConfiguration()).newBuilder();
builder.setHint("INDEX_OVERFLOW", "near");
Query query = builder.createQuery("var values = [10,20]; return values[5];");
query.setHint("INDEX_OVERFLOW", "null");
Object result = query.execute().getData().unwrap(); // null
```

`setHint(name, Object)` can carry application objects. Copying Hints or cloning a Query preserves their references. Each execution uses a separate Hint container; script changes do not write back to the Query.

See the [Hint reference](../../dataql/hints/index.md) for syntax and options, and [Custom functions](../engine/functions.md#execution-context) for reading these objects from a UDF.

## Extension registration {#extension-registration}

Use the existing implementation guides and register extensions directly when running the engine independently:

| Extension | Engine registration | Implementation guide |
| --- | --- | --- |
| UDF | `builder.addShareVar(name, () -> udf)` | [Custom functions](../engine/functions.md) |
| Libraries and application objects | `host.addImport(name, supplier)` | [Function libraries](../engine/libraries.md), [Application imports](../engine/imports.md) |
| External fragments | `host.addFragment(name, supplier)` | [Fragment processors](../engine/fragments.md) |
| Finder | `new HostConfiguration(finder)` | [Finder](../engine/finder.md) |
| Application resources | `host.addAttachment(type, instance)` | [Engine and query configuration](../engine/customizers.md) |
| Parameter scopes | `query.execute(customizeScope)` | [Custom scopes](../engine/scope.md); Dataway's merging rules apply only within Dataway |

`QueryWrap` delegates to a `Query`. Subclass it and override `execute(CustomizeScope)` to add execution logic. The other execute overloads call this method.

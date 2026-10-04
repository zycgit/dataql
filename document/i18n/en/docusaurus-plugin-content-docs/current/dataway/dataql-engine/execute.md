---
title: "8.1 Standalone usage"
---

Use `dataql-engine` to compile and execute queries in a Java application. See the [overview](index.md) for a first example. The application supplies input data and handles query results.

## Execution parameters

| Call | Script access |
| --- | --- |
| `execute()` | No parameters |
| `execute(Map<String, ?>)` | Named values, such as `${name}` |
| `execute(Object[])` | Positional values, such as `${_0}` and `${_1}` |
| `execute(CustomizeScope)` | Separate `$`, `@` and `#` environments |

With direct `execute(Map)` calls, `${name}`, `@{name}` and `#{name}` read the same Map. Use `CustomizeScope` to separate them. Dataway adds its own default merging and request wrapping; see [Custom scopes](../engine/scope.md).

## Compile and reuse

`createQuery(String)` compiles immediately. To reuse compiled code, obtain QIL first and create a query for each call:

```java title="CompiledQueryExample.java"
import java.util.Map;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;

public class CompiledQueryExample {
    public static void main(String[] args) throws Exception {
        QueryManager manager = new QueryManager(new HostConfiguration());
        QueryBuilder builder = manager.newBuilder();
        QIL compiled = builder.compilerQuery("return ${price} * ${count};");

        Query first = builder.createQuery(compiled);
        System.out.println(first.execute(Map.of("price", 10, "count", 2)).getData().unwrap());

        Query second = builder.createQuery(compiled);
        System.out.println(second.execute(Map.of("price", 10, "count", 3)).getData().unwrap());
    }
}
```

The output is `20`, then `30`. Both queries use the same QIL with separate parameters. `QueryManager` creates builders; the application manages any compiled-code cache.

`QueryBuilder` also reads scripts from a `Reader` or `InputStream`. Streams use UTF-8 by default, with an overload for an explicit charset. To inspect or modify the syntax tree, call `parserQuery(...)` for a `QueryModel`, then compile it with `compilerQuery(model)`.

## Results and errors

`execute(...)` returns a `QueryResult`:

| Method | Meaning |
| --- | --- |
| `getData().unwrap()` | Convert the script result to a Java object |
| `getCode()` | Script result code; defaults to `0` |
| `getExitType()` / `isExit()` | Distinguish `return` from `exit` |
| `executionTime()` | Query execution duration |

Parsing, compilation and execution can each throw exceptions. Runtime failures use `QueryRuntimeException`, which includes location information. The engine does not create HTTP responses; the caller handles response formats and errors.

Reuse `HostConfiguration` after configuration. `QueryBuilder` and `Query` contain mutable settings; avoid changing shared variables or Hints while executing queries concurrently.

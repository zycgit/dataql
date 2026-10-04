---
slug: /dataway/dataql-engine
title: "8. DataQL engine"
hide_table_of_contents: true
---

The DataQL engine compiles scripts into QIL and executes them directly in Java applications. Create queries through `HostConfiguration → QueryManager → QueryBuilder → Query` and supply parameters at execution time.

## Compilation and execution

The parser creates a query model, which the compiler turns into QIL. The executor runs the instructions, invokes UDFs as needed and returns the query result.

![DataQL compilation and execution](/img/dataql/compiler-flow.png)

## Add the dependency

```xml title="pom.xml"
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataql-engine</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
```

## Run your first query

```java title="QueryExample.java"
import java.util.Map;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.QueryResult;

public class QueryExample {
    public static void main(String[] args) throws Exception {
        HostConfiguration host = new HostConfiguration();
        QueryManager manager = new QueryManager(host);
        Query query = manager.newBuilder().createQuery("return ${name};");
        QueryResult result = query.execute(Map.of("name", "DataQL"));
        System.out.println(result.getData().unwrap());
    }
}
```

The script reads the supplied parameter through `${name}`. `result.getData().unwrap()` converts the result into a Java object. Running the example prints:

```text
DataQL
```

## Guides

- [Standalone usage](execute.md): Supply parameters, reuse compiled code and handle results.
- [Data models](model.md): Convert Java data, read results and handle binary resources.
- [Core APIs](core.md): Understand configuration, builders, queries and result objects.
- [SQL executor](sql.md): Add SQL execution and configure database connections.
- [JSR-223](jsr223.md): Execute and precompile queries through the JDK scripting APIs.
- [QIL instruction reference](instruction.md): Look up instructions when inspecting compiled code.

For functions, fragments, Finders and scopes, see [Engine extensions](../engine/index.md).

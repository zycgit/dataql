---
slug: /dataway/dataql-engine
title: "8. DataQL 引擎"
hide_table_of_contents: true
---

DataQL 引擎将脚本编译为 QIL 并执行，可直接嵌入 Java 应用。通过 `HostConfiguration → QueryManager → QueryBuilder → Query` 创建查询，在执行时传入参数。

## 编译与执行

脚本先解析为查询模型，再编译为 QIL。执行器运行指令，按需调用 UDF，并返回查询结果。

![DataQL 编译与执行流程](/img/dataql/compiler-flow.png)

## 引入依赖

```xml title="pom.xml"
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataql-engine</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
```

## 执行第一个查询

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

脚本通过 `${name}` 读取传入的参数，`result.getData().unwrap()` 将查询结果转为 Java 对象。运行后输出：

```text
DataQL
```

## 使用指引

- [独立使用](execute.md)：传入参数、复用编译结果并处理执行结果。
- [数据模型](model.md)：转换 Java 数据、读取结果并处理二进制资源。
- [核心接口](core.md)：了解配置、构建器、查询和结果对象的职责。
- [SQL 执行器](sql.md)：引入 SQL 执行能力并配置数据库连接。
- [JSR-223](jsr223.md)：通过 JDK 脚本接口执行和预编译查询。
- [QIL 指令参考](instruction.md)：需要查看编译结果时查阅指令说明。

函数、片段、查找器和作用域的实现见[引擎扩展](../engine/index.md)。

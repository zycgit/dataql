---
title: "8.1 独立使用"
---

引入 `dataql-engine` 即可在 Java 应用中编译和执行查询，入门示例见[概览](index.md)。应用负责提供输入数据并处理查询结果。

## 执行参数

`Query.execute(...)` 支持以下方式：

| 调用方式 | 脚本取值 |
| --- | --- |
| `execute()` | 不提供参数 |
| `execute(Map<String, ?>)` | 按键名读取，如 `${name}` |
| `execute(Object[])` | 按位置读取，如 `${_0}`、`${_1}` |
| `execute(CustomizeScope)` | 分别提供 `$`、`@`、`#` 三组参数 |

直接调用 `execute(Map)` 时，`${name}`、`@{name}`、`#{name}` 读取同一份 Map。需要区分三组数据时，使用 `CustomizeScope`。Dataway 的默认值合并和请求参数包装由上层实现，见[自定义作用域](../engine/scope.md)。

## 编译与复用

`createQuery(String)` 会立即编译脚本。需要复用编译结果时，先取得 QIL，再为各次调用创建查询：

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

输出为 `20`、`30`，两次查询复用 QIL，各自接收参数。`QueryManager` 负责创建构建器，编译缓存由应用自行管理。

`QueryBuilder` 还支持从 `Reader`、`InputStream` 读取脚本；流的默认字符集为 UTF-8，也可显式指定。需要检查或修改语法树时，使用 `parserQuery(...)` 获取 `QueryModel`，再通过 `compilerQuery(model)` 编译。

## 结果与异常

`execute(...)` 返回 `QueryResult`：

| 方法 | 含义 |
| --- | --- |
| `getData().unwrap()` | 将脚本结果转为 Java 对象 |
| `getCode()` | 脚本结果码，默认 `0` |
| `getExitType()` / `isExit()` | 区分 `return` 和 `exit` |
| `executionTime()` | 查询执行耗时 |

语法解析、编译和运行阶段均可能抛出异常；运行阶段通过 `QueryRuntimeException` 提供错误位置等信息。引擎不生成 HTTP 响应，返回格式和异常响应由调用方处理。

`HostConfiguration` 可在配置完成后复用。`QueryBuilder`、`Query` 包含可变配置，应用应避免在并发执行时修改共享变量和 Hint。

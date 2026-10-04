---
title: "8.3 核心接口"
---

引擎通过以下对象完成配置、编译和执行：

```text
HostConfiguration → QueryManager → QueryBuilder → Query → QueryResult
```

## 接口职责

| 类型 | 职责 | 常用入口 |
| --- | --- | --- |
| `HostConfiguration` | 实现 `HostContext`，保存加载器、导入对象、片段执行器和应用对象 | `addImport`、`addFragment`、`addAttachment` |
| `HostContext` | 向引擎和扩展提供对象查找、加载器及按类型注册的应用对象 | `findBean`、`findFragmentProcess`、`getAttachment` |
| `QueryManager` | 持有同一个 `HostContext`，创建查询构建器 | `newBuilder()` |
| `QueryBuilder` | 设置查询配置，解析脚本并生成 QIL 或 Query | `parserQuery`、`compilerQuery`、`createQuery` |
| `Query` | 接收参数、执行已编译的脚本 | `execute` |
| `QueryResult` | 保存返回值、结果码和执行耗时 | `getData`、`getCode`、`executionTime` |

`QueryModel` 是解析后的语法树，`QIL` 是编译后的指令。普通查询直接使用 `createQuery(script)`，分步编译见[独立使用](execute.md)。

## 共享变量

共享变量在编译前声明，脚本通过变量名直接访问；调用参数在执行时传入，通过 `${...}` 读取：

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

输出为 `orders:Alice`。供应器在创建 Query 时调用；已有变量可通过 `query.addShareVar(name, value)` 更新值，新增变量名需要重新编译。

## Hint {#hint}

`QueryBuilder.setHint(name, value)` 为新查询提供初始配置，`Query.setHint(name, value)` 可覆盖该值，`removeHint(name)` 可将其移除。脚本中的显式设置在执行时优先。

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

`setHint(name, Object)` 可存放应用对象，复制 Hint 或克隆 Query 时保留对象引用。每次执行使用独立的 Hint 容器，脚本修改不会回写 Query。

脚本语法与选项见 [Hint 参考手册](../../dataql/hints/index.md)，UDF 读取这些对象的方式见[自定义函数](../engine/functions.md#execution-context)。

## 扩展入口

扩展实现沿用已有文档，独立使用引擎时按下表注册：

| 扩展 | 引擎注册方式 | 实现说明 |
| --- | --- | --- |
| UDF | `builder.addShareVar(name, () -> udf)` | [自定义函数](../engine/functions.md) |
| 函数库、应用对象 | `host.addImport(name, supplier)` | [函数库](../engine/libraries.md)、[应用对象导入](../engine/imports.md) |
| 外部片段 | `host.addFragment(name, supplier)` | [片段执行器](../engine/fragments.md) |
| 查找器 | `new HostConfiguration(finder)` | [查找器](../engine/finder.md) |
| 应用资源 | `host.addAttachment(type, instance)` | [引擎与查询配置](../engine/customizers.md) |
| 参数作用域 | `query.execute(customizeScope)` | [自定义作用域](../engine/scope.md)；Dataway 的参数合并规则仅适用于 Dataway |

`QueryWrap` 是 `Query` 的委托包装类。需要在执行前后附加逻辑时，可继承它并重写 `execute(CustomizeScope)`；其他执行重载会进入该方法。

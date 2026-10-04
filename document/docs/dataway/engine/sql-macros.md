---
title: "9.9 SQL 片段"
---

:::info[依赖 SQL 执行器]

本扩展依赖 [SQL 执行器](../dataql-engine/sql.md)（`dataql-sqlproc`），使用前请完成 Dataway 的[数据源接入](../capabilities/datasources.md)。

:::

SQL 宏将公共 SQL 文本注册为名称，供文本 SQL 和 XML 动态 SQL 复用。

## 在 Dataway 中注册

`config` 表示应用注册的 `DatawayConfig`。在其配置方法中加入以下回调，并在创建 Dataway 前完成配置。Spring、Solon、Hasor 均使用此入口，配置时机见[引擎与查询配置](customizers.md)。

```java
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContextImpl;

config.configureHost(host -> {
    ExecuteContext context = host.getAttachment(ExecuteContext.class);
    ((ExecuteContextImpl) context).addMacro("adult", "age >= 18");
});
```

回调将 `adult` 注册到当前 Dataway 引擎。宏注册入口位于内置实现 `ExecuteContextImpl`，因此先取得 `ExecuteContext`，再转换为该实现调用 `addMacro`。

## 使用

先在业务数据库中初始化 [SQL 执行](../../dataql/sql/execute.md)中的 `people` 示例表，再在控制台创建 DataQL 类型的 API。文本 SQL 通过 `@{macro, adult}` 引用已注册片段：

```javascript
var find = @@selectSql()<%
    SELECT count(*) FROM people WHERE @{macro, adult}
%>;
return find();
```

XML 动态 SQL 通过 `include` 引用同一片段：

```javascript
var find = @@selectXml()<%
    SELECT count(*) FROM people WHERE <include refid="adult"/>
%>;
return find();
```

两种写法都会将 `age >= 18` 拼入 SQL。调试 API，或保存、发布后调用，按示例数据得到的脚本结果均为 `2`。

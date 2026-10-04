---
title: "9.8 SQL 拦截器"
---

:::info[依赖 SQL 执行器]

本扩展依赖 [SQL 执行器](../dataql-engine/sql.md)（`dataql-sqlproc`），使用前请完成 Dataway 的[数据源接入](../capabilities/datasources.md)。

:::

`SqlExecutionInterceptor` 拦截最终的 JDBC 执行，可用于 SQL 日志、耗时统计和执行检查。

## 实现拦截器

```java
package com.example.sql;

import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInvocation;

public class SqlTraceInterceptor implements SqlExecutionInterceptor {
    private final System.Logger logger = System.getLogger(SqlTraceInterceptor.class.getName());

    @Override
    public Object invoke(SqlExecutionInvocation invocation) throws Throwable {
        this.logger.log(System.Logger.Level.INFO, invocation.getSqlInfo().queryString());
        return invocation.proceed();
    }
}
```

## 在 Dataway 中注册

`config` 表示应用注册的 `DatawayConfig`。在其配置方法中加入以下回调，并在创建 Dataway 前完成配置。Spring、Solon、Hasor 均使用此入口，配置时机见[引擎与查询配置](customizers.md)。

```java
import com.example.sql.SqlTraceInterceptor;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;

config.configureHost(host -> {
    ExecuteContext context = host.getAttachment(ExecuteContext.class);
    context.addInterceptor(new SqlTraceInterceptor());
});
```

Dataway 初始化引擎时提供 `host`，回调将拦截器注册到该引擎的 `ExecuteContext`。已发布 API 和控制台调试共用这份注册。

## 使用

在 Dataway 控制台创建 DataQL 类型的 API，调试或发布后调用：

```javascript
var find = @@selectSql(value)<% SELECT #{value} AS result_value %>;
return find(42);
```

每次查询都会进入 `invoke()`。示例以 INFO 级别记录最终 SQL `SELECT ? AS result_value`，再通过 `proceed()` 执行，脚本结果为 `42`。

## 执行规则

拦截器按注册顺序执行。`getSqlInfo()` 提供最终 SQL、绑定参数、数据源名称和 Hint；分页的总数查询和当前页查询分别进入拦截链。

`addInterceptor(interceptor, predicate)` 可按操作类型、片段内容和 Hint 筛选。匹配结果保存在片段配置缓存中，应在首次执行 SQL 前完成注册。参数信息用于观察；直接返回值时须满足执行链要求的结果类型。

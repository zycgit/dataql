---
title: "9.10 SQL 规则"
---

:::info[依赖 SQL 执行器]

本扩展依赖 [SQL 执行器](../dataql-engine/sql.md)（`dataql-sqlproc`），使用前请完成 Dataway 的[数据源接入](../capabilities/datasources.md)。

:::

`SqlRule` 为动态 SQL 增加应用规则。下面将查询有效记录的固定条件封装为一个规则。

## 实现规则

```java
package com.example.sql;

import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

/** Adds the application's fixed active-record condition. */
public class EnabledRule implements SqlRule {
    @Override
    public boolean test(SqlArgSource data, QueryContext context, String expression) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context, SqlBuilder sql, String expression, String value) {
        sql.appendSql("enabled = 1");
    }
}
```

## 在 Dataway 中注册

`config` 表示应用注册的 `DatawayConfig`。在其配置方法中加入以下回调，并在创建 Dataway 前完成配置。Spring、Solon、Hasor 均使用此入口，配置时机见[引擎与查询配置](customizers.md)。

```java
import com.example.sql.EnabledRule;
import net.hasor.dataql.sqlproc.dynamic.rule.RuleRegistry;

config.configureHost(host -> {
    RuleRegistry.DEFAULT.register("manualEnabled", new EnabledRule());
});
```

回调在 Dataway 初始化时注册 `manualEnabled`。`RuleRegistry.DEFAULT` 是共享注册表，注册结果对使用它的 SQL 执行器都生效。应用规则应使用独立名称，实现类应保持线程安全。

## 使用

先初始化 [SQL 执行](../../dataql/sql/execute.md)中的 `people` 示例表，再在控制台创建 DataQL 类型的 API，使用注册名调用规则：

```javascript
var find = @@selectSql()<% SELECT count(*) FROM people WHERE @{manualEnabled} %>;
return find();
```

`test` 决定规则是否输出，`executeRule` 向 `SqlBuilder` 添加 SQL 和参数。示例生成 `WHERE enabled = 1`；调试 API，或保存、发布后调用，按示例数据得到的脚本结果为 `2`。内置规则见[动态规则](../../dataql/sql/rules.md)。

---
title: "9.12 SQL 方言"
---

:::info[依赖 SQL 执行器]

本扩展依赖 [SQL 执行器](../dataql-engine/sql.md)（`dataql-sqlproc`）。在 Dataway 中使用时，先完成[数据源接入](../capabilities/datasources.md)。

:::

`PageDialect` 为分页查询生成总数 SQL 和当前页 SQL。内置方言及脚本分页用法见[分页与方言](../../dataql/sql/dialect.md)，应用可通过实现该接口支持其他分页语法。

## 实现方言

下面以 H2 支持的 `LIMIT ? OFFSET ?` 为例，为尚未分页、没有末尾分号的 SELECT 追加分页条件。`start` 是从 0 开始的记录偏移，`limit` 是每页条数。

```java
package com.example.sql;

import java.util.Arrays;
import net.hasor.dataql.sqlproc.dialect.BoundSql;
import net.hasor.dataql.sqlproc.dialect.BoundSql.BoundSqlObj;
import net.hasor.dataql.sqlproc.dialect.PageDialect;

public class LimitOffsetDialect implements PageDialect {
    @Override
    public BoundSql pageSql(BoundSql boundSql, long start, long limit) {
        Object[] original = boundSql.getArgs();
        Object[] arguments = Arrays.copyOf(original, original.length + 2);
        arguments[original.length] = limit;
        arguments[original.length + 1] = start;
        String sql = boundSql.getSqlString() + " LIMIT ? OFFSET ?";
        return new BoundSqlObj(sql, arguments);
    }
}
```

原 SQL 参数保持原顺序，新增参数依次对应 `LIMIT` 和 `OFFSET`。默认 `countSql` 将原 SQL 包装为 `SELECT COUNT(*) FROM (...) as TEMP_T`，需要其他计数语法时可重写该方法。

方言类需提供公开无参构造方法；上例由 Java 自动生成。方言实例会被缓存，应避免保存单次查询状态。

## 在 Dataway 中注册

`config` 表示应用注册的 `DatawayConfig`，在创建 Dataway 前加入以下配置：

```java
import com.example.sql.LimitOffsetDialect;
import net.hasor.dataql.sqlproc.dialect.SqlDialectRegister;

config.configureHost(host -> {
    SqlDialectRegister.registerDialectAlias("appLimitOffset", LimitOffsetDialect.class);
});
```

Spring、Solon、Hasor 均可使用此配置。别名注册表和方言缓存由 SQL 模块共享，应使用应用自己的别名，并在首次分页查询前完成注册。独立使用引擎时，在创建查询前直接调用同一注册方法。

## 脚本使用

在 H2 中初始化 [SQL 执行](../../dataql/sql/execute.md)的 `people` 示例表，再在控制台创建 DataQL 类型的 API：

```javascript
hint FRAGMENT_SQL_PAGE_DIALECT = 'appLimitOffset';
hint FRAGMENT_SQL_QUERY_BY_PAGE = true;
hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1;
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(minAge)<%
    SELECT id, name FROM people WHERE age >= #{minAge} ORDER BY id
%>;
var page = find(18);
run page.setPageInfo({'currentPage':2, 'pageSize':1});
return page.data();
```

调试或发布后调用，当前页返回 Bob。分页 SQL 的参数依次为 `18`、`1`、`1`，分别对应年龄、每页条数和记录偏移。

也可将 `FRAGMENT_SQL_PAGE_DIALECT` 设为 `com.example.sql.LimitOffsetDialect`，直接按类名加载，无需注册别名。

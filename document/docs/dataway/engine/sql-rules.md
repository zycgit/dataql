---
title: "9.10 SQL 规则"
---

:::info[依赖 SQL 执行器]

本扩展依赖 [SQL 执行器](../dataql-engine/sql.md)（`dataql-sqlproc`），使用前请完成 Dataway 的[数据源接入](../capabilities/datasources.md)。

:::

`SqlRule` 为 `@{规则名, 表达式, 内容}` 提供自定义实现，适合封装应用共用的筛选条件或参数处理。规则在 SQL 执行前运行，向 `SqlBuilder` 写入 SQL 和绑定值。

## 实现固定条件

将有效记录的条件封装为 `manualEnabled`：

```java
package com.example.sql;

import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

public class EnabledRule implements SqlRule {
    @Override
    public boolean test(SqlArgSource data, QueryContext context, String expression) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context,
                            SqlBuilder sql, String expression, String value) {
        sql.appendSql("enabled = 1");
    }
}
```

`test` 返回 true 才调用 `executeRule`；返回 false 时整个规则不输出内容。该规则没有业务参数，每次生成固定条件。

## 在 Dataway 中注册

在应用提供的 `DatawayConfig` 配置中注册规则，框架整合模块负责创建 Dataway。下面 `connectionProvider` 是应用已配置的 SQL 数据源提供者：

```java
import com.example.sql.EnabledRule;
import net.hasor.dataql.sqlproc.dynamic.rule.RuleRegistry;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.service.DatawayConfig;

public class SqlConfiguration {
    public DatawayConfig datawayConfig(ConnectionProvider connectionProvider) {
        DatawayConfig config = new DatawayConfig();
        config.attachment(ConnectionProvider.class, connectionProvider);
        config.configureHost(host -> {
            RuleRegistry.DEFAULT.register("manualEnabled", new EnabledRule());
        });
        return config;
    }
}
```

将此方法返回的配置交给 [Spring](../integration/spring.md)、[Solon](../integration/solon.md)或 [Hasor](../integration/hasor.md) 的配置入口。已有 `DatawayConfig` 时，只需加入 `configureHost` 回调。独立使用 DataQL 引擎时，在创建查询前调用同一条 `RuleRegistry.DEFAULT.register(...)`。

`RuleRegistry.DEFAULT` 是共享注册表。应用启动时注册一次，名称不区分大小写，同名注册会覆盖已有规则。选择应用专用名称，实现类应保持无状态或线程安全，避免在字段中保存本次请求的数据。

## 从脚本使用

先初始化 [SQL 执行](../../dataql/sql/execute.md)中的 `people` 表，再执行：

```javascript
var find = @@selectSql()<%
    SELECT count(*) FROM people WHERE @{manualEnabled}
%>;
return find();
```

生成 `SELECT count(*) FROM people WHERE enabled = 1`。Dataway 控制台的 SQL 类型 API 可直接填写此 SQL，DataQL 类型 API 使用完整脚本。内置规则见[动态规则](../../dataql/sql/rules.md)。

## 生成绑定参数

下面的 `appEquals` 把指定参数绑定到一个已允许的列，用于复用字段筛选。列名从应用白名单选择，参数值始终使用 JDBC 占位符。

```java
package com.example.sql;

import java.sql.SQLException;
import java.util.Set;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.SqlBuilder;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.SqlArgSource;

public class EqualsRule implements SqlRule {
    private static final Set<String> COLUMNS = Set.of("id", "name", "age");

    @Override
    public boolean test(SqlArgSource data, QueryContext context, String expression) {
        return true;
    }

    @Override
    public void executeRule(SqlArgSource data, QueryContext context,
                            SqlBuilder sql, String expression, String value) throws SQLException {
        String parameter = expression == null ? "" : expression.trim();
        String column = value == null ? "" : value.trim();
        if (!COLUMNS.contains(column)) {
            throw new SQLException("Unsupported filter column: " + column);
        }
        if (!data.hasValue(parameter)) {
            throw new SQLException("Missing filter parameter: " + parameter);
        }
        Object parameterValue = data.getValue(parameter);
        if (parameterValue == null) {
            sql.appendSql(column + " IS NULL");
        } else {
            sql.appendSql(column + " = ?", parameterValue);
        }
    }
}
```

在同一个初始化回调中增加：

```java
RuleRegistry.DEFAULT.register("appEquals", new EqualsRule());
```

其中 `EqualsRule` 的导入为 `com.example.sql.EqualsRule`。调用时第二段指定参数名，第三段指定列：

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(searchName)<%
    SELECT id, name FROM people WHERE @{appEquals, searchName, name}
%>;
return find('Alice');
```

生成 `WHERE name = ?`，参数为 `["Alice"]`；传入 null 时生成 `WHERE name IS NULL`，没有绑定参数。参数值中的引号仍作为数据传给 JDBC。

## 接口与解析约定

- `test(data, context, expression)`：决定是否执行。`expression` 的解释方式由规则自己定义，不会自动按 OGNL 求值。
- `executeRule(data, context, sql, expression, value)`：写入 SQL 和参数，可抛出 `SQLException`。
- `SqlArgSource`：读取当前片段参数。示例直接按名称读取；需要复杂表达式时，可使用 `OgnlUtils.evalOgnl(expression, data)`。
- `QueryContext`：查找规则、公共 SQL 片段、类型处理器和类加载器。
- `SqlBuilder.appendSql(text, args...)`：追加 SQL 与参数；SQL 中的 `?` 应与追加参数一一对应。

解析器把第一个顶层逗号之前作为规则名，第二个之前作为表达式，余下内容作为 `value`。引号和嵌套规则内部的逗号不作为分段边界。例如 `@{appEquals, searchName, name}` 得到表达式 `searchName` 和内容 `name`。

要让自定义规则继续解析内部规则和参数，可在 `executeRule` 中调用：

```java
DynamicParsed.getParsedSql(value).buildQuery(data, context, sql);
```

`DynamicParsed` 的导入为 `net.hasor.dataql.sqlproc.dynamic.segment.DynamicParsed`。直接调用 `appendSql(value)` 只追加文字，不会再次解析；两种行为应按规则用途选择。组合方式见[规则嵌套](../../dataql/sql/rules/nesting.md)。

---
id: index
slug: /dataql/sql
title: 6. SQL 执行器
---

SQL 执行器是 DataQL 的扩展模块，通过代码片段执行数据库语句。脚本声明 SQL 和参数，再像函数一样调用片段；查询结果可以继续参与计算和转换。

:::info 使用条件
应用需要接入 `dataql-sqlproc` 并提供数据库连接，配置见 [DataQL 引擎：SQL 执行器](../../dataway/dataql-engine/sql.md)。本章介绍脚本用法。
:::

```js
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(minAge)<%
    SELECT name, age FROM people WHERE age >= #{minAge} ORDER BY id
%>;
return find(25) => [{"name", "nextAge": age + 1}];
```

示例使用 [SQL 执行](execute.md#示例数据)中的 `people` 表，返回 `[{"name":"Alice","nextAge":26},{"name":"Bob","nextAge":31}]`。

## 使用指引

- [SQL 执行](execute.md)：选择片段入口，执行查询、增删改和批量操作。
- [参数绑定](parameters.md)：传递值、指定 JDBC 类型和使用文本替换。
- [动态规则](rules.md)、[XML 动态 SQL](mybaits.md)：按条件构造 SQL。
- [结果与主键](results.md)、[分页与方言](dialect.md)：读取查询结果和分页数据。
- [类型处理](types.md)：选择参数和结果的类型转换方式。
- [事务](transactions.md)：将多次调用放在同一事务中。
- [存储过程与多结果](procedures.md)：调用存储过程并读取输出。
- [SQL Hint](../hints/hint_sql.md)：查阅全部 SQL 执行选项。

新增规则、方言和处理器见[引擎扩展](../../dataway/engine/index.md)。

---
id: rule-binding
title: 6.2.4 规则传参
---

`@{规则名, ...}` 在生成 SQL 时执行，用于选填条件、集合展开和参数预处理。规则可以同时输出 SQL 文本和绑定参数。

## 选填筛选条件

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT name, age FROM people WHERE enabled = 1
    @{and, name = #{name}}
    @{ifand, minAge != null, age >= #{minAge}}
    ORDER BY id
%>;
return find(null, 30);
```

使用 [people 表](../execute.md#示例数据)，生成：

```sql
SELECT name, age FROM people WHERE enabled = 1 AND age >= ? ORDER BY id
```

绑定 `[30]`，返回 `[{"name":"Bob","age":30}]`。`and` 省略只有 null 参数的条件；`ifand` 按显式表达式决定是否加入条件。

## 集合展开

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(ids)<%
    SELECT name FROM people WHERE 1 = 1
    @{in, AND id IN #{ids}}
    ORDER BY id
%>;
return find([1,2]);
```

生成 `AND id IN (?, ?)`，绑定 `[1,2]`。与直接 `#{ids}` 绑定一个 JDBC 数组相比，`in` 为每个元素建立独立占位符。

空集合或 null 会省略整个 `in` 规则内容，上述查询因此不再按 ID 筛选。需要空集合返回空结果时，显式生成 `AND 1 = 0`，完整写法见[语句生成规则](../rules/statements.md#in)。

## 选择规则

- 选填条件：`and`、`or`；需要明确判断时使用 `ifand`、`ifor`。
- 动态更新：`set`、`ifset`。
- 集合条件：`in`、`ifin`；复杂多行模板可使用 `pairs` 或 XML `foreach`。
- 分支：`if`、`case`、`when`、`else`。
- 参数预处理：`arg`、`md5`、`uuid32`、`uuid36`。
- 固定文本或公共片段：`text`、`iftext`、`macro`、`ifmacro`。

完整语法、空值行为和生成结果见[动态规则](../rules.md)。规则名不区分大小写；字符串和 SQL 注释中的规则标记保持原文。

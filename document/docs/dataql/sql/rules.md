---
id: rules
title: 6.3 动态规则
---

动态规则根据参数生成 SQL 条件、展开集合或生成绑定值。规则写在 SQL 片段内，适用于 `selectSql`、`insertSql`、`updateSql`、`deleteSql` 和 `executeSql`。

## 从一个查询开始

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT id, name FROM people WHERE 1 = 1
    @{ifand, name != null and name != '', name = #{name}}
    @{ifand, minAge != null, age >= #{minAge}}
    ORDER BY id
%>;
return find(null, 30);
```

`name` 为 null，姓名条件被省略；`minAge` 为 30，生成 `AND age >= ?`，并通过 JDBC 绑定 30。示例数据见 [SQL 执行](execute.md)。在 Dataway 中创建 SQL 类型的 API 时，直接填写片段内的 SQL，参数由请求提供。

## 语法

```sql
@{规则名, 内容}
@{规则名, 条件表达式, 内容}
```

规则名不区分大小写。条件表达式使用 OGNL，直接写参数名，例如 `age >= 18`；SQL 中的值使用 `#{age}` 绑定。`@{...}` 是 SQL 片段内部语法，DataQL 的函数、表达式仍按 DataQL 语法书写。

规则内容可以嵌套其他规则。SQL 单引号、双引号和注释内的标记按普通文本保留，不执行规则。

## 使用指引

- [语句生成规则](rules/statements.md)：条件拼接、集合展开、分支、片段引用及参数生成。
- [规则嵌套](rules/nesting.md)：组合规则，理解条件判断与内部参数的执行顺序。
- [自定义 SQL 规则](../../dataway/engine/sql-rules.md)：实现 `SqlRule`，并在 Dataway 中注册。
- [XML 动态 SQL](mybaits.md)：使用标签组织较长的条件和循环。

## 查询结果处理

动态规则在 SQL 执行前生成语句。查询结果通过 [结果与主键](results.md)中的拆包、列名转换和 `bindOut` 设置处理，存储过程见 [存储过程与多结果](procedures.md)。SQL 执行器未注册 dbVisitor 的 `resultSet`、`resultUpdate`、`defaultResult` 规则，不能将这些规则直接写入片段。

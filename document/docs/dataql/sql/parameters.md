---
id: parameters
title: 6.2 参数绑定
---

SQL 片段声明形参，调用时传入实参。执行器根据参数生成 SQL，再通过 JDBC 绑定值。参数既可用于查询条件，也可用于插入、更新、存储过程及原生命令。

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT id, name, age FROM people
    WHERE name = #{name} AND age >= :minAge
%>;
return find('Alice', 20);
```

示例使用 [people 表](execute.md#示例数据)。生成 `WHERE name = ? AND age >= ?`，绑定参数 `["Alice",20]`，结果为 `[{"id":1,"name":"Alice","age":25}]`。

## 选择传参方式

| 形式 | 用途 | 示例 |
| --- | --- | --- |
| `?` | 按出现顺序绑定 `arg0`、`arg1` 等形参 | `WHERE id = ?` |
| `#{表达式}` | 绑定值，可附加参数选项 | `#{name, jdbcType=VARCHAR}` |
| `:表达式`、`&表达式` | 简短的名称参数 | `:filter.minAge` |
| `${表达式}` | 将文本直接拼入 SQL 结构 | `ORDER BY ${column}` |
| `@{规则名, ...}` | 按条件生成 SQL 和参数 | `@{in, id IN #{ids}}` |

业务值使用绑定参数；表名、列名等 SQL 结构可使用来自固定集合的文本替换。规则和 XML 标签可以组合这些传参方式。

## 使用指引

- [位置参数](parameters/position.md)：形参命名、绑定顺序和重复值。
- [名称参数](parameters/named.md)：对象属性、列表下标、模糊查询和空值。
- [SQL 文本替换](parameters/injection.md)：动态列名、排序方向及值绑定的区别。
- [规则传参](parameters/rule-binding.md)：用规则处理选填条件和集合展开。
- [参数选项](parameter-options.md)：JDBC 类型、类型处理器和输出参数。
- [参数符号转义](parameter-escape.md)：保留命令本身的 `?`、`:`、`&`。

## 从 API 参数传入

```javascript
var find = @@selectSql(id)<%
    SELECT name FROM people WHERE id = #{id}
%>;
return find(${id});
```

片段外的 `${id}` 读取本次 DataQL 调用参数，`find(...)` 将它传入 SQL 片段；片段内的 `#{id}` 绑定该形参。片段内 `${id}` 则是 SQL 文本替换。SQL 正文中的表达式使用 OGNL，与片段外的 DataQL 表达式分别求值。

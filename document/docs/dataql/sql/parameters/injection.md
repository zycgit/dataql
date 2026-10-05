---
id: injection
title: 6.2.3 SQL 文本替换
---

`${表达式}` 将表达式结果转为文本并拼入 SQL，用于表名、列名、排序方向等不能通过 JDBC 参数绑定的结构。

## 动态排序

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(column, direction, minAge)<%
    SELECT id, name, age FROM people
    WHERE age >= #{minAge} ORDER BY ${column} ${direction}
%>;
return find('age', 'DESC', 20);
```

使用 [people 表](../execute.md#示例数据)，生成：

```sql
SELECT id, name, age FROM people WHERE age >= ? ORDER BY age DESC
```

只有 `minAge` 被绑定，参数为 `[20]`；结果按 Bob、Alice 顺序返回。`column`、`direction` 由脚本固定值提供，也可由应用从允许的列名和方向中选择。

## 与参数绑定的区别

| 写法 | 生成 SQL | 参数 |
| --- | --- | --- |
| `name = #{name}`，值为 `Alice` | `name = ?` | `["Alice"]` |
| `ORDER BY ${column}`，值为 `age` | `ORDER BY age` | 无 |
| `ORDER BY #{column}`，值为 `age` | `ORDER BY ?` | `["age"]`，绑定的是值，不是列名 |

文本替换不加引号、不做 SQL 转义，也不检查表名或列名。SQL 结构应来自应用允许的固定集合，用户输入的业务值使用绑定参数。

## 动态表名

```javascript
var count = @@selectSql(tableName, minAge)<%
    SELECT count(*) FROM ${tableName} WHERE age >= #{minAge}
%>;
return count('people', 25);
```

生成 `SELECT count(*) FROM people WHERE age >= ?`，绑定 `[25]`，结果为 `2`。表名为 null 时，文本替换会写出文本 `null`，不会省略该 SQL 片段；应在调用前确定合法名称。

## 固定文本和公共片段

- 只有少量固定分支时，可用 `case` 或 `iftext` 规则选择固定 SQL。
- 多处复用相同 SQL 时，用 [SQL 片段](../../../dataway/engine/sql-macros.md)注册名称，再通过 `macro` 或 `include` 引用。
- 原生命令的 `?`、`:`、`&` 用[符号转义](../parameter-escape.md)保留，无需将整段命令拆成文本替换。

上述 `${...}` 位于 SQL 正文。片段外的 `${name}` 是 DataQL 参数访问，见[参数绑定概览](../parameters.md#从-api-参数传入)。

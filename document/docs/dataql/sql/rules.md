---
id: rules
title: 6.3 动态规则
---

文本 SQL 使用 `@{规则名, 内容}` 构建动态语句；条件规则使用 `@{规则名, 条件表达式, 内容}`。条件表达式基于 OGNL，参数绑定仍使用 `#{...}`。规则名不区分大小写，SQL 字符串和注释中的规则标记按普通文本保留。

## 条件筛选

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT name FROM people WHERE 1 = 1
    @{ifand, name != null and name != '', name = #{name}}
    @{ifand, minAge != null, age >= #{minAge}}
    ORDER BY id
%>;
return find(null, 30);
```

示例跳过空姓名条件，生成 `AND age >= ?`，返回 Bob。用 `ifand`、`ifor` 明确写出空值判断，避免依赖隐式的空值过滤。

## 集合展开

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(ids)<%
    SELECT name FROM people WHERE 1 = 1 @{in, AND id IN #{ids}} ORDER BY id
%>;
return find([1,2]);
```

`in` 将集合展开为多个 `?`。空集合或 null 会省略整个规则内容；查询前应明确空集合的业务含义，必要时直接返回空结果，避免扩大查询范围。

## 内置规则

| 规则 | 用途 |
| --- | --- |
| `if` | 条件成立时输出 SQL 片段 |
| `ifand`、`ifor`、`ifset` | 条件成立时补上 `AND`、`OR` 或赋值逗号 |
| `in`、`ifin` | 展开一个集合参数，可附加条件 |
| `text`、`iftext` | 原样输出 SQL 文本，可附加条件 |
| `macro`、`ifmacro` | 引入已注册的公共 SQL 片段 |
| `and`、`or`、`set` | 为包含单个绑定参数的内容补分隔符 |
| `arg` | 创建带 JDBC 选项的参数 |
| `uuid32`、`uuid36` | 生成 UUID 参数 |

`and`、`or`、`set` 对绑定参数数量有限制；复杂条件使用条件规则或 [XML 动态 SQL](mybaits.md)。`text` 输出不经过 JDBC 参数绑定，只应用于可信内容。扩展方式见[SQL 规则](../../dataway/engine/sql-rules.md)和[SQL 片段](../../dataway/engine/sql-macros.md)。

`md5` 虽已注册，当前计算的是参数包装对象文本的摘要。业务值的摘要应在脚本或应用中预先计算，再绑定到 SQL。

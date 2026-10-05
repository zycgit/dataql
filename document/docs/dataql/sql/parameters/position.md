---
id: position
title: 6.2.1 位置参数
---

SQL 中的 `?` 表示一个绑定位置，从左到右依次读取片段形参 `arg0`、`arg1` 等。以下示例使用 [people 表](../execute.md#示例数据)。

## 按顺序绑定

```javascript
var find = @@selectSql(arg0, arg1)<%
    SELECT name FROM people WHERE age >= ? AND age < ?
%>;
return find(20, 30);
```

生成的 SQL 仍有两个 `?`，参数依次为 `[20,30]`，结果为 `Alice`。形参名也应使用 `arg0`、`arg1`；命名为 `minAge`、`maxAge` 时应改用 `#{minAge}`、`#{maxAge}`。

## 重复使用一个值

每个 `?` 是独立位置。以下两处均需传入值：

```javascript
var find = @@selectSql(arg0, arg1)<%
    SELECT count(*) FROM people WHERE age >= ? AND age <= ?
%>;
return find(25, 25);
```

绑定 `[25,25]`，返回 `1`。同一个值需要多次引用时，[名称参数](named.md)可以直接重复写 `#{age}`。

## 为位置参数增加选项

`?` 本身不能附加选项，改为引用同名形参即可：

```javascript
var find = @@selectSql(arg0)<%
    SELECT name FROM people WHERE id = #{arg0, jdbcType=BIGINT}
%>;
return find(1);
```

结果为 `Alice`。`jdbcType` 和 `typeHandler` 的取值见[参数选项](../parameter-options.md)。

## 使用边界

- 引号、SQL 注释中的 `?` 保持字面文本，不绑定参数。
- 一个 `?` 只绑定一个值；整个列表作为一个 JDBC 数组传入。查询多个 ID 使用 `in` 规则或 `foreach`。
- 动态规则、标签和公共片段分别解析 SQL；这些片段内部使用名称参数，可以避免位置编号与动态分支相互影响。
- 原生命令中的字面问号用 `\?`，见[参数符号转义](../parameter-escape.md)。

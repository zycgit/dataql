---
id: fragment
title: 3.4 代码片段
description: DataQL 代码片段的语法与用法。
---

代码片段将其他语言的内容交给已注册的执行器处理。DataQL 负责传递参数、调用执行器和转换返回结果。

## 定义与调用

语法为 `@@name(arg1, arg2)<% 内容 %>`，定义后得到一个函数，调用时才执行片段。`name` 是执行器的注册名；`<%` 与 `%>` 之间的文本原样传给执行器。

以下示例需要引入 SQL 扩展并配置数据源：

```js
var findPeople = @@selectSql(minAge)<%
    SELECT name, age FROM people WHERE age >= #{minAge}
%>;
return findPeople(18);
```

`#{minAge}` 属于 SQL 执行器的参数语法。执行器注册与实现见[片段执行器](../../dataway/engine/fragments.md)，数据库配置见[数据源接入](../../dataway/capabilities/datasources.md)。

## 参数表达式

片段参数可以指定表达式，表达式在每次调用时求值，可以使用其他参数和外层变量。指定表达式的参数使用表达式结果，同位置传入的实参不会替换它。

```js
var offset = 1;
var findPeople = @@selectSql(minAge, threshold = minAge + offset)<%
    SELECT name, age FROM people WHERE age >= #{threshold}
%>;
return findPeople(18);
```

本例实际使用 `threshold = 19`。未指定表达式的参数按调用位置取值，缺少时为 `null`。

## 批量调用

`@@name[](...)` 表示批量执行。每个参数必须是列表，所有参数列表长度必须一致；相同下标的值组成一次调用，结果按顺序组成列表。

```js
var findPeople = @@selectSql[](minAge)<%
    SELECT name, age FROM people WHERE age >= #{minAge}
%>;
return findPeople([18, 30]);
```

本例分别执行 `minAge = 18` 和 `minAge = 30` 的查询。空参数列表产生空结果列表。片段文本中的 `%>` 会结束片段定义，不能直接作为片段正文使用。

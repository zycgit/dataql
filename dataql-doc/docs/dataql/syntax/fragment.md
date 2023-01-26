---
id: fragment
sidebar_position: 11
title: k.代码片段
description: DataQL 查询中使用其它语言，这个能力在 DataQL 被称作 代码片段。
---
# 代码片段

在查询中将其它语言片段直接写在 DataQL 中并作为函数来使用，这个能力在 DataQL 被称作：`代码片段`

注：在 DataQL 中混合其它语言一起协同处理 DataQL 查询，需要定义一个片段执行器

一个典型的场景是把 SQL 语句混合在 DataQL 查询中，利用 DataQL 对 SQL 的查询结果进行二次加工。

```js title='例如'
var dataSet = @@sql(item_code) <%
    select * from category where co_code = #{item_code}
%>

return dataSet() => [
    { "id","name","code","body" }
]
```

`@@sql` 是 FunctionX 扩展包中提供的一组片段执行器，这个片段执行器相当于让 DataQL 有能力执行数据库的 SQL 语句。

## 定义

定义一个片段执行器需要，实现 `net.hasor.dataql.FragmentProcess` 接口（更多信息请参考开发手册）并且将其注册到 DataQL 环境中：

```js title='方式一：通过 DataQL 接口'
FragmentProcess process = ...
AppContext = appContext = ...

DataQL dataQL = appContext.getInstance(DataQL.class);//获取 DataQL 接口
dataQL.addFragmentProcess("sql", process); //注册片段执行器
```

```js title='方式二：通过 QueryModule'
FragmentProcess process = ...

public class MyQueryModule implements QueryModule {
  public void loadModule(QueryApiBinder apiBinder) {
    dataQL.addFragmentProcess("sql", process); //注册片段执行器
  }
}
```

## 使用

定义一个片段执行器需要使用 `@@xxxx(arg1,arg2,arg3,...)<% ..... %>` 语法，其中：
- `xxxx` 为片段执行器注册到名称。
- `(arg1,arg2,arg3,...)` 为执行这个代码时传入的参数列表。如果不需要定义任何参数可以是 `()`
- 在 `<%` 和 `%>` 之间编写的是 目标语言的代码片段。

比如在 MySQL 中插入一条数据，并返回自增的ID：

```js
var saveData = @@sql(data) <%
    insert into my_option (
        `key`,
        `value`,
        `desc`
    ) values (
        #{data.key},
        #{data.value},
        #{data.desc}
    );
	select LAST_INSERT_ID();
%>
return saveData(${root});
```

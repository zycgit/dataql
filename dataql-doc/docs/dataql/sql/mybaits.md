---
id: MyBatis
sidebar_position: 4
title: d.MyBatis执行器
description: DataQL的 MyBatis执行器，@@MyBatis 扩展可以执行部分 MyBatis标签、DataQL 动态SQL执行器
---

# MyBatis 执行器

在 4.1.8 版本后加入了 `@@MyBatis` 执行器(非常感谢 社区小东的贡献 https://gitee.com/jmxd )，MyBatis 执行器是对 [`@@sql`](./execute.md) 执行器的一个扩展，它会继承所有 `@@sql` 的能力。
同时提供类似 MyBatis 的配置方式，并提供了动态 SQL 的能力。

对比 `@@sql` 的优势
- 继承 @@sql 全部能力
- 动态SQL能力，提供 SQL 层面的 `if` 和 `for`
- 类似 MyBatis 的工作方式，比起 DataQL 拼接字符串注入更加安全可靠。

```js title='用法'
var dimSQL = @@MyBatis(userName)<%
    <select>
        select * from user_info where `name` like concat('%',#{userName},'%') order by id asc
    </select>
%>;
```

## 可用的根标签

| 根标签      | 含义          |
|----------|-------------|
| `select` | `Select` 语句 |
| `update` | `Update` 语句 |
| `insert` | `Insert` 语句 |
| `delete` | `Delete` 语句 |

**foreach 标签**

与 MyBatis 的用法一致，用来循环拼接SQL。下面是可用的标签属性。

| 属性           | 含义         |
|--------------|------------|
| `collection` | 集合，必填项     |
| `item`       | item，必填项   |
| `open`       | 拼接起始SQL，选填 |
| `close`      | 拼接结束SQL，选填 |
| `separator`  | 分隔符，选填     |

```js
<foreach collection="userIds.split(',')" item="userId" open="(" close=")" separator=",">
    #{userId}
</foreach>
```

**if 标签**

与 MyBatis 一致,用来判断，当条件成立时拼接if标签里的内容

| 属性     | 含义       |
|--------|----------|
| `test` | 判断条件，必填项 |

```js
<if test="userId != null and userId != ''">
    and user_id = #{userId}
</if>
```

---
id: setter
sidebar_position: 8
title: h.赋值
description: 在 DataQL 中数据是不可被修改的，如要修改数据集中的数据。则需要重新生成这个数据集，或者利用结果转换在转换的过程中对局部数据进行修改。。
---
# 赋值

:::caution
在 DataQL 中数据是不可被修改的，如要修改数据集中的数据。则需要重新生成这个数据集，或者利用结果转换在转换的过程中对局部数据进行修改。
:::

```js title='重定义：在任何时候都可以通过 var 来重新定义一个已存在的变量'
var data = [0,1,2,3,4,5,6,7,8,9]
// 此时 data 是一个数组。
var data = {}
// 此时 data 是一个对象
var data = 123
// 此时 data 是一个数字
```

```js title='在重定义变量的时候，可以访问自身'
var tmpSql = "select * from user"
var tmpSql = tmpSql + " where uid = ?"
```
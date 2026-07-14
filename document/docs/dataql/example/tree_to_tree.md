---
id: tree_to_tree
sidebar_position: 1
title: Tree 到 Tree 的变换
description: DataQL 样例集锦，通过 DataQL 查询实现 Tree 节点中属性的重定义
---
# Tree 到 Tree 的变换

通过 DataQL 查询实现 Tree 节点中属性的重定义。

样本数据：[test_json_2020-01-14-11-29.json](../_files/test_json_2020-01-14-11-29.json)

```js title='DataQL 查询'
var fmt = (dat)-> {
return {
    'id' : dat.value,
    'text' : dat.text,
    'child' : dat.ChildNodes => [ fmt(#) ] }
}
return ${result} => [ fmt(#) ]
```

:::tip
`${result}` 是访问执行 DataQL 时程序传入的参数。
:::

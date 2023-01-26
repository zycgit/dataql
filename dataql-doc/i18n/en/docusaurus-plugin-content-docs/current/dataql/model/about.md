---
id: about
sidebar_position: 1
title: a.介绍
description: DataQL 数据模型、ListModel模型、数据模型介绍
---

# 介绍

DataQL 的数据模型是通过 `net.hasor.dataql.domain.DataModel` 接口表示的，共计有4个实现类。

其中：`String`、`Number`、`Boolean`、`Null` 四个类型同时使用 `ValueModel` 来表示。

由于 DataQL 在进行数据处理和转换期间所有数据操作都会在统一的数据模型上进行处理，因此 `DataModel` 也是 DataQL 的一项核心概念。

`DataModel` 是一个接口，可以表示任意 DataQL 查询的返回值。根据实际情况可以将其强制转换成下列任一类型：

| 满足条件                 | 可强转的类型        |
|----------------------|---------------|
| `isValue() == true`  | `ValueModel`  |
| `isList() == true`   | `ListModel`   |
| `isObject() == true` | `ObjectModel` |
| `isUdf() == true`    | `UdfModel`    |

还有一个非常重要的 `unwrap` 方法，这个方法是用来解除数据对象 `DataModel` 形态的封装，直接变为 `Map/List` 结构。

但是需要注意 `unwrap` 方法对于 `UdfModel` 类型解开的结果会是一个 UDF 接口。

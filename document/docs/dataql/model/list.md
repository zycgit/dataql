---
id: list
sidebar_position: 3
title: c.ListModel
description: DataQL 数据模型、ListModel模型
---

# ListModel

表示一个列表或集合的数据，相比较 `DataModel` 多了一组根据元素位置判断对应类型的接口方法。

| 满足条件                    | 表示的类型         | 获取对应值            |
|-------------------------|---------------|------------------|
| `isNull() == true`      | `Null`        | `asOri()`        |
| `isValue(int) == true`  | `ValueModel`  | `getValue(int)`  |
| `isList(int) == true`   | `ListModel`   | `getList(int)`   |
| `isObject(int) == true` | `ObjectModel` | `getObject(int)` |
| `isUdf(int) == true`    | `UdfModel`    | `getUdf(int)`    |

---
id: object
sidebar_position: 4
title: d.ObjectModel
description: DataQL 数据模型、ObjectModel模型
---

# ObjectModel

| 满足条件                    | 表示的类型         | 获取对应值            |
|-------------------------|---------------|------------------|
| `isValue(str) == true`  | `ValueModel`  | `getValue(str)`  |
| `isList(str) == true`   | `ListModel`   | `getList(str)`   |
| `isObject(str) == true` | `ObjectModel` | `getObject(str)` |
| `isUdf(str) == true`    | `UdfModel`    | `getUdf(str)`    |

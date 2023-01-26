---
id: group_by
sidebar_position: 7
title: 集合分组
description: DataQL 样例集锦，通过 DataQL 查询实现将一个集合数据按照某个条件拆分为多个数据集、集合分组
---
# 集合分组

数据集中需要有一个公共字段，根据公共字段对数据进行分组。例如：

```js title='DataQL 查询'
var dataSet = [
    {'id': 1, 'parent_id':null, 'label' : 't1'},
    {'id': 2, 'parent_id':1   , 'label' : 't2'},
    {'id': 3, 'parent_id':1   , 'label' : 't3'},
    {'id': 4, 'parent_id':2   , 'label' : 't4'},
    {'id': 5, 'parent_id':null, 'label' : 't5'}
]

return collect.groupBy(dataSet, "parent_id")
```

```js title='执行结果为'
{
  "1": [
    {'id': 2, 'parent_id':1   , 'label' : 't2'},
    {'id': 3, 'parent_id':1   , 'label' : 't3'}
  ],
  "2": [
    {'id': 4, 'parent_id':2   , 'label' : 't4'}
  ],
  "null": [
    {'id': 1, 'parent_id':null, 'label' : 't1'},
    {'id': 5, 'parent_id':null, 'label' : 't5'}
  ]
}
```
---
id: like_for
sidebar_position: 3
title: 仿for循环遍历
description: DataQL 样例集锦，通过 DataQL 查询模拟一个for循环遍历
---
# 仿for循环遍历

```js title='遍历集合，追加一个序列号字段'
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;
var items = [
    {
        "a": "a1",
        "b": "b1"
    },
    {
        "a": "a2",
        "b": "b2"
    },
    {
        "a": "a3",
        "b": "b3"
    },
    {
        "a": "a4",
        "b": "b4"
    }
]
var addIndex = (dataSet, arrObj, index) -> {
    if(index < collect.size(dataSet)){
        var data = dataSet[index] => {
            "a",
            "b",
            "seq_no": index
        }
        run arrObj.addLast(data)
        var i = index + 1
        run addIndex(dataSet, arrObj, i)
    }
    return arrObj
}

var newList = collect.newList()
return addIndex(items, newList, 0).data()
```

```js title='执行结果'
[
  {
    "a": "a1",
    "b": "b1",
    "seq_no": 0
  },
  {
    "a": "a2",
    "b": "b2",
    "seq_no": 1
  },
  {
    "a": "a3",
    "b": "b3",
    "seq_no": 2
  },
  {
    "a": "a4",
    "b": "b4",
    "seq_no": 3
  }
]
```

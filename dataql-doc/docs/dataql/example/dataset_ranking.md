---
id: dataset_ranking
sidebar_position: 2
title: 为数据集进行排名
description: DataQL 样例集锦，通过 DataQL 查询为数据集生成一个排名列
---
# 为数据集进行排名

通过 DataQL 查询为数据集生成一个排名列

```js title='DataQL 查询'
import 'net.hasor.dataql.fx.basic.StateUdfSource' as state;
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;

var yearData = [
    { "colKey":"k1", "col1":"01", "col2":"02", "col3":"03", "col4":"04", "col5":"05" },
    { "colKey":"k2", "col1":"06", "col2":"07", "col3":"08", "col4":"09", "col5":"10" },
    { "colKey":"k3", "col1":"11", "col2":"12", "col3":"13", "col4":"14", "col5":"15" },
    { "colKey":"k4", "col1":"16", "col2":"17", "col3":"18", "col4":"19", "col5":"20" },
    { "colKey":"k5", "col1":"21", "col2":"22", "col3":"23", "col4":"24", "col5":"25" }
];

var stateNum = state.decNumber(0)
var addId = (row) -> {
    var dtmpData = collect.newMap();
    run dtmpData.put('id',stateNum());
    run dtmpData.putAll(row);
    return dtmpData.data()
}

return yearData => [
    addId(#)
]
```

```js title='执行结果'
[
  {"id": 1,"colKey": "k1","col1": "01","col2": "02","col3": "03","col4": "04","col5": "05"},
  {"id": 2,"colKey": "k2","col1": "06","col2": "07","col3": "08","col4": "09","col5": "10"},
  {"id": 3,"colKey": "k3","col1": "11","col2": "12","col3": "13","col4": "14","col5": "15"},
  {"id": 4,"colKey": "k4","col1": "16","col2": "17","col3": "18","col4": "19","col5": "20"},
  {"id": 5,"colKey": "k5","col1": "21","col2": "22","col3": "23","col4": "24","col5": "25"}
]
```

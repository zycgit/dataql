---
id: row_to_col
sidebar_position: 5
title: 行列转换（行数量任意）
description: DataQL 样例集锦，通过 DataQL 查询实现将一个数据集进行 90度旋转
---
# 行列转换（行数量任意）

下面这个 Case 中使用了比较复杂的 DataQL 处理逻辑，但实现了 任意行下的行列转换。

假如有 100行，每行有 4 列记录，在通过下面 QL 脚本执行后会变成 有 4 行记录，但是每行有 100 列。

（相当于把原始数据当成一个二维矩阵做了一个 90度的旋转）

```js
// 实现思路类似程序中的 双层 for 循环
//
// col1, col2, col3, col4, col5 ,colKey
// 01    02    03    04    05   | k1
// 06    07    08    09    10   | k2
// 11    12    13    14    15   | k3
// 16    17    18    19    20   | k4
// 21    22    23    24    25   | k5
//
// colKey, k1, k2, k3, k4, k5
// col1    01  06  11  16  21
// col2    02  07  12  17  22
// col3    03  08  13  18  23
// col4    04  09  14  19  24
// col5    05  10  15  20  25
//
//
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;
//
var yearData = [
    { "colKey":"k1", "col1":"01", "col2":"02", "col3":"03", "col4":"04", "col5":"05" },
    { "colKey":"k2", "col1":"06", "col2":"07", "col3":"08", "col4":"09", "col5":"10" },
    { "colKey":"k3", "col1":"11", "col2":"12", "col3":"13", "col4":"14", "col5":"15" },
    { "colKey":"k4", "col1":"16", "col2":"17", "col3":"18", "col4":"19", "col5":"20" },
    { "colKey":"k5", "col1":"21", "col2":"22", "col3":"23", "col4":"24", "col5":"25" }
];
//
//
var keySet1 = collect.mapKeys(yearData[0]);
var keySet2 = yearData => [ colKey ]
//
var data = collect.list2map(yearData, (idx,dat)-> {
    return dat.colKey;
},(idx,dat) -> {
    return dat
});
//
var lambdaFoo = (row) -> {
    var entList = keySet2 => [
        {
            "key" : #,
            "val" : data[#][row]
        }
    ]
    var entList = collect.list2map(entList, (idx,dat)-> {
        return dat.key;
    },(idx,dat) -> {
        return dat.val
    });
    // return entList;
    //
    var tmpData = collect.mapKeyReplace({"key" : row}, (oldKey,value) -> {
        if (oldKey == 'key') {
            return value
        } else {
            return oldKey
        }
    });
    var tmpData = collect.mapValueReplace(tmpData, (oldKey,value) -> {
        return entList
    });
    return tmpData
}
var dat = keySet1 => [
    lambdaFoo(#)
]
//
var tempDat = collect.newMap();
run dat => [
    tempDat.putAll(#)
]
var dat = tempDat.data();
//
var dat = collect.map2list(dat, (key,value) -> {
    var tempDat = collect.newMap({ "colKey" : key});
    run tempDat.putAll(value);
    return tempDat.data();
});


return {
    "oldData" : yearData,
    "newData" : dat
}
```

```js title='查询结果'
{
  "success": true,
  "message": "OK", 
  "code":0,
  "lifeCycleTime":59,
  "executionTime":22,
  "value":{
    "oldData":[
      {"colKey":"k1", "col1":"01", "col2":"02", "col3":"03", "col4":"04", "col5":"05"},
      {"colKey":"k2", "col1":"06", "col2":"07", "col3":"08", "col4":"09", "col5":"10"},
      {"colKey":"k3", "col1":"11", "col2":"12", "col3":"13", "col4":"14", "col5":"15"},
      {"colKey":"k4", "col1":"16", "col2":"17", "col3":"18", "col4":"19", "col5":"20"},
      {"colKey":"k5", "col1":"21", "col2":"22", "col3":"23", "col4":"24", "col5":"25"}
    ],
    "newData":[
      {"colKey":"colKey", "k1":"k1", "k2":"k2", "k3":"k3", "k4":"k4", "k5":"k5"},
      {"colKey":  "col1", "k1":"01", "k2":"06", "k3":"11", "k4":"16", "k5":"21"},
      {"colKey":  "col2", "k1":"02", "k2":"07", "k3":"12", "k4":"17", "k5":"22"},
      {"colKey":  "col3", "k1":"03", "k2":"08", "k3":"13", "k4":"18", "k5":"23"},
      {"colKey":  "col4", "k1":"04", "k2":"09", "k3":"14", "k4":"19", "k5":"24"},
      {"colKey":  "col5", "k1":"05", "k2":"10", "k3":"15", "k4":"20", "k5":"25"}
    ]
  }
}
```

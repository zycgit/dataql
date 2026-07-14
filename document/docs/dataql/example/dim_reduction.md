---
id: dim_reduction
sidebar_position: 8
title: 高维数组降维
description: DataQL 样例集锦，通过 DataQL 查询将一个高维数组降维降级为一维数组
---
# 高维数组降维

通过 DataQL 查询将一个高维数组降维降级为一维数组

```js title='DataQL 查询'
// 递归：利用有状态集合，把一个多维数组打平成为一维数组
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;
var data = [
    [1,2,3,[4,5]],
    [6,7,8,9,0]
]
var foo = (dat, arrayObj) -> {
    var tmpArray = dat => [ # ];  // 符号 '#' 相当于在循环 dat 数组期间的，当前元素。
    if (tmpArray[0] == dat) {
        run arrayObj.addLast(dat);// 末级元素直接加到最终的集合中，否则就继续遍历集合
    } else {
        run tmpArray => [ foo(#,arrayObj) ];
    }
    return arrayObj;
}
return foo(data,collect.newList()).data();
```

```js title='执行结果为'
[ 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 ]
```

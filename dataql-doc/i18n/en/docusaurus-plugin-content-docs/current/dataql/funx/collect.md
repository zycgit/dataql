---
id: collect
sidebar_position: 2
title: b.集合函数库
description: DataQL FunctionX库函数，集合函数库
---
# 集合函数库

引入集合函数库的方式为：`import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;`

## isEmpty
函数定义：`boolean isEmpty(target)`
- 参数定义：`target` 类型：`List/Map`
- 返回类型：`boolean`
- 作用：判断集合或对象是否为空。

说明：
- `ObjectModel` 等价于 `Map`
- `ListModel` 等价于 `List`
- `数组` 等价于 `List`

如果是一个空 `Map` 那么返回 `true`，否则返回 `false`
如果是一个空 `List` 那么返回 `true`，否则返回 `false`

```js title='例子'
collect.isEmpty([])          = true  // 空集合
collect.isEmpty({})          = true  // 空对象
collect.isEmpty([0,1,2])     = false // 集合不为空
collect.isEmpty({'key':123}) = false // 对象含有至少一个属性
collect.isEmpty(null)        = false // 不支持的基本类型会返回 false
```

## size
函数定义：`int size(target)`
- 参数定义：`target` 类型：`任意`
- 返回类型：`int`
- 作用：返回对象或数组的长度。

说明：该函数需要在 `4.1.10` 版本及其后续版本中才可以使用。
- 如果是 `空`：返回 `0`
- 如果是 `Map`：返回 `字段数量`
- 如果是 `数组`：返回 `数组长度`

```js title='例子'
collect.size(null)   = 0
collect.size([]),    = 0
collect.size([null]) = 1
collect.size({}),    = 0
collect.size({"key1":1, "key2":2, "key3":3 }) = 3
```

## merge
函数定义：`List merge(target_1, target_2, target_3, ..., target_n)`
- 参数定义：`target` 类型：`任意`
- 返回类型：`List`
- 作用：将多个 `List` 入参合并成一个集合，或者将多个对象合并成一个集合。

```js title='例子'
collect.merge([0], [1,2], [3,4,5]) = [0,1,2,3,4,5] // 将三个集合合并成一个集合。
collect.merge(0,1,2,3,4) = [0,1,2,3,4]             // 将多个对象合并成一个集合
collect.merge([0,1,2], 3, 4, 5) = [0,1,2,3,4,5]    // 集合和对象混合存在会自动归并。
```

## mergeMap
函数定义：`Map mergeMap(target_1, target_2, target_3, ..., target_n)`
- 参数定义：`target` 类型：`Map`
- 返回类型：`Map`
- 作用：合并多个对象合成为一个新的对象，当 `Key` 冲突会覆盖老数据。

说明：
- 将多个对象合并成一个对象。或者将多个对象合并成一个集合。
- 如果入参不是 `Map/ObjectModel`，会引发错误，并终止后续查询的执行。

```js title='例子'
var data1 = {"key1":1, "key2":2, "key3":3 }

var data2 = {"key4":4, "key5":5, "key3":6 }
var result = collect.mergeMap(data1,data2)
// result = { "key1":1, "key2":2, "key3":6, "key4":4, "key5":5} // 合并两个Map，由于key3冲突，后面的会覆盖前面的。

collect.mergeMap(data1,data2,[]) // throw "all args must be Map."
```

## filter
函数定义：`List filter(dataList, filterUDF)`
- 参数定义：`dataList` 类型：`List`，待过滤的原始数据； `filterUDF` 类型：`UDF/Lambda`，过滤的规则函数；
- 返回类型：`List`
- 作用：根据规则函数来对集合进行过滤。

```js title='例子'
var dataList = [
    {"name" : "马一" , "age" : 18 },
    {"name" : "马二" , "age" : 28 },
    {"name" : "马三" , "age" : 30 },
    {"name" : "马四" , "age" : 25 }
]

var result = collect.filter(dataList, (dat) -> {
    return dat.age > 20;
});

// result = [
//     {"name" : "马二" , "age" : 28 },
//     {"name" : "马三" , "age" : 30 },
//     {"name" : "马四" , "age" : 25 }
// ]
```

## filterMap
函数定义：`Map filterMap(dataMap, keyFilterUDF)`
- 参数定义：`dataMap` 类型：`Map`，待过滤的原始数据； `keyFilterUDF` 类型：`UDF/Lambda`，过滤 `Key` 的规则函数；
- 返回类型：`Map`
- 作用：根据规则函数来对 `Map` 进行过滤。

```js title='例子'
var dataMap = {
    "key1" : "马一",
    "key2" : "马二",
    "key3" : "马三",
    "key4" : "马四"
}

var result = collect.filterMap(dataMap, (key) -> {
    return key == 'key1' || key == 'key3' || key == 'key5'
});

// result = { "key1": "马一", "key3": "马三" }
```

## limit
函数定义：`List limit(dataList, start, limit)`
- 参数定义：`dataList` 类型：`List`，原始数据；`start` 类型：`Integer`，截取的起始位置； `limit` 类型：`Integer`，截取长度；
- 返回类型：`List`
- 作用：截取 `List` 的一部分，返回一个集合。

```js title='例子'
var dataList = [0,1,2,3,4,5,6,7,8,9]
var result = collect.limit(dataList, 3,4);

// result = [3,4,5,6] -> start从0开始
var result = collect.limit(dataList, 3,0);

// result = [3,4,5,6,7,8,9] -> limit 小于等于0表示全部
```

# newList
函数定义：`Map newList(target)`
- 参数定义：`target` 类型：任意，初始化数据或集合；
- 返回类型：`Map`
- 作用：创建一个带有状态的 `List`。

说明：
- 带有状态的 `List` ，类似于 `ArrayList` 对象。
- 提供三个子方法来使用：`addFirst(target)`、`addLast(target)`、`data()`、`size()`
- 提示：由于 DataQL 只能表示无状态的数据，并不能表示有状态的对象。因此为了表示一个带有状态的对象，通常是创建一组 UDF，这些 UDF 内部共享同一个对象。

```js title='例子'
// 多维数组打平成为一纬
var data = [
    [1,2,3,[4,5]],
    [6,7,8,9,0]
]

var foo = (dat, arrayObj) -> {
    // 无论 dat 是什么都将其转换为数组（符号 '#' 相当于在循环 dat 数组期间的当前元素）
    var tmpArray = dat => [ # ];
    // 如果 dat 是最终元素，在将其转换为 List 的时会作为第一个元素存在。这里判断可以断言dat是末级元素。
    if (tmpArray[0] == dat) {
        run arrayObj.addLast(dat); // 末级元素直接加到最终的集合中，否则就继续遍历集合
    } else {
        run tmpArray => [ foo(#,arrayObj) ]; // 继续递归遍历，直至末级。
    }
    return arrayObj;
}

var newList = collect.newList();
var result = foo(data, newList).data();

// result = [1,2,3,5,6,7,8,9,0]
```

## newMap
函数定义：`Map newMap(target)`
- 参数定义：`target` 类型：任意，初始化数据或集合；
- 返回类型：`Map`
- 作用：创建一个带有状态的 `List`。

说明：
- 该函数需要在 `4.1.10` 版本及其后续版本中才可以使用。
- 带有状态的 `Map`，类似于 `LinkedHashMap` 对象。
- 提供三个子方法来使用：`put(target)`、`putAll(target)`、`data()`、`size()`
- 提示：由于 DataQL 只能表示无状态的数据，并不能表示有状态的对象。因此为了表示一个带有状态的对象，通常是创建一组UDF，这些 UDF 内部共享同一个对象。

```js title='例子'
var mapData = collect.newMap({'key':123 });
// 调用 sss.data() 的结果是
// {
//     "key": 123
// }

var mapData = mapData.put('sss','sss')
// 调用 sss.data() 的结果是
// {
//     "key": 123,
//     "sss": "sss"
// }

var mapData = mapData.putAll({'id':1, 'parent_id':null, 'label': 't1'})
// 调用 sss.data() 的结果是
// {
//     "key": 123,
//     "sss": "sss",
//     "id": 1,
//     "parent_id": null,
//     "label": "t1"
// }
```

## mapJoin
函数定义：`List mapJoin(data_1, data_2, joinMapping)`
- 参数定义：`data_1` 类型：`List`，左表数据；`data_2` 类型：`List`，右表数据；`joinMapping` 类型：`Map`，两表的 `join` 关系；
- 返回类型：`List`
- 作用：将两个 `Map/List` 进行左链接，行为和 `sql` 中的 `left join` 相同。目前 `mapJoin` 函数只支持一个连接条件。

```js title='左连接形式，连接两个数据集'
var year2019 = [
    { "pt":2019, "item_code":"code_1", "sum_price":2234 },
    { "pt":2019, "item_code":"code_2", "sum_price":234 },
    { "pt":2019, "item_code":"code_3", "sum_price":12340 },
    { "pt":2019, "item_code":"code_4", "sum_price":2344 }
];

var year2018 = [
    { "pt":2018, "item_code":"code_1", "sum_price":1234.0 },
    { "pt":2018, "item_code":"code_2", "sum_price":1234.0 },
    { "pt":2018, "item_code":"code_3", "sum_price":1234.0 },
    { "pt":2018, "item_code":"code_4", "sum_price":1234.0 }
];

var result = collect.mapJoin(year2019,year2018, { "item_code":"item_code" }) => [
    {
        "商品Code": data1.item_code,
        "去年同期": data2.sum_price,
        "今年总额": data1.sum_price,
        "环比去年增长": ((data1.sum_price - data2.sum_price) / data2.sum_price * 100) + "%"
    }
]

// result = [
//     {"商品Code":"code_1", "去年同期":1234.0, "今年总额":2234, "环比去年增长":"81.04%"},
//     {"商品Code":"code_2", "去年同期":1234.0, "今年总额":234, "环比去年增长":"-81.04%"},
//     {"商品Code":"code_3", "去年同期":1234.0, "今年总额":12340,"环比去年增长":"900.0%"},
//     {"商品Code":"code_4", "去年同期":1234.0, "今年总额":2344, "环比去年增长":"89.95%"}
// ]
```

## mapKeyToLowerCase
函数定义：`Map mapKeyToLowerCase(dataMap)`
- 参数定义：`dataMap` 类型：`Map`，准备要转换的 `Map` 对象；
- 返回类型：`Map`
- 作用：将 `Map` 的 Key 全部转为小写，如果 Key 有冲突会产生覆盖。

```js title='例子'
var mapData = {
    "abc" : "aa",
    "ABC" : "bb",
    "test_abc" : "cc"
}

var result = collect.mapKeyToLowerCase(mapData)
// result = { "abc": "bb", "test_abc": "cc" }
```

## mapKeyToUpperCase
函数定义：`Map mapKeyToUpperCase(dataMap)`
- 参数定义：`dataMap` 类型：`Map`，准备要转换的 `Map` 对象；
- 返回类型：`Map`
- 作用：将 `Map` 的 Key 全部转为大写，如果 Key 有冲突会产生覆盖。

```js title='例子'
var mapData = {
    "abc" : "aa",
    "ABC" : "bb",
    "test_abc" : "cc"
}

var result = collect.mapKeyToUpperCase(mapData)
// result = { "ABC": "bb", "TEST_ABC": "cc" }
```

## mapKeyToHumpCase
函数定义：`Map mapKeyToHumpCase(dataMap)`
- 参数定义：`dataMap` 类型：`Map`，准备要转换的 `Map` 对象；
- 返回类型：`Map`
- 作用：将 `Map` 的 Key 中下划线做驼峰转换。

```js title='例子'
var mapData = {
    "abc" : "aa",
    "ABC" : "bb",
    "test_abc" : "cc"
}

var result = collect.mapKeyToHumpCase(mapData)
// result = { "ABC": "bb", "testAbc": "cc" }
```

## mapKeys
函数定义：`List mapKeys(dataMap)`
- 参数定义：`dataMap` 类型：`Map`，准备要提取 Keys 的 `Map` 对象；
- 返回类型：`List`
- 作用：提取 `Map` 的 Key，并返回数组。

```js title='例子'
var data = {"key1":1, "key2":2, "key3":3 };
var result = collect.mapKeys(data);
// result = [ "key1", "key2", "key3" ]
```

## mapValues
函数定义：`List mapValues(dataMap)`
- 参数定义：`dataMap` 类型：`Map`，准备要提取 Keys 的 `Map` 对象；
- 返回类型：`List`
- 作用：提取 `Map` 的 Values，并返回数组。

```js title='例子'
var data = {"key1":1, "key2":2, "key3":3 };
var result = collect.mapValues(data);
// result = [ 1, 2, 3 ]
```

## mapKeyReplace
函数定义：`Map mapKeyReplace(dataMap, replaceKey)`
- 参数定义：`dataMap` 类型：`Map`，准备要替换 Key 的 `Map` 对象；`replaceKey` 类型：`UDF`，用于生成新 Key 的函数
- 返回类型：`Map`
- 作用：提取 `Map` 的 Key。

说明：
- 循环遍历每一个 `Map` 元素，并且对 `Map` 的 Key 进行替换。
- 如果说使用 DataQL 语言来处理 key 值映射是静态方式处理的话，`mapKeyReplace` 函数的最大意义在于提供了动态的能力来决定对象的 key 值。

```js title='例子'
var data = {"key1":1, "key2":2, "key3":3 };
var result = collect.mapKeyReplace(data, (oldKey,value) -> {
    return "new_" + oldKey
});
// result = {"new_key1":1, "new_key2":2, "new_key3":3 }
```

## mapValueReplace
函数定义：`Map mapValueReplace(dataMap, replaceKey)`
- 参数定义：`dataMap` 类型：`Map`，准备要替换 Key 的 `Map` 对象；`replaceValue` 类型：`UDF`，用于生成新 Key 的函数
- 返回类型：`Map`
- 作用：提取 `Map` 的 Key。

说明：
- 循环遍历每一个 `Map` 元素，并且对 `Map` 的 Value 进行替换。
- 和 `mapKeyReplace` 函数是相同用法，不同的是 `mapKeyReplace` 专注的是 Key 动态处理。而 `mapValueReplace` 是值的动态处理。

```js title='例子'
var data = {"key1":1, "key2":2, "key3":3 };
var result = collect.mapValueReplace(data, (okdKey,value) -> {
    return {
        "new_value" : value
    }
});

// result = {
// {
//     "key1": {
//         "new_value": 1
//     },
//     "key2": {
//         "new_value": 2
//     },
//     "key3": {
//         "new_value": 3
//     }
// }
```

## list2map
函数定义：`Map list2map(listData, dataKey, convertUDF)`
- 参数定义：`listData` 类型：`List`，行转列的数据集；`dataKey` 类型：`String/UDF/Lambda`，行对象中作为 key 的字段或者提取 Key 的函数；`convertUDF` 类型：`UDF/Lambda`，行对象到列转换函数。
- 返回类型：`Map`
- 作用：`List` 转为 `Map`。

```js title='通过字符串指明Key字段'
var yearData = [
    { "pt":2018, "item_code":"code_1", "sum_price":12.0 },
    { "pt":2018, "item_code":"code_2", "sum_price":23.0 },
    { "pt":2018, "item_code":"code_3", "sum_price":34.0 },
    { "pt":2018, "item_code":"code_4", "sum_price":45.0 }
];

var result = collect.list2map(yearData, "item_code");
// result = {
//     "code_1": { "pt":2018, "item_code":"code_1", "sum_price":12.0 },
//     "code_2": { "pt":2018, "item_code":"code_2", "sum_price":23.0 },
//     "code_3": { "pt":2018, "item_code":"code_3", "sum_price":34.0 },
//     "code_4": { "pt":2018, "item_code":"code_4", "sum_price":45.0 }
// };
```

```js title='使用 Key 提取函数'
var yearData = [ 1,2,3,4,5];
var result = collect.list2map(yearData, (idx,dat)-> {
    // Key 提取函数，直接把数组的数字元素内容作为 key 返回
    return dat;
},(idx,dat) -> {
    // 构造 value
    return { "index": idx, "value": dat };
});

// result = {
//     "1": { "index": 0, "value": 1 },
//     "2": { "index": 1, "value": 2 },
//     "3": { "index": 2, "value": 3 },
//     "4": { "index": 3, "value": 4 },
//     "5": { "index": 4, "value": 5 }
// }
```

## map2list
函数定义：`List map2list(dataMap, convert)`
- 参数定义：`dataMap` 类型：`Map`，准备转换的数据集；`convert` 类型：`UDF/Lambda`，转换成行的转换器；
- 返回类型：`List`
- 作用：将 `Map` 转为 `List`。

```js title='不指定转换函数'
var data = {"key1":1, "key2":2, "key3":3 };
var result = collect.map2list(data);
// result = [
//     { "key": "key1", "value": 1},
//     { "key": "key2", "value": 2},
//     { "key": "key3", "value": 3}
// ]
```

```js title='指定转换函数'
var data = {"key1":1, "key2":2, "key3":3 };
var result = collect.map2list(data, (key,value) -> {
    return { "k" : key, "v" : value };
});
// result = [
//     { "k": "key1", "v": 1},
//     { "k": "key2", "v": 2},
//     { "k": "key3", "v": 3}
// ]
```

## map2string
函数定义：`String map2string(dataMap, joinStr, convert)`
- 参数定义：`dataMap` 类型：`Map`，准备转换的数据集；`joinStr` 类型：`String`，连接每个 `K/V` 对的连接字符串；`convert` 类型：`UDF/Lambda`，转换器；
- 返回类型：`String`
- 作用：`Map` 转为字符串，通常在生成 URL 参数的时候会用到这个函数。

```js title='例子'
var data = {"key1":1, "key2":2, "key3":3 };
var result = collect.map2string(data,"&",(key,value) -> {
     return key + "=" + value;
});
// result = "key1=1&key2=2&key3=3"
// Tips：通常在转换 URL 的时候，还会连同编码函数库的 urlEncode 函数组合使用。以处理URL参数特殊字符问题。
```

## mapSort
函数定义：`Map mapSort(dataMap, sortUdf)`
- 参数定义：`dataMap` 类型：`Map`，待处理的数据；`sortUdf` 类型：`UDF/Lambda`，排序函数返回值 `-1,0,1`；
- 返回类型：`Map`
- 作用：对 `Map` Key进行排序，DataQL 的 `Map` 都是有序 Map，因此可以利用 `mapSort` 进行 key 排序。一个典型的场景是利用 DataQL 生成一个 HMAC 签名串。

```js title='例子'
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;
import 'net.hasor.dataql.fx.basic.CompareUdfSource' as compare; // 通常排序还要引入一个排序的函数库

var data = {"key3":1, "key2":2, "key1":3 };
var result = collect.mapSort(data, (k1, k2) -> {
    return compare.compareString(k1, k2);//对 key 比大小进行排序
});
// result = {"key1": 3, "key2": 2, "key3": 1}
```

## listSort
函数定义：`List listSort(dataList, sortUdf)`
- 参数定义：`dataList` 类型：`List`，待处理的数据；`sortUdf` 类型：`UDF/Lambda`，排序函数返回值 `-1,0,1`；
- 返回类型：`List`
- 作用：对 `List` 进行排序。

```js
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;
import 'net.hasor.dataql.fx.basic.CompareUdfSource' as compare; // 通常排序还要引入一个排序的函数库

var data = [
     { "key": "key1", "value": 1},
     { "key": "key2", "value": 2},
     { "key": "key3", "value": 3}
];
var result = collect.listSort(data, (dat1, dat2) -> {
    return compare.compareString(dat1.key, dat2.key) * -1; // 按照 Key 倒序
});
// result = [
//   { "key": "key3", "value": 3},
//   { "key": "key2", "value": 2},
//   { "key": "key1", "value": 1}
// ];
```

## groupBy
函数定义：`Map<String,List> groupBy(dataList, groupByKey)`

- 参数定义：`dataList` 类型：`List`，待处理的数据；`groupByKey` 类型：`String`，要分组的字段名
- 返回类型：`Map<String,List>`
- 作用：根据公共字段对数据进行分组。

说明：
- 该函数需要在 `4.1.10` 版本及其后续版本中才可以使用。
- 数据集中需要有一个公共字段，并根据公共字段对数据进行分组。

```js title='例子'
var dataSet = [
    {'id': 1, 'parent_id':null, 'label' : 't1'},
    {'id': 2, 'parent_id':1   , 'label' : 't2'},
    {'id': 3, 'parent_id':1   , 'label' : 't3'},
    {'id': 4, 'parent_id':2   , 'label' : 't4'},
    {'id': 5, 'parent_id':null, 'label' : 't5'}
]
var result = collect.groupBy(dataSet, "parent_id")

// result = {
//   "1": [
//     {'id': 2, 'parent_id':1   , 'label' : 't2'},
//     {'id': 3, 'parent_id':1   , 'label' : 't3'}
//   ],
//   "2": [
//     {'id': 4, 'parent_id':2   , 'label' : 't4'}
//   ],
//   "null": [
//     {'id': 1, 'parent_id':null, 'label' : 't1'},
//     {'id': 5, 'parent_id':null, 'label' : 't5'}
//   ]
// }
```

## uniqueBy
函数定义：`List uniqueBy(dataList, uniqueByKey)`
- 参数定义：`dataList` 类型：`List`，待处理的数据；`uniqueByKey` 类型：`String`，去重判断的字段名
- 返回类型：`List`
- 作用：根据公共字段对数据进行去重，数据集中需要有一个公共字段，并根据公共字段对数据进行去重（4.2.2 版本加入）

```js title='例子'
var dataSet = [
    {'id': 1, 'parent_id':null, 'label' : 't1'},
    {'id': 2, 'parent_id':1   , 'label' : 't2'},
    {'id': 3, 'parent_id':1   , 'label' : 't3'},
    {'id': 4, 'parent_id':2   , 'label' : 't4'},
    {'id': 5, 'parent_id':null, 'label' : 't5'}
]
var result = collect.uniqueBy(dataSet, "parent_id")

// result = [
//   { "id": 1, "parent_id": null, "label": "t1" },
//   { "id": 2, "parent_id": 1   , "label": "t2" },
//   { "id": 4, "parent_id": 2   , "label": "t4" }
// ]
```

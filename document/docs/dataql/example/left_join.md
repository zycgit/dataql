---
id: left_join
sidebar_position: 6
title: 连接两个数据集(left join)
description: DataQL 样例集锦，通过 DataQL 查询实现将两个数据集以 left join 的形式连接到一起成为一个集合数据
---
# 连接两个数据集(left join)

有两个数据集 year2019、year2018 通过 mapJoin 的方式将两个数据联合在一起，并计算同比

```js title='DataQL 查询'
hint MAX_DECIMAL_DIGITS = 4
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;

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

// 求同比
return collect.mapJoin(year2019,year2018, { "item_code":"item_code" }) => [
    {
        "商品Code": data1.item_code,
        "去年同期": data2.sum_price,
        "今年总额": data1.sum_price,
        "环比去年增长": ((data1.sum_price - data2.sum_price) / data2.sum_price * 100) + "%"
    }
]
```

```js title='执行结果'
[
    {
        "商品Code":"code_1",
        "去年同期":1234.0,
        "今年总额":2234,
        "环比去年增长":"81.04%"
    },
    {
        "商品Code":"code_2",
        "去年同期":1234.0,
        "今年总额":234,
        "环比去年增长":"-81.04%"
    },
    {
        "商品Code":"code_3",
        "去年同期":1234.0,
        "今年总额":12340,
        "环比去年增长":"900.0%"
    },
    {
        "商品Code":"code_4",
        "去年同期":1234.0,
        "今年总额":2344,
        "环比去年增长":"89.95%"
    }
]
```

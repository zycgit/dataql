---
id: function
sidebar_position: 10
title: j.函数
description: DataQL 语言中定义函数，然后在后续查询中使用它。DataQL函数、DataQL Lambda 函数定义、DataQL 模拟 for 循环
---
# 函数

## 定义函数

在 DataQL 查询中可以直接通过 DataQL 语言本身定义一个函数，然后在后续查询中使用它。一个典型的场景就是对性别字段的转换：

```js
var convertSex = (sex) -> {
  return (sex == 'F') ? '男' : '女'
};

var data = {
  "userID" : 1234567890,
  "age"    : 31,
  "name"   : "this is name.",
  "nick"   : "my name is nick.",
  "sex"    : "F",
  "status" : true
};

return data => {
    "name",
    "age" : age + "岁",
    "sex" : convertSex(sex)
}
```

## 函数的 Lambda写法

:::tip
Lambda 写法相当于一个匿名的函数。
:::

例如在使用集合函数的过滤功能时，如果没有 Lambda 写法可能整个查询写出来会比较臃肿。比如我们有如下数据：

```js
// 原始数据
var dataList = [
    {"name" : "马一" , "age" : 18 },
    {"name" : "马二" , "age" : 28 },
    {"name" : "马三" , "age" : 30 },
    {"name" : "马四" , "age" : 25 }
]
// 过滤后的数据
// [
//     {"name" : "马二" , "age" : 28 },
//     {"name" : "马三" , "age" : 30 },
//     {"name" : "马四" , "age" : 25 }
// ]
```

只保留年龄大于20岁的数据：

```js
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;
// 数据
var dataList = ...
// 年龄过滤逻辑
var filterAge = (dat) -> {
  return return dat.age > 20;
};
// 调用 filter 函数
return collect.filter(dataList, filterAge);
```

换成 Lambda 写法可以省掉一个函数的定义：

```js
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;
// 数据
var dataList = ...
var result = collect.filter(dataList, (dat) -> { // lambda 写法
    return dat.age > 20;// 年龄过滤条件
});
```

## 通过Lambda 模拟 for 循环

```js
import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;

var map = {
    "a" : 123,
    "b" : 321
}
var data = [
    {
        "name" : "马三",
        "type" : "a"
    },
    {
        "name" : "n2",
        "type" : "b"
    }
]

var appendData = (data) -> {
    var newMap = collect.newMap(data);
    run newMap.put('type',map[data.type])
    return newMap.data()
};

return data => [
    appendData(#)
]
```

查询结果:

```js
[
    {
      "name": "马三",
      "type": 123
    },
    {
      "name": "n2",
      "type": 321
    }
]
```

## 使用外部函数库

DataQL 携带了一个官方标准函数库。里面提供了大量不同功能的函数，可以通过 `import` 语句导入然后来使用它们（FunctionX库函数）比如：通过时间函数库获取系统时间：

```js
import 'net.hasor.dataql.fx.basic.DateTimeUdfSource' as time;

return time.now();
```

或者使用 json 函数库来生成 JSON 数据：

```js
import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;

return json.toJson([0,1,2]);// "[0,1,2]"
```

解析 Json

```js
import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;

return json.fromJson("{\n\t\"key\":123\n}");// {'key':123}
```

---
id: transform
sidebar_position: 9
title: i.结果转换
description: 结果转换是 DataQL 核心能力，它的作用是让原始数据的格式进行变换。这个变换可以大体归纳为：组装、变换
---
# 结果转换

:::tip
结果转换是 DataQL 核心能力，它的作用是让原始数据的格式进行变换。这个变换可以大体归纳为如下几种：
- **组装**：凭空构造一个全新的结构，每个数据元素可以从任何变量中获取。
- **变换**：将已有数据的结构进行变换
:::

变换语法为：
- `expr => fmt`，`fmt` 有两种表达方式别为：`对象` 和 `数组`。

## 组装

**Case1**：返回一个对象数据，对象中包含一个 value 字段。value 字段的来源是 data 变量：

```js
var data = 123;
return {
  "value" : data
};
```

**Case2**：如果返回一个更加复杂的结构或者是将多个值类型组合到一个数据结构中：

```js
var userName = "马三"; // 姓名
var userAge = 23;      // 年龄

// 返回一个对象数据，将用户名称和年龄组装到一个对象中
return {
  "name" : userName,
  "age"  : userAge
};
```

**Case3**：组装数组数据：

```js
var data = 123; // 值
return [
  data
];
```

**Case4**：组装多个元素的数组：

```js
var data1 = 123; // 值1
var data2 = 456; // 值2

// 返回2个元素的数组
return [
  data1, data2
];
```

**Case5**：下面这个组装稍微复杂一点，它组装了两个字段。但是两个字段分别来自于同一个数组数据的不同元素：

```js
var data = [123, 456];
return {
  "element_0" : data[0], // 123
  "element_1" : data[1]  // 456
};
```

## 数组的变换

```js title='比如我有一个如下数据'
var data = [
  {
    "name" : "马三",
    "age"  : 23,
    "type" : 1
  },{
    "name" : "马四",
    "age"  : 23,
    "type" : 2
  }
];
```

**Case1**：希望得到一个新的数据集，数据集中只包含 name 和 age 字段：

```js
return data => [
  {
    "name", "age"
  }
];
```

**Case2**：或者新的数据集中只包含 name 和 age 字段，但同时改一下字段名。

```js
return data => [
  {
    "userName" : name, // 取 name 字段的值作为 userName
    "userAge" : age
  }
];
```

**Case3**：下面这个也比较常用，从数据集中取得所有用户名的列表。最终的数据是一个字符串数组：

```js
return data => [ name ];
```

**Case4**：将一组值类型变换成一组对象 DataQL 也可以做到：

```js
var data = ["马三", "马四"];
return data => [
  {
    "name" : # // 符号 "#" 表示在对每个元素进行转换的过程中的那个元素本身。
  }
];
```

上面的代码用 JavaScript 来表示如下：

```js
var data = ["马三", "马四"];
var arrayList = [];
for (i = 0; i < data.length; i++) {
  arrayList.push({"name": data[i]}); // 这里的 data[i] 就相当于 # 号
}
return arrayList;
```

**Case5**：如果是只想取元素的第一个值，并且将第一个元素的结构进行变换。还可以这样简写：

```js
return data => {
  "userName" : name, // 取 name 字段的值作为 userName
  "userAge" : age
};
```

**Case6**：有的时候可能会遇到多维数组，比如我们要为下列数字矩阵的每一个值都加上一个字符串前缀：

```js
var data = [
    [1,2,3],
    [4,5,6],
    [7,8,9]
]
return data => [
    # => [ // 在结果转换中对当前元素进行二次转换
        "值：" + #
    ]
]
```

```js title='查询结果'
[
    ["值：1","值：2","值：3"],
    ["值：4","值：5","值：6"],
    ["值：7","值：8","值：9"]
]
```

## 对象的变换

```js title='首先我们有一个对象'
var data = {
  "userID" : 1234567890,
  "age"    : 31,
  "name"   : "this is name.",
  "nick"   : "my name is nick.",
  "sex"    : "F",
  "status" : true
}
```

**Case1**：通过变换而非组装的方式将其转换为一个元素的数组

```js
return data => [ # ];
```

上述这种写法通常用于，需要结果必须是数组的情况。但是又不能确定返回的值一定是数组。

比如：默认情况下 @@sql 查询结果中如果只含有一个元素，那么它会返回对象而非 List/Map。（当然也可以通过 FRAGMENT_SQL_OPEN_PACKAGE hint 来控制 @@sql 返回值形态）

**Case2**：对象的变换通常都是对对象结构上的变化，例如：查询结果中将年龄和性别放入一个新的结构中返回：

```js
return data => {
    "name",
    "info" : {
        "age",
        "sex"
    }
}
```

```js title='查询结果'
{
  "name": "this is name.",
  "info": {
    "age": 31,
    "sex": "F"
  }
}
```

## 使用表达式

在对结果变换过程中 DataQL 还允许通过表达式来对字段进行重新计算，例如：

```js title='将年龄表示的数值转换为 xx岁。将性别表示的 F/M,转换为：男/女'
return data => {
    "name",
    "age" : age + "岁",
    "sex" : (sex == 'F') ? '男' : '女'
}
```

```js title='查询结果'
{
  "name": "this is name.",
  "age": "31岁",
  "sex": "男"
}
```

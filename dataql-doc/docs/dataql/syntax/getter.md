---
id: getter
sidebar_position: 7
title: g.取值
description: DataQL 取值的几种方式、DataQL对象取值、函数结果取值、数组中取值、下标取值、连续下标取值、数组下标取值、DataQL下表越界。
---
# 取值

## 对象取值

DataQL 支持面向对象中的取值方式，例如下面几个例子：

```js
var userInfo = {
  'id': 4,
  'username': '马A'
}
// 返回用户ID为4的用户名
return userInfo.username;
```

## 函数结果取值

```js
// userInfo 返回和上述例子相同结构的数据
var userInfo = userByID({'id': 4});
// 在函数返回值后直接取值
return userByID({'id': 4}).username;
```

## 数组中取值

```js
var userInfo = [
  {
    'id': 4,
    'username': '马A'
  },{
    'id': 2,
    'username': '马B'
  }
]
// 返回用户列表中第一条数据的用户名
return userList()[0].username;
```

## 下标取值

DataQL 支持类似 `JavaScript` 中类似的下标方式取值。在 DataQL 中有结构的数据只有两种：`集合` 和 `Map`。

因此带有结构的对象类型数据其就相当于一个 `Map`，可以通过下面的方式来获取属性值。

```js
var userInfo = {
  'id': 4,
  'username': '马A'
}
return userInfo['username'];
```

## 连续下标取值

```js
var userList = [
  {
    'id': 4,
    'username': '马A'
  },{
    'id': 2,
    'username': '马B'
  }
]

// 返回用户列表中第一条数据的用户名
return userList[0]['username'];
```

## 数组下标取值

对于数字形式的下标分为：`正向索引`、`反向索引` 两种。例如：有一个数据集：

```js
var list = [0,1,2,3,4,5,6,7,8,9]
```

**正向索引**：正数，从前向后数，从 `0` 开始。

```js
list[3] = 3
list[5] = 5
```

**反向索引**：负数，从后向前数，从 `1` 开始。

```js
list[-3] = 7
list[-5] = 5
```

**索引溢出**：无论正向还是反向，都会涉及到下标越界问题，这种情况称之为索引溢出。DataQL 有三种可以选择的处理行为：

| 行为       | DataQL 语句                       | 功效                                                |
|----------|---------------------------------|---------------------------------------------------|
| throw    | `hint INDEX_OVERFLOW = 'throw'` | 当遇到索引溢出情况时严格的抛出，ArrayIndexOutOfBoundsException 异常 |
| null     | `hint INDEX_OVERFLOW = 'null'`  | 当遇到索引溢出情况时返回 null                                 |
| near(默认) | `hint INDEX_OVERFLOW = 'near'`  | 正向索引溢出 `list[100]` 取最后一个，反向索引溢出 `list[-100]` 取第一个 |

## 下标变量

DataQL 允许使用变量替代下标中的值，这使得下标取值可以更加灵活。

```js
var userInfo = {
  'id': 4,
  'username': '马A'
}

// 定义一个变量，变量表示要取值的字段名。
var columnKey = 'username';
// 通过下标变量方式来取值
return userInfo[columnKey];
```

---
id: statements
sidebar_position: 4
title: d.语句
description: DataQL 一共有 8 种语句、var语句、run语句、return语句、throw语句、exit语句、if语句、else语句、hint语句、import语句
---
# 语句

DataQL 一共有 8 种
- `var` - 执行语句并将结果存入变量
- `run` - 仅执语句，但忽略结果
- `return`、`throw`、`exit` - 三大退出语句
- `if`、`else` - 分支选择语句
- `hint` - 选项语句
- `import` - 资源导入

## var - 执行语句并将结果存入变量

语法格式
- `var <变量名> = <表达式 or 值对象 or 函数定义>`

:::tip
`var` 语句是用的最广泛的语句，它有两个特性：
- 定义变量
- 执行并存储表达式的值
:::

变量定义的作用是可以保存临时查询结果，以及保存查询过程中需要用到的函数定义。例如：

`var a = 1`

执行表达式

`var a = 1 + 1`

## run - 仅执语句，但忽略结果

语法格式
- `run <表达式 or 值对象 or 函数定义>`
- `run` 语句相比 `var` 语句，只是不能保存查询结果。因此 `run` 语句也就不具备定义变量的能力。例如：下面这个语法是正确的，但是没有实际意义：

```js
run 1 + 1
```

在一些特殊场合中，查询的中间结果并不重要。因此也就无需专门为这些临时查询开辟数据保存空间，例如：把查询到的每一条数据，都调用一次 UDF。

```js
var dataSet = ...
run dataSet => [ callUdf(#) ] // 遍历 dataSet 集合并调用 callUdf 函数
```

## return、throw、exit - 三大退出语句

DataQL 有三大退出指令，分别是：return、throw、exit。它们的区别如下：

| 关键字      | 含义                       |
|----------|--------------------------|
| `return` | 终止当前过程的执行并正常退出到上一个执行过程中。 |
| `throw`  | 终止所有后续指令的执行并抛出异常。        |
| `exit`   | 终止所有后续指令的执行并正常退出。        |

它们的一般语法格式：
- `return <状态码>, <表达式 or 值对象 or 函数定义>`
- `throw <状态码>, <表达式 or 值对象 or 函数定义>`
- `exit <状态码>, <表达式 or 值对象 or 函数定义>`

还可以不指定状态码（其它退出语句同理）
- `return <表达式 or 值对象 or 函数定义>`

```js title='返回结果'
return ...
```

```js title='抛出异常，同时带上一个查询结果'
throw ...
```

```js title='返回 200 状态，查询结果是一个对象'
return 200, { ... }
```

## if - 分支选择语句

让 DataQL 变得灵活的正式由于 `if` 语句的存在，它的用法和 Java 或 JavaScript 相同。其语法格式为：

```js
if (boolean_expression) {
   /* 如果布尔表达式为真将执行的语句 */
} else {
   /* 如果布尔表达式为假将执行的语句 */
}
```

```js title='用法'
if (testExpr) {
    return ...
} else {
    return ...
}
```

```js title='DataQL 也支持多重分支'
if (testExpr1) {
    return ...
} else if (testExpr2) {
    return ...
} else if (testExpr3) {
    return ...
} else {
    return ...
}
```

## hint - 选项语句

语法格式
- `hint <选项名称> = <选项值>`
- 选项名称：是满足 `标识符` 特征的
- 选项值：可以定义 `数字`、`字符串`、`布尔`、`null` 四种基本类型数据。

```js title='计算百分比，精确到小数点后2位'
hint MAX_DECIMAL_DIGITS = 4;
hint NUMBER_ROUNDING = 'HALF_UP'; // 默认值也是 HALF_UP
var num = 1.0
var sumNum = 3.0
return num / sumNum * 100 + "%"
...
```

执行查询结果为：`33.33%`

`hint` 语句要放在 `import` 语句的前面，其作用是设置一些执行查询使用的选项参数（更多 HINT 可以参考这里）

## import - 资源导入

语法格式
- `import "<函数类或函数包类>" as <别名>`
- `import @"<资源地址>" as <别名>`
- 别名必须满足 `标识符`

`import` 语句必须要放在整个查询过程最开始的地方，它的作用是导入外部 `函数` 或者 `函数包`。被导入的资源会以 `var` 的方式定义。

例如：导入 `net.hasor.dataql.sdk.CollectionUdfSource` 函数包，并通过函数包中的函数判断一个集合是否为空。

```js
import "net.hasor.dataql.sdk.CollectionUdfSource" as collect
var dataSet = []
var test = collect.isEmpty(dataSet)
```

`import` 语句还有另外一个功效就是，可以将另外一个 DataQL 查询当作 `函数` 的形式导入到当前查询中。

例如：有一个已经保存在文件中的 DataQL 查询语句，这个查询保存在 classpath 路径中，`/net/hasor/demo.ql`

```js
return { "name": "马三", "msg": "Hello DataQL." }
```

有另外一个 DataQL 查询引用了 `demo.ql` 这个查询并拿到返回的结果。

```js
import @"/net/hasor/demo.ql" as demo
return demo()
```

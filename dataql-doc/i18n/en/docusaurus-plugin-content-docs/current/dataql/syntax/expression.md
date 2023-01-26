---
id: expression
sidebar_position: 5
title: e.表达式
description: DataQL 具备完整的表达式计算能力，这使得数据在转换过程中在需要数值计算的情况上变得非常好用。
---
# 表达式

DataQL 具备完整的表达式计算能力，这使得数据在转换过程中在需要数值计算的情况上变得非常好用。这会节省大量精力很多的精力来编写 UDF 的操作。

```js title='一个典型的场景就是是货币和汇率的转换'
return orderByUser({'id': 4}) => {
    'orderID',
    'price_rmb' : '¥' + (price_rmb * 6.9) + '元' ,
    'buyTime'
}
```

## 一元运算

```js title='对一个 bool 值进行取反'
var bool = true;
return !bool;
```

可以被一元运算的类型有：`boolean`、`number`

| 一元运算 | 针对的类型   | 语意          |
|------|---------|-------------|
| !    | boolean | 对Boolean 取非 |
| -    | number  | 数值的相反数      |

## 二元运算

```js title='例如 1 + 2 等于 3'
var a = 1;
var b = 2;
return a + b;
```

<table><tbody>
<tr>
    <th colspan="2">数学运算</th>
    <th colspan="2">位运算</th>
    <th colspan="2">比较运算</th>
    <th colspan="2">逻辑运算</th>
</tr>
<tr>
    <td><strong>+</strong></td>
    <td>加法</td>
    <td><strong>&amp;</strong></td>
    <td>按位于运算</td>
    <td><strong>&gt;</strong></td>
    <td>大于</td>
    <td><strong>||</strong></td>
    <td>逻辑或</td>
</tr>
<tr>
    <td><strong>-</strong></td>
    <td>减法</td>
    <td><strong>|</strong></td>
    <td>按位或运算</td>
    <td><strong>&gt;=</strong></td>
    <td>大于等于</td>
    <td><strong>&amp;&amp;</strong></td>
    <td>逻辑与</td>
</tr>
<tr>
    <td><strong>*</strong></td>
    <td>乘法</td>
    <td><strong>!</strong></td>
    <td>按位取反</td>
    <td><strong>&lt;</strong></td>
    <td>小于</td>
    <td><br/></td>
    <td><br/></td>
</tr>
<tr>
    <td><strong>/</strong></td>
    <td>除法</td>
    <td><strong>^</strong></td>
    <td>异或</td>
    <td><strong>&lt;=</strong></td>
    <td>小于等于</td>
    <td><br/></td>
    <td><br/></td>
</tr>
<tr>
    <td><strong>\</strong></td>
    <td>整除</td>
    <td><strong>&lt;&lt;</strong></td>
    <td>左位移</td>
    <td><strong>==</strong></td>
    <td>等于</td>
    <td><br/></td>
    <td><br/></td>
</tr>
<tr>
    <td><strong>%</strong></td>
    <td>取摸</td>
    <td><strong>&gt;&gt;</strong></td>
    <td>有符号右位移</td>
    <td><strong>!=</strong></td>
    <td>不等于</td>
    <td><br/></td>
    <td><br/></td>
</tr>
<tr>
    <td><br/></td>
    <td><br/></td>
    <td><strong>&gt;&gt;&gt;</strong></td>
    <td>无符号右位移</td>
    <td><br/></td>
    <td><br/></td>
    <td><br/></td>
    <td><br/></td>
</tr>
</tbody></table>

:::tip
二元运算主要还是面向 `number`。但加法运算是比较特殊的，它可以对任意类型做加法。执行结果就是两个数据字符串拼接。
:::

## 三元运算

语法为：`testExpr ? expr1 : expr2`

```js
var bool = true;
return (bool ? 1 : 2) ;
```

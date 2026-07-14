---
id: lexical
sidebar_position: 1
title: a.词法记号
description: 以下正式的 DataQL 语法规范可以用来帮助更深入的了解如何使用 DataQL 的所有特性、DataQL 语法、DataQL词法、DataQL运算符、DataQL关键字、DataQL标识符、DataQL分隔符。
---
# 词法记号

以下正式的语法规范可以用来帮助更深入的了解如何使用 DataQL 的所有特性。

## 源码文本

DataQL 文档表示建议使用 Unicode字符序列。但这并不是强制的，您可以通过 `java.io.Reader ` 来读取您的字符流数据。故本文不会强调您的编写查询语言所使用的字符集。

DataQL 支持单行注释和多行注释两种注释方式，被注释的代码会被语法解析器忽略。
- **单行注释**：以 `//` 开头后面的当前换行符内的所有内容均为注释内容。
- **多行注释**：以 `\*` 开始直到遇到 `*/` 为止，中间的所有内容均为注释。

## 空白字符

空白字符用于提高源文本的可读性，并作为标记之间的分隔，任何数量的空白都可能出现在任何标记之前或之后。
标记之间的空白对于 DataQL 文档的语义意义并不重要。

DataQL 会把这些字符识别为空白字符：`(空格)`、`\t`、`\n`、`\r`、`\f`

## 关键字

| 关键字       | 含义                                                 |
|-----------|----------------------------------------------------|
| `if`      | 条件语句的引导词                                           |
| `else`    | 用在条件语句中，表明当条件不成立时的分支                               |
| `return`  | 三大退出指令之一，终止当前过程的执行并正常退出到上一个执行过程中                   |
| `throw`   | 三大退出指令之一，终止所有后续指令的执行并抛出异常                          |
| `exit`    | 三大退出指令之一，终止所有后续指令的执行并正常退出                          |
| `var`     | 执行一个查询动作，并把查询结果保存到临时变量中                            |
| `run`     | 仅仅执行查询动作，不保留查询的结果                                  |
| `hint`    | 写在 DataQL 查询语句的最前面，或者通过局部 hint，用于设置一些执行选项参数        |
| `import`  | 将另外一个 DataQL 查询导入并作为一个 Udf 形式存在、或直接导入一个 Udf 到当前查询中 |
| `as`      | 与 import 关键字配合使用，用作将导入的 Udf 命名为一个本地变量名             |
| `import`  | 将另外一个 DataQL 查询导入并作为一个 Udf 形式存在、或直接导入一个 Udf 到当前查询中 |
| `true`    | 基础类型之一，表示 Boolean 的：真值                             |
| `false`   | 基础类型之一，表示 Boolean 的：假值                             |
| `null`    | 基础类型之一，表示 NULL 值                                   |

## 标识符

在编写 DataQL 中用来表示查询中的一些实体。例如：`变量名`、`参数名`

标识符必须满足正则表达式：`[_a-zA-Z][_0-9a-zA-Z]*`

在一些接口中数据对象的 key 可能会超出标识符标示的范围，这时候可以使用反引号 **&acute;xxx&acute;** 的形式来标示标识符。

## 分割符

主要用于分割语义，例如：表达式计算中的提权；语句块的包裹；函数入参的圈定等等。DataQL 的分割符有下面几种：

| 分割符 | 含义                                     |
|-----|----------------------------------------|
| ()  | 函数入参的圈定，表达式中的计算项提取权                    |
| {}  | 用来定义复合语句                               |
| []  | 对数据通过下标方式取值操作                          |
| ,   | 不同参数的分割；对象键值对或数组元素之间的分割                |
| :   | 对象键值对，键和值之间的分割                         |
| ;   | 语句的结束，DataQL 会自动推断语句结束，因此语句结束分割符并不是必须的 |

## 运算符

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

---
id: overview
title: 1. 语言入门
---

{/* llms:start */}

DataQL（Data Query Language）是一门数据查询与转换语言。脚本通过变量、表达式和函数组织计算，用对象、列表及转换模板生成结果。SQL 执行器负责查询数据库，内置函数库提供可导入的数据处理函数；应用可以扩展这两类能力。

- [第一个脚本](tutorial/first-query.md)：从返回一个值开始，逐步完成计算和列表转换。
- [语言基础](syntax/lexical.md)：学习书写规则、类型、变量和运算符。
- [流程与函数](syntax/flow.md)：编写分支、函数、导入和代码片段。
- [数据访问与转换](syntax/data.md)：读取参数、访问嵌套数据并组织结果。
- [执行选项](hints/hint_core.md)：查阅索引越界和数值精度等 Hint。
- [SQL 执行器](sql/execute.md)：使用 SQL 查询、动态规则、分页和事务。
- [内置函数库](funx/string.md)：查阅字符串、集合、日期、JSON 等函数。

脚本可在 [Dataway 控制台](../dataway/intro/quickstart.md)中运行，也可由应用[独立使用 DataQL 引擎](../dataway/dataql-engine/execute.md)执行。

{/* llms:end */}

## 一段 DataQL

```js
var people = [{"name": "Alice", "age": 25}, {"name": "Bob", "age": 30}];
return people => [{"name", "nextAge": age + 1}];
```

```json
[{"name":"Alice","nextAge":26},{"name":"Bob","nextAge":31}]
```

`var` 保存数据，`=>` 按模板转换每一项，`return` 返回查询结果。变量无需声明类型，结果可以是单个值、对象或列表。

## 阅读顺序

初次使用从本章开始，再依次阅读语言基础、流程与函数、数据访问与转换。需要数据库查询时进入 SQL 执行器章节，需要处理字符串或集合时查阅函数库。Hint 参考用于查找具体执行选项。

完整的数据处理案例见 [DataQL 数据处理专栏](/blog/topics/dataql-recipes)。

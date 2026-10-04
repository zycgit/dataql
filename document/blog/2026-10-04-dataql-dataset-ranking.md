---
slug: dataql-dataset-ranking
title: "用 DataQL 为数据集排序并生成名次"
description: "成绩列表需要按分数降序展示，并返回从 1 开始的名次。本文先排序，再为每一行追加 rank，使结果顺序和显示名次保持一致。"
authors: [zyc]
tags: [DataQL]
topics: [dataql-recipes]
language: zh-cn
---

成绩列表需要按分数降序展示，并返回从 1 开始的名次。本文先排序，再为每一行追加 rank，使结果顺序和显示名次保持一致。

<!-- truncate -->

## 输入与目标

把以下 JSON 作为查询参数传入，脚本通过 `${参数名}` 读取。

```json title="查询参数"
{
  "rows": [
    {
      "name": "Alice",
      "score": 85
    },
    {
      "name": "Bob",
      "score": 96
    },
    {
      "name": "Carol",
      "score": 90
    }
  ]
}
```

## 实现过程

listSort 的比较函数在左侧分数更高时返回 -1。state.decNumber(0) 创建一个逐次加一的计数器，第一次调用得到 1；mergeMap 保留原始字段并写入 rank。计数器在本次查询中创建，每次执行从头编号。

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
import 'net.hasor.dataql.host.function.basic.StateUdfSource' as state;
var rows = collect.listSort(${rows}, (left,right) -> {
    return left.score > right.score ? -1 : (left.score == right.score ? 0 : 1);
});
var nextRank = state.decNumber(0);
return rows => [collect.mergeMap(#, {'rank': nextRank()})];
```

## 执行结果

```json
[
  {
    "name": "Bob",
    "score": 96,
    "rank": 1
  },
  {
    "name": "Carol",
    "score": 90,
    "rank": 2
  },
  {
    "name": "Alice",
    "score": 85,
    "rank": 3
  }
]
```

这里生成连续序号，同分记录仍有不同名次，并保留输入中的相对顺序。需要并列排名时，应增加“上一条分数与当前排名”的业务状态。若数据已经排好顺序，可以省略 listSort。

## 运行源码

[完整示例工程](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example)包含 Java 入口、脚本、参数和预期结果；本篇文件位于 [cases/dataset-ranking](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example/src/main/resources/cases/dataset-ranking)。

```bash
mvn -f example/dataql-blog-example/pom.xml compile exec:java -Dexec.args=dataset-ranking
```

执行 `mvn -f example/dataql-blog-example/pom.xml test` 可核对八篇文章的结果。查询入口使用 `HostConfiguration → QueryManager → QueryBuilder`，不需要启动 Web 服务。通过 Dataway 运行时，在 Parameters 面板填入同一份参数，选择 Raw Value 查看本文的原始结果。

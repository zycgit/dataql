---
slug: dataql-left-join
title: "关联两个数据集并计算同比增长"
description: "经营报表需要把本期金额与去年同期金额放在一起。本文按商品 code 关联两份数据，以本期记录为基准，并计算增长百分比。"
authors: [zyc]
tags: [DataQL]
topics: [dataql-recipes]
language: zh-cn
---

经营报表需要把本期金额与去年同期金额放在一起。本文按商品 code 关联两份数据，以本期记录为基准，并计算增长百分比。

<!-- truncate -->

## 输入与目标

把以下 JSON 作为查询参数传入，脚本通过 `${参数名}` 读取。

```json title="查询参数"
{
  "current": [
    {
      "code": "A",
      "amount": 150
    },
    {
      "code": "B",
      "amount": 40
    },
    {
      "code": "C",
      "amount": 10
    }
  ],
  "previous": [
    {
      "code": "A",
      "amount": 100
    },
    {
      "code": "B",
      "amount": 0
    }
  ]
}
```

## 实现过程

mapJoin 为每条本期记录生成 data1 和 data2。data1 保存本期值，data2 保存对应的同期值。计算顺序为差额乘 100 再除以同期值，并使用 BigDecimal 和两位小数配置。

```javascript
hint MIN_DECIMAL_WIDTH = 'big';
hint MAX_DECIMAL_DIGITS = 2;
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
var joined = collect.mapJoin(${current}, ${previous}, {'code':'code'});
return joined => [{
    'code': data1.code,
    'amount': data1.amount,
    'previous': data2 == null ? null : data2.amount,
    'growthPercent': data2 == null || data2.amount == 0 ? null :
        (data1.amount - data2.amount) * 100 / data2.amount
}];
```

## 执行结果

```json
[
  {
    "code": "A",
    "amount": 150,
    "previous": 100,
    "growthPercent": 50.0
  },
  {
    "code": "B",
    "amount": 40,
    "previous": 0
  },
  {
    "code": "C",
    "amount": 10
  }
]
```

同期缺失或金额为 0 时，增长率返回 null，JSON 输出会省略这个字段。右侧相同键存在多条记录时，mapJoin 取最后一条；需要一对多关联时应先分组。不同年份的同一期间比较称为同比。

## 运行源码

[完整示例工程](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example)包含 Java 入口、脚本、参数和预期结果；本篇文件位于 [cases/left-join](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example/src/main/resources/cases/left-join)。

```bash
mvn -f example/dataql-blog-example/pom.xml compile exec:java -Dexec.args=left-join
```

执行 `mvn -f example/dataql-blog-example/pom.xml test` 可核对八篇文章的结果。查询入口使用 `HostConfiguration → QueryManager → QueryBuilder`，不需要启动 Web 服务。通过 Dataway 运行时，在 Parameters 面板填入同一份参数，选择 Raw Value 查看本文的原始结果。

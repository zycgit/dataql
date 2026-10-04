---
slug: dataql-row-to-col
title: "按指定字段完成数据集行列转换"
description: "报表返回每个地区一行、每个月一个字段，页面希望按月份展示各地区的数据。本文把三行两列的数值转换成两行三列，并由 columns 明确指定参与转换的字段。"
authors: [zyc]
tags: [DataQL]
topics: [dataql-recipes]
language: zh-cn
---

报表返回每个地区一行、每个月一个字段，页面希望按月份展示各地区的数据。本文把三行两列的数值转换成两行三列，并由 columns 明确指定参与转换的字段。

<!-- truncate -->

## 输入与目标

把以下 JSON 作为查询参数传入，脚本通过 `${参数名}` 读取。

```json title="查询参数"
{
  "columns": [
    "jan",
    "feb"
  ],
  "rows": [
    {
      "key": "east",
      "jan": 10,
      "feb": 20
    },
    {
      "key": "west",
      "jan": 30,
      "feb": 40
    },
    {
      "key": "north",
      "jan": 50,
      "feb": 60
    }
  ]
}
```

## 实现过程

外层按 columns 遍历。transpose 把当前列名保存在函数参数中，list2map 再使用每行的 key 作为新字段名、row[column] 作为字段值。这样主键字段不会被误当成一列业务数据。

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
var rows = ${rows};
var transpose = (column) -> {
    return {
        'column': column,
        'values': collect.list2map(rows, 'key', (index,row) -> { return row[column]; })
    };
};
return ${columns} => [transpose(#)];
```

## 执行结果

```json
[
  {
    "column": "jan",
    "values": {
      "east": 10,
      "west": 30,
      "north": 50
    }
  },
  {
    "column": "feb",
    "values": {
      "east": 20,
      "west": 40,
      "north": 60
    }
  }
]
```

每行的 key 需要唯一，重复键会由后面的行覆盖前面的行。columns 中的字段应存在于每条记录。空 rows 会生成空 values；空 columns 返回空列表。

## 运行源码

[完整示例工程](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example)包含 Java 入口、脚本、参数和预期结果；本篇文件位于 [cases/row-to-col](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example/src/main/resources/cases/row-to-col)。

```bash
mvn -f example/dataql-blog-example/pom.xml compile exec:java -Dexec.args=row-to-col
```

执行 `mvn -f example/dataql-blog-example/pom.xml test` 可核对八篇文章的结果。查询入口使用 `HostConfiguration → QueryManager → QueryBuilder`，不需要启动 Web 服务。通过 Dataway 运行时，在 Parameters 面板填入同一份参数，选择 Raw Value 查看本文的原始结果。

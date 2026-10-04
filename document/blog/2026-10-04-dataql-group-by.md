---
slug: dataql-group-by
title: "按公共字段把列表分成多个集合"
description: "成员列表需要按团队分别展示。本文使用 groupBy 将一份列表变成以团队名称为键的对象，每个键下面保留对应的完整成员列表。"
authors: [zyc]
tags: [DataQL]
topics: [dataql-recipes]
language: zh-cn
---

成员列表需要按团队分别展示。本文使用 groupBy 将一份列表变成以团队名称为键的对象，每个键下面保留对应的完整成员列表。

<!-- truncate -->

## 输入与目标

把以下 JSON 作为查询参数传入，脚本通过 `${参数名}` 读取。

```json title="查询参数"
{
  "rows": [
    {
      "name": "Alice",
      "team": "研发"
    },
    {
      "name": "Bob",
      "team": "销售"
    },
    {
      "name": "Carol",
      "team": "研发"
    }
  ]
}
```

## 实现过程

groupBy 的第二个参数是字段名 team。函数读取每条记录的该字段，把同一值的记录放进一个列表；分组内部保留原始顺序。

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
return collect.groupBy(${rows}, 'team');
```

## 执行结果

```json
{
  "研发": [
    {
      "name": "Alice",
      "team": "研发"
    },
    {
      "name": "Carol",
      "team": "研发"
    }
  ],
  "销售": [
    {
      "name": "Bob",
      "team": "销售"
    }
  ]
}
```

调用前保证每行都提供 team。显式 null 会归入字符串键 "null"，因此不要同时把真实团队名称也设为 "null"。分组不计算汇总值，需要计数时再对各组调用 size。

## 运行源码

[完整示例工程](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example)包含 Java 入口、脚本、参数和预期结果；本篇文件位于 [cases/group-by](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example/src/main/resources/cases/group-by)。

```bash
mvn -f example/dataql-blog-example/pom.xml compile exec:java -Dexec.args=group-by
```

执行 `mvn -f example/dataql-blog-example/pom.xml test` 可核对八篇文章的结果。查询入口使用 `HostConfiguration → QueryManager → QueryBuilder`，不需要启动 Web 服务。通过 Dataway 运行时，在 Parameters 面板填入同一份参数，选择 Raw Value 查看本文的原始结果。

---
slug: dataql-like-for
title: "用递归遍历列表并追加序号"
description: "接口需要按原有顺序逐条处理记录，并给每条数据追加从 0 开始的 seq_no。DataQL 可以用递归函数表达这个过程。"
authors: [zyc]
tags: [DataQL]
topics: [dataql-recipes]
language: zh-cn
---

接口需要按原有顺序逐条处理记录，并给每条数据追加从 0 开始的 seq_no。DataQL 可以用递归函数表达这个过程。

<!-- truncate -->

## 输入与目标

把以下 JSON 作为查询参数传入，脚本通过 `${参数名}` 读取。

```json title="查询参数"
{
  "rows": [
    {
      "name": "Alice"
    },
    {
      "name": "Bob"
    }
  ]
}
```

## 实现过程

append 接收列表、结果容器和当前位置。index 小于列表长度时处理当前行，再把 index + 1 交给下一次调用；到达末尾后返回容器。newList 在递归外只创建一次，各层把结果追加到同一列表。

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
var append = (rows, output, index) -> {
    if (index < collect.size(rows)) {
        run output.addLast(collect.mergeMap(rows[index], {'seq_no': index}));
        run append(rows, output, index + 1);
    }
    return output;
};
return append(${rows}, collect.newList(), 0).data();
```

## 执行结果

```json
[
  {
    "name": "Alice",
    "seq_no": 0
  },
  {
    "name": "Bob",
    "seq_no": 1
  }
]
```

列表为空时直接返回 []，不会读取下标 0。这个写法适合说明按位置处理的流程；普通字段变换优先使用列表转换表达式，较长列表避免逐行递归带来的调用栈增长。

## 运行源码

[完整示例工程](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example)包含 Java 入口、脚本、参数和预期结果；本篇文件位于 [cases/like-for](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example/src/main/resources/cases/like-for)。

```bash
mvn -f example/dataql-blog-example/pom.xml compile exec:java -Dexec.args=like-for
```

执行 `mvn -f example/dataql-blog-example/pom.xml test` 可核对八篇文章的结果。查询入口使用 `HostConfiguration → QueryManager → QueryBuilder`，不需要启动 Web 服务。通过 Dataway 运行时，在 Parameters 面板填入同一份参数，选择 Raw Value 查看本文的原始结果。

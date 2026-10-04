---
slug: dataql-dim-reduction
title: "递归展开多层数值数组"
description: "多个数据来源返回层数不一的数值数组，后续统计需要一份平面列表。本文通过递归展开所有层级，保留数值出现的先后顺序，并忽略空数组。"
authors: [zyc]
tags: [DataQL]
topics: [dataql-recipes]
language: zh-cn
---

多个数据来源返回层数不一的数值数组，后续统计需要一份平面列表。本文通过递归展开所有层级，保留数值出现的先后顺序，并忽略空数组。

<!-- truncate -->

## 输入与目标

把以下 JSON 作为查询参数传入，脚本通过 `${参数名}` 读取。

```json title="查询参数"
{
  "values": [
    [
      1,
      2,
      [
        3
      ]
    ],
    [],
    [
      4,
      [
        5,
        6
      ]
    ]
  ]
}
```

## 实现过程

`value => [#]` 将当前值转换为可遍历列表。数值会成为单元素列表，首元素仍等于原值；数组则进入递归。newList 累积最终结果，空列表在读取下标前返回。

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
var flatten = (value, output) -> {
    var items = value => [#];
    if (collect.size(items) == 0) {
        return output;
    }
    if (items[0] == value) {
        run output.addLast(value);
    } else {
        run items => [flatten(#, output)];
    }
    return output;
};
return flatten(${values}, collect.newList()).data();
```

## 执行结果

```json
[
  1,
  2,
  3,
  4,
  5,
  6
]
```

示例输入限定为数值和嵌套数组，空数组通过 size 判断，其他对象类型应另行定义展开规则。深层递归仍受调用栈限制，扁平列表可以直接使用 collect.merge。

## 运行源码

[完整示例工程](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example)包含 Java 入口、脚本、参数和预期结果；本篇文件位于 [cases/dim-reduction](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example/src/main/resources/cases/dim-reduction)。

```bash
mvn -f example/dataql-blog-example/pom.xml compile exec:java -Dexec.args=dim-reduction
```

执行 `mvn -f example/dataql-blog-example/pom.xml test` 可核对八篇文章的结果。查询入口使用 `HostConfiguration → QueryManager → QueryBuilder`，不需要启动 Web 服务。通过 Dataway 运行时，在 Parameters 面板填入同一份参数，选择 Raw Value 查看本文的原始结果。

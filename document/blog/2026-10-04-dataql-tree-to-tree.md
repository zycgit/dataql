---
slug: dataql-tree-to-tree
title: "用 DataQL 转换树节点，保留层级关系"
description: "菜单、组织架构和设备目录常常有相同的树结构，却使用不同的字段名。本文将 value、text、ChildNodes 转成前端需要的 id、text、children，并保留每一层父子关系。"
authors: [zyc]
tags: [DataQL]
topics: [dataql-recipes]
language: zh-cn
---

菜单、组织架构和设备目录常常有相同的树结构，却使用不同的字段名。本文将 value、text、ChildNodes 转成前端需要的 id、text、children，并保留每一层父子关系。

<!-- truncate -->

## 输入与目标

把以下 JSON 作为查询参数传入，脚本通过 `${参数名}` 读取。

```json title="查询参数"
{
  "nodes": [
    {
      "value": "root",
      "text": "总部",
      "ChildNodes": [
        {
          "value": "team",
          "text": "研发",
          "ChildNodes": []
        }
      ]
    }
  ]
}
```

## 实现过程

每个节点由同一个 convert 函数处理：复制当前层字段，再对 ChildNodes 执行列表转换。递归结束条件是空子节点列表；叶子节点输出 children: []。

```javascript
var convert = (node) -> {
    return {
        'id': node.value,
        'text': node.text,
        'children': node.ChildNodes => [convert(#)]
    };
};
return ${nodes} => [convert(#)];
```

## 执行结果

```json
[
  {
    "id": "root",
    "text": "总部",
    "children": [
      {
        "id": "team",
        "text": "研发",
        "children": []
      }
    ]
  }
]
```

输入中的 ChildNodes 统一为列表，叶子节点使用空列表。节点深度决定递归深度，大量深层节点应在数据源侧控制层数。此例只重命名和筛选字段，不改变节点顺序。

## 运行源码

[完整示例工程](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example)包含 Java 入口、脚本、参数和预期结果；本篇文件位于 [cases/tree-to-tree](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example/src/main/resources/cases/tree-to-tree)。

```bash
mvn -f example/dataql-blog-example/pom.xml compile exec:java -Dexec.args=tree-to-tree
```

执行 `mvn -f example/dataql-blog-example/pom.xml test` 可核对八篇文章的结果。查询入口使用 `HostConfiguration → QueryManager → QueryBuilder`，不需要启动 Web 服务。通过 Dataway 运行时，在 Parameters 面板填入同一份参数，选择 Raw Value 查看本文的原始结果。

---
slug: dataql-build-tree
title: "把父子关系表转换为树形数据"
description: "组织表通常用 id 和 parent_id 保存层级，前端树组件则需要嵌套 children。本文从根节点开始查找子节点，把平面记录组织成树。"
authors: [zyc]
tags: [DataQL]
topics: [dataql-recipes]
language: zh-cn
---

组织表通常用 id 和 parent_id 保存层级，前端树组件则需要嵌套 children。本文从根节点开始查找子节点，把平面记录组织成树。

<!-- truncate -->

## 输入与目标

把以下 JSON 作为查询参数传入，脚本通过 `${参数名}` 读取。

```json title="查询参数"
{
  "rows": [
    {
      "id": 1,
      "parent_id": null,
      "label": "总部"
    },
    {
      "id": 2,
      "parent_id": 1,
      "label": "研发"
    },
    {
      "id": 3,
      "parent_id": 1,
      "label": "销售"
    },
    {
      "id": 4,
      "parent_id": 2,
      "label": "平台组"
    }
  ]
}
```

## 实现过程

children(parentId) 先筛选 parent_id 等于指定值的记录，再为每条记录递归生成 children。入口 children(null) 找到所有根节点，列表转换保留每一层的原始顺序。

```javascript
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
var rows = ${rows};
var children = (parentId) -> {
    var matches = collect.filter(rows, (row) -> { return row.parent_id == parentId; });
    return matches => [{
        'id': id,
        'label': label,
        'children': children(id)
    }];
};
return children(null);
```

## 执行结果

```json
[
  {
    "id": 1,
    "label": "总部",
    "children": [
      {
        "id": 2,
        "label": "研发",
        "children": [
          {
            "id": 4,
            "label": "平台组",
            "children": []
          }
        ]
      },
      {
        "id": 3,
        "label": "销售",
        "children": []
      }
    ]
  }
]
```

id 应唯一，父子引用不能形成环。没有根节点可达路径的孤立记录不会出现在结果里。本例每层查找都会扫描原始列表，适合小型目录；大数据集可以先按 parent_id 分组，再按父节点取子列表。

## 运行源码

[完整示例工程](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example)包含 Java 入口、脚本、参数和预期结果；本篇文件位于 [cases/build-tree](https://gitee.com/zycgit/dataql/tree/dev/example/dataql-blog-example/src/main/resources/cases/build-tree)。

```bash
mvn -f example/dataql-blog-example/pom.xml compile exec:java -Dexec.args=build-tree
```

执行 `mvn -f example/dataql-blog-example/pom.xml test` 可核对八篇文章的结果。查询入口使用 `HostConfiguration → QueryManager → QueryBuilder`，不需要启动 Web 服务。通过 Dataway 运行时，在 Parameters 面板填入同一份参数，选择 Raw Value 查看本文的原始结果。

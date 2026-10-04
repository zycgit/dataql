---
id: valuescope
title: 4.2 参数与上下文
description: DataQL 参数域、转换环境栈与递归取值。
---

DataQL 使用 `$`、`@`、`#` 三个符号访问参数或转换中的数据。带花括号的参数访问与不带花括号的环境访问含义不同。

## 参数访问

| 写法 | 含义 |
| --- | --- |
| `${name}` | 从 `$` 参数域读取 `name` |
| `@{name}` | 从 `@` 参数域读取 `name` |
| `#{name}` | 从 `#` 参数域读取 `name` |

同名参数可放在不同域中。假设宿主分别为三个域提供 `name` 值，脚本可直接读取：

```js
return {"request": ${name}, "application": @{name}, "custom": #{name}};
```

三个参数域的内容由运行脚本的应用提供。Dataway 将请求参数提供给 `$` 域，`@`、`#` 的内容由应用配置，详见[自定义作用域](../../dataway/engine/scope.md)。

## 转换中的环境访问

结果转换 `=>` 会将当前数据放入环境栈，嵌套转换会建立新的层级。

| 写法 | 含义 |
| --- | --- |
| `#`、`#.name` | 当前转换的数据及其字段 |
| `$`、`$.name` | 最外层转换的数据及其字段 |
| `@`、`@[0]` | 整个环境栈及指定层级 |

```js
var data = {
    "id": 10,
    "user": {"name": "Alice"}
};
return data => {
    "user": user => {
        "name",
        "ownerId": $.id
    }
};
```

结果为 `{"user":{"name":"Alice","ownerId":10}}`。列表转换也会建立遍历层级，访问上层数据时应根据实际嵌套层次选择下标。

### 递归读取父节点

列表转换会依次将列表和当前元素放入环境栈，完成转换后退出对应层级。`@[0]` 从栈底读取，`@[-1]` 从栈顶读取。下面递归转换一棵树，为每个子节点补充 `parent_id`：

```js
var treeData = [
    {
        "id": 1,
        "label": "t1",
        "children": [
            {
                "id": 2,
                "label": "t2",
                "children": [
                    {"id": 4, "label": "t4", "children": []}
                ]
            },
            {"id": 3, "label": "t3", "children": []}
        ]
    },
    {"id": 5, "label": "t5", "children": []}
]

var treeFmt = (dat) -> {
    return {
        "id": dat.id,
        "parent_id": ((@[-3] != null) ? @[-3].id : null),
        "label": dat.label,
        "children": dat.children => [ treeFmt(#) ]
    }
}

return treeData => [ treeFmt(#) ]
```

处理 `id = 4` 的节点时，环境栈包含六层数据：

| 访问位置 | 对应数据 |
| --- | --- |
| `@[-1]`、`#` | 当前节点，`id = 4` |
| `@[-2]` | 父节点的 `children` 列表 |
| `@[-3]` | 父节点，`id = 2` |
| `@[-4]` | `id = 1` 节点的 `children` 列表 |
| `@[-5]` | `id = 1` 的节点 |
| `@[-6]`、`$` | 最外层的 `treeData` 列表 |

![嵌套列表转换的环境栈与父节点位置](/img/dataql/CC2_F439_1D05_42F7.jpeg)

本例中，`@[-3].id` 读取父节点的编号。下标取决于转换的嵌套层次，增加一层转换后，应按新的环境栈选择位置。

执行结果如下，根节点的 `parent_id` 为 `null`，在此 JSON 输出中省略：

```json
[
    {
        "id": 1,
        "label": "t1",
        "children": [
            {
                "id": 2,
                "parent_id": 1,
                "label": "t2",
                "children": [
                    {"id": 4, "parent_id": 2, "label": "t4", "children": []}
                ]
            },
            {"id": 3, "parent_id": 1, "label": "t3", "children": []}
        ]
    },
    {"id": 5, "label": "t5", "children": []}
]
```

## 同名变量与字段

省略访问符时，优先读取可见的本地变量，再读取当前转换对象的字段。需要明确读取字段时使用 `#.字段名`。

```js
var name = 'local';
var data = {"name": "Alice"};
return data => {"variable": name, "field": #.name};
```

结果为 `{"variable":"local","field":"Alice"}`。`${name}` 始终表示参数访问，不表示转换环境的根字段。

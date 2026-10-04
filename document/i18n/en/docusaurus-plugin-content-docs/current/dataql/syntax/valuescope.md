---
id: valuescope
title: 4.2 Parameters and context
description: DataQL parameter scopes, transformation contexts and recursive access.
---

DataQL uses `$`, `@` and `#` to access parameters or data during a transformation. Braces distinguish parameter access from transformation context access.

## Parameter access

| Syntax | Meaning |
| --- | --- |
| `${name}` | Read `name` from the `$` parameter scope |
| `@{name}` | Read `name` from the `@` parameter scope |
| `#{name}` | Read `name` from the `#` parameter scope |

Each scope can contain a parameter with the same name. When the host supplies a `name` value in all three scopes, read them as follows:

```js
return {"request": ${name}, "application": @{name}, "custom": #{name}};
```

The application supplies the contents of these scopes. Dataway exposes request parameters through `$`; the application configures `@` and `#`. See [Custom scopes](../../dataway/engine/scope.md).

## Transformation contexts

The `=>` transformation pushes its input onto the context stack. Nested transformations add further levels.

| Syntax | Meaning |
| --- | --- |
| `#`, `#.name` | The current input and one of its fields |
| `$`, `$.name` | The outermost input and one of its fields |
| `@`, `@[0]` | The complete context stack and an indexed level |

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

The result is `{"user":{"name":"Alice","ownerId":10}}`. List transformations also add iteration levels. Choose an index according to the actual nesting depth.

### Read parent nodes recursively

A list transformation pushes the list and then its current item onto the context stack, removing these levels when the transformation finishes. `@[0]` reads from the bottom; `@[-1]` reads from the top. This example transforms a tree recursively and adds `parent_id` to each child:

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

When processing the node with `id = 4`, the stack contains six levels:

| Position | Data |
| --- | --- |
| `@[-1]`, `#` | The current node, `id = 4` |
| `@[-2]` | The parent's `children` list |
| `@[-3]` | The parent node, `id = 2` |
| `@[-4]` | The `children` list of the node with `id = 1` |
| `@[-5]` | The node with `id = 1` |
| `@[-6]`, `$` | The outermost `treeData` list |

![Context stack and parent positions in nested list transformations](/img/dataql/CC2_F439_1D05_42F7.jpeg)

Here, `@[-3].id` reads the parent's ID. The index depends on the transformation depth; adjust it when adding another transformation level.

The result follows. Root nodes have a `null` parent ID, omitted from this JSON output:

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

## Variables and fields with the same name

Without an access symbol, DataQL first checks visible local variables and then fields of the current transformation input. Use `#.fieldName` to read a field explicitly.

```js
var name = 'local';
var data = {"name": "Alice"};
return data => {"variable": name, "field": #.name};
```

The result is `{"variable":"local","field":"Alice"}`. `${name}` always reads a parameter; `$.name` reads a field of the outermost transformation input.

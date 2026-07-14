---
id: json
sidebar_position: 4
title: d.Json函数库
description: DataQL FunctionX库函数，Json函数库
---
# Json函数库

引入 Json 函数库的方式为：`import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;`

## toJson
函数定义：`String toJson(target)`
- 参数定义：`target` 类型：`任意`
- 返回类型：`String`
- 作用：把对象序列化为 Json 格式。

```js title='例子'
json.toJson([])          = "[]"
json.toJson({})          = "{}"
json.toJson([0,1,2])     = "[0,1,2]"
json.toJson({'key':123}) = "{\"key\":123}"
json.toJson(null)        = "null"
```

## toFmtJson
函数定义：`String toFmtJson(target)`
- 参数定义：`target` 类型：`任意`
- 返回类型：`String`
- 作用：把对象 JSON 序列化（带格式）

```js title='例子'
json.toFmtJson([])          = "[]"
json.toFmtJson({})          = "{}"
json.toFmtJson([0,1,2])     = "[\n\t0,\n\t1,\n\t2\n]"
json.toFmtJson({'key':123}) = "{\n\t\"key\":123\n}"
json.toFmtJson(null)        = "null"
```

## fromJson
函数定义：`Object fromJson(jsonString)`
- 参数定义：`jsonString` 类型：`String`
- 返回类型：`Object`
- 作用：把 JSON 格式的字符串解析成对象。

```js title='例子'
json.fromJson("[]")                     = []
json.fromJson("{}")                     = {}
json.fromJson("[\n\t0,\n\t1,\n\t2\n]")  = [0,1,2]
json.fromJson("{\n\t\"key\":123\n}")    = {'key':123}
json.fromJson("null")                   = null
```

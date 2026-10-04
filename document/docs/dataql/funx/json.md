---
id: json
title: 7.5 JSON 函数
---

:::info 依赖模块
本库由 `dataql-engine` 提供，导入 `net.hasor.dataql.host.function.encryt.JsonUdfSource` 后使用。
:::

本库在 DataQL 数据和 JSON 字符串之间转换。

## toJson

`json.toJson(value)` 接收对象、列表或基本值，返回 JSON 字符串。对象中的空值字段省略，列表中的空值保留，根值 `null` 返回字符串 `"null"`。

```js
import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;
return json.toJson({'name':'Alice', 'nickname':null, 'tags':[null, 'dataql']});
```

返回字符串，其内容为：

```json
{"name":"Alice","tags":[null,"dataql"]}
```

## toFmtJson

`json.toFmtJson(value)` 接收与 `toJson` 相同的数据，返回带缩进和换行的 JSON 字符串。

```js
import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;
return json.toFmtJson({'name':'Alice', 'enabled':true});
```

返回字符串，其内容为：

```json
{
  "name" : "Alice",
  "enabled" : true
}
```

## fromJson

`json.fromJson(text)` 接收 JSON 字符串，返回对应的对象、列表或基本值，可继续取值和转换。`null` 和空白输入返回 `null`，非法 JSON 抛出异常；JSON 中的空值字段会保留，小数按十进制数解析。

```js
import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;
var person = json.fromJson('{"name":"Alice","age":25,"nickname":null}');
var values = json.fromJson('[1,2,3]');
return {
    'name': person.name,
    'nextAge': person.age + 1,
    'nicknameIsNull': person.nickname == null,
    'first': values[0],
    'enabled': json.fromJson('true')
};
```

```json
{"name":"Alice","nextAge":26,"nicknameIsNull":true,"first":1,"enabled":true}
```

函数和二进制值需先转换为适合 JSON 的数据，二进制编码见[类型转换函数](convert.md)。

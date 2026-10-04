---
id: json
title: 7.5 JSON functions
---

:::info Module dependency
Provided by `dataql-engine`. Import `net.hasor.dataql.host.function.encryt.JsonUdfSource` to use this library.
:::

Convert between DataQL values and JSON strings.

## toJson

`json.toJson(value)` accepts objects, lists or primitive values and returns a JSON string. Null object fields are omitted, null list elements remain, and a root null produces the string `"null"`.

```js
import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;
return json.toJson({'name':'Alice', 'nickname':null, 'tags':[null, 'dataql']});
```

Returns a string containing:

```json
{"name":"Alice","tags":[null,"dataql"]}
```

## toFmtJson

`json.toFmtJson(value)` accepts the same values as `toJson` and returns an indented JSON string with line breaks.

```js
import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;
return json.toFmtJson({'name':'Alice', 'enabled':true});
```

Returns a string containing:

```json
{
  "name" : "Alice",
  "enabled" : true
}
```

## fromJson

`json.fromJson(text)` accepts JSON text and returns its object, list or primitive value for further access and transformation. Null and blank input return null; invalid JSON throws an error. Explicit null fields are retained and decimal numbers use decimal precision.

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

Convert functions and binary values to suitable JSON data first. See [Conversion functions](convert.md) for binary encoding.

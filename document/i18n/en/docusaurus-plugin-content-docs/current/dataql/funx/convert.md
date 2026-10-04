---
id: convert
title: 7.6 Conversion functions
---

:::info Module dependency
Provided by `dataql-engine`. Import `net.hasor.dataql.host.function.basic.ConvertUdfSource` to use this library.
:::

Convert values to numbers, strings, booleans or binary data. Each example runs independently. Square brackets in signatures denote optional parameters.

## toInt

`convert.toInt(target)` accepts a number or numeric string and returns a number, preserving fractions. `null`, blank strings and other types return `0`; invalid numeric strings throw an error.

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.toInt('12.5'), convert.toInt('42'), convert.toInt(null), convert.toInt(' ')];
```

Returns `[12.5, 42, 0, 0]`.

## toString

`convert.toString(target)` returns a string representation; `null` becomes the string `"null"`. Use [json.toJson](json.md#tojson) for JSON objects.

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.toString(12), convert.toString(true), convert.toString(null)];
```

Returns `["12", "true", "null"]`.

## toBoolean

`convert.toBoolean(target)` preserves booleans and accepts case-insensitive boolean strings including `true/false`, `on/off` and `yes/no`. Unrecognized strings return `null`; null, numbers and other types return `false`.

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.toBoolean('ON'), convert.toBoolean('off'),
        convert.toBoolean('blue'), convert.toBoolean(null), convert.toBoolean(1)];
```

Returns `[true, false, null, false, false]`.

## textToByte

`convert.textToByte(text[, charset])` encodes a string as binary data. `charset` is a charset name and defaults to `UTF-8`. Null and empty strings produce empty binary values.

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
var content = convert.textToByte('你好', 'UTF-16LE');
return {'hex': convert.byteToHex(content), 'text': convert.byteToString(content, 'UTF-16LE')};
```

Returns `{"hex":"604F7D59","text":"你好"}`. Pass binary values directly to other functions; this example decodes them to show their contents.

## stringToByte

`convert.stringToByte(text[, charset])` returns binary data with the same behavior as `textToByte`.

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return convert.byteToHex(convert.stringToByte('AB'));
```

Returns `"4142"`.

## hexToByte

`convert.hexToByte(text)` parses an even-length hexadecimal string as binary data. Hex letters are case-insensitive. Null returns null; an empty string returns empty binary data.

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return convert.byteToHex(convert.hexToByte('007fff'));
```

Returns `"007FFF"`.

## byteToHex

`convert.byteToHex(content)` returns uppercase hex from binary data or a numeric byte list. Null returns null; empty content returns an empty string.

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.byteToHex([0, 127, -1]), convert.byteToHex([]), convert.byteToHex(null)];
```

Returns `["007FFF", "", null]`. Numbers are converted to signed bytes; for example, `-1` becomes `FF`.

## byteToString

`convert.byteToString(content[, charset])` decodes binary data or a numeric byte list. The charset defaults to `UTF-8`. Null returns null; empty content returns an empty string.

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.byteToString([65, 66]), convert.byteToString([]), convert.byteToString(null)];
```

Returns `["AB", "", null]`.

Binary reads close the stream they open. In-memory bytes support repeated reads; a single-use stream can be consumed only once. Uploaded temporary files are cleaned up after the request. See [Binary responses](../../dataway/capabilities/development/response.md#binary-response) for HTTP output.

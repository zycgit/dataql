---
id: convert
title: 7.6 类型转换函数
---

:::info 依赖模块
本库由 `dataql-engine` 提供，导入 `net.hasor.dataql.host.function.basic.ConvertUdfSource` 后使用。
:::

本库将值转换为数值、文本、布尔值或二进制。以下示例均可独立运行，方括号中的参数表示可省略。

## toInt

`convert.toInt(target)` 接收数值或数字字符串，返回数值并保留小数部分。`null`、空白字符串和其他类型返回 `0`，非法数字字符串抛出异常。

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.toInt('12.5'), convert.toInt('42'), convert.toInt(null), convert.toInt(' ')];
```

返回 `[12.5, 42, 0, 0]`。

## toString

`convert.toString(target)` 返回值的字符串表示，`null` 返回字符串 `"null"`。对象的 JSON 表示使用 [json.toJson](json.md#tojson)。

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.toString(12), convert.toString(true), convert.toString(null)];
```

返回 `["12", "true", "null"]`。

## toBoolean

`convert.toBoolean(target)` 接收布尔值或布尔字符串，返回布尔值；字符串支持 `true/false`、`on/off`、`yes/no`，忽略大小写。无法识别的字符串返回 `null`，`null`、数值和其他类型返回 `false`。

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.toBoolean('ON'), convert.toBoolean('off'),
        convert.toBoolean('blue'), convert.toBoolean(null), convert.toBoolean(1)];
```

返回 `[true, false, null, false, false]`。

## textToByte

`convert.textToByte(text[, charset])` 将字符串编码为二进制值，`charset` 为字符集名称，默认 `UTF-8`。`null` 和空字符串返回空二进制值。

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
var content = convert.textToByte('你好', 'UTF-16LE');
return {'hex': convert.byteToHex(content), 'text': convert.byteToString(content, 'UTF-16LE')};
```

返回 `{"hex":"604F7D59","text":"你好"}`。二进制值可以直接交给其他函数；示例将其转换为十六进制和文本以展示内容。

## stringToByte

`convert.stringToByte(text[, charset])` 与 `textToByte` 相同，返回二进制值。

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return convert.byteToHex(convert.stringToByte('AB'));
```

返回 `"4142"`。

## hexToByte

`convert.hexToByte(text)` 将偶数长度的十六进制字符串解析为二进制，字母可大小写混用。`null` 返回 `null`，空字符串返回空二进制值。

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return convert.byteToHex(convert.hexToByte('007fff'));
```

返回 `"007FFF"`。

## byteToHex

`convert.byteToHex(content)` 将二进制或字节数值列表转换为大写十六进制字符串。`null` 返回 `null`，空内容返回空字符串。

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.byteToHex([0, 127, -1]), convert.byteToHex([]), convert.byteToHex(null)];
```

返回 `["007FFF", "", null]`。列表中的数值按有符号字节转换，例如 `-1` 对应 `FF`。

## byteToString

`convert.byteToString(content[, charset])` 按指定字符集解码二进制或字节数值列表，默认 `UTF-8`。`null` 返回 `null`，空内容返回空字符串。

```js
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return [convert.byteToString([65, 66]), convert.byteToString([]), convert.byteToString(null)];
```

返回 `["AB", "", null]`。

读取二进制时会关闭本次打开的流。内存字节可以重复读取；单次输入流只能消费一次。上传文件的临时资源在请求结束时清理，HTTP 输出见[二进制响应](../../dataway/capabilities/development/response.md#binary-response)。

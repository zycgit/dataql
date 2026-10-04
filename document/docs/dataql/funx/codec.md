---
id: codec
title: 7.7 编码与摘要函数
---

:::info 依赖模块
本页函数由 `net.hasor:dataql-engine` 提供，通过 `CodecUdfSource` 引入；摘要结果转十六进制时还需引入 `ConvertUdfSource`。
:::

```javascript
import 'net.hasor.dataql.host.function.encryt.CodecUdfSource' as codec;
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
return {
    'base64': codec.encodeString('AB'),
    'text': codec.decodeString('QUI='),
    'url': codec.urlEncode('a b+c'),
    'sha256': convert.byteToHex(codec.digestString('SHA256', 'AB'))
};
```

结果：

```json
{
  "base64": "QUI=",
  "text": "AB",
  "url": "a+b%2Bc",
  "sha256": "38164FBD17603D73F696B8B4D72664D735BB6A7C88577687FD2AE33FD6964153"
}
```

## Base64

| DataQL 调用 | 入参 | 返回值 |
| --- | --- | --- |
| `codec.encodeString(text)` | 待编码文本 | Base64 字符串 |
| `codec.decodeString(text)` | Base64 字符串 | 解码后的文本 |
| `codec.encodeBytes(values)` | 字节列表，例如 `decodeBytes` 的返回值 | Base64 字符串 |
| `codec.decodeBytes(text)` | Base64 字符串 | 解码后的字节列表 |

文本编解码使用运行环境默认字符集。若需要按指定字符集解码，可将 `decodeBytes` 的结果传给 [convert.byteToString](convert.md#bytetostring)。

```javascript
import 'net.hasor.dataql.host.function.encryt.CodecUdfSource' as codec;
var bytes = codec.decodeBytes('QUI=');
return {
    'encodedText': codec.encodeString('AB'),
    'decodedText': codec.decodeString('QUI='),
    'decodedBytes': bytes,
    'encodedBytes': codec.encodeBytes(bytes),
    'nullValue': codec.decodeString(null),
    'emptyText': codec.encodeString(''),
    'emptyBytes': codec.decodeBytes('')
};
```

结果：

```json
{
  "encodedText": "QUI=",
  "decodedText": "AB",
  "decodedBytes": [65, 66],
  "encodedBytes": "QUI=",
  "nullValue": null,
  "emptyText": "",
  "emptyBytes": []
}
```

四个函数的 `null` 输入均返回 `null`。空文本的编码与解码结果都是 `""`，空字节列表编码为 `""`，空 Base64 字符串解码为 `[]`。非法 Base64 内容会使解码失败。

`encodeBytes`、`digestBytes`、`hmacBytes` 使用字节列表，可以直接传入 `decodeBytes` 的返回值。类型转换函数生成的二进制值不能直接作为这些字节函数的入参。

## URL 编码

| DataQL 调用 | 入参 | 返回值 |
| --- | --- | --- |
| `codec.urlEncode(text)` | 待编码文本 | 按 UTF-8 编码后的字符串 |
| `codec.urlDecode(text)` | 已编码文本 | 按 UTF-8 解码后的字符串 |
| `codec.urlEncodeBy(text, charset)` | 待编码文本、字符集名称 | 按指定字符集编码后的字符串 |
| `codec.urlDecodeBy(text, charset)` | 已编码文本、字符集名称 | 按指定字符集解码后的字符串 |

遵循表单 URL 编码规则：空格编码为 `+`，原有 `+` 编码为 `%2B`；解码时裸 `+` 会变为空格。对单个参数值编码后再拼接 URL，解码时使用与编码时相同的字符集。

```javascript
import 'net.hasor.dataql.host.function.encryt.CodecUdfSource' as codec;
return {
    'encoded': codec.urlEncode('a b+c'),
    'decoded': codec.urlDecode('a+b%2Bc'),
    'gbkEncoded': codec.urlEncodeBy('数据查询', 'GBK'),
    'gbkDecoded': codec.urlDecodeBy('%CA%FD%BE%DD%B2%E9%D1%AF', 'GBK')
};
```

结果：

```json
{
  "encoded": "a+b%2Bc",
  "decoded": "a b+c",
  "gbkEncoded": "%CA%FD%BE%DD%B2%E9%D1%AF",
  "gbkDecoded": "数据查询"
}
```

`text` 为 `null` 时返回 `null`，空字符串仍返回空字符串。不支持的字符集或不完整的百分号转义（例如 `%2`）会使对应调用失败。

## 摘要

| DataQL 调用 | 入参 | 返回值 |
| --- | --- | --- |
| `codec.digestString(algorithm, text)` | 算法名称、待计算文本 | 摘要字节列表 |
| `codec.digestBytes(algorithm, values)` | 算法名称、字节列表 | 摘要字节列表 |

`algorithm` 忽略大小写，支持 `MD5`、`SHA`、`SHA1`、`SHA256`、`SHA512`；`SHA` 与 `SHA1` 的结果相同。使用这里列出的名称，例如 `SHA256`，不要写成 `SHA-256`。文本使用运行环境默认字符集。

摘要返回字节列表，使用 `convert.byteToHex(...)` 得到大写十六进制字符串。

```javascript
import 'net.hasor.dataql.host.function.encryt.CodecUdfSource' as codec;
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
var bytes = codec.decodeBytes('QUI=');
return {
    'md5': convert.byteToHex(codec.digestString('MD5', 'AB')),
    'sha1': convert.byteToHex(codec.digestString('SHA1', 'AB')),
    'sha256': convert.byteToHex(codec.digestString('SHA256', 'AB')),
    'sha256Bytes': convert.byteToHex(codec.digestBytes('SHA256', bytes))
};
```

结果：

```json
{
  "md5": "B86FC6B051F63D73DE262D4C34E3A0A9",
  "sha1": "06D945942AA26A61BE18C3E22BF19BBCA8DD2B5D",
  "sha256": "38164FBD17603D73F696B8B4D72664D735BB6A7C88577687FD2AE33FD6964153",
  "sha256Bytes": "38164FBD17603D73F696B8B4D72664D735BB6A7C88577687FD2AE33FD6964153"
}
```

算法有效时，内容为 `null` 返回 `null`，空内容则正常计算摘要。例如 `convert.byteToHex(codec.digestString('MD5', ''))` 返回 `D41D8CD98F00B204E9800998ECF8427E`。不支持的算法会使调用失败，即使内容为 `null` 也是如此。

## HMAC

| DataQL 调用 | 入参 | 返回值 |
| --- | --- | --- |
| `codec.hmacString(algorithm, key, text)` | 算法名称、密钥文本、待签名文本 | Base64 签名字符串 |
| `codec.hmacBytes(algorithm, key, values)` | 算法名称、密钥文本、待签名字节列表 | Base64 签名字符串 |

支持 `HmacMD5`、`HmacSHA1`、`HmacSHA256`、`HmacSHA512`，名称忽略大小写。`key` 为非空密钥文本；密钥与 `hmacString` 的文本都使用运行环境默认字符集。

```javascript
import 'net.hasor.dataql.host.function.encryt.CodecUdfSource' as codec;
var bytes = codec.decodeBytes('SGVsbG8gRGF0YVFM');
return {
    'textSignature': codec.hmacString('HmacSHA256', 'example-key', 'Hello DataQL'),
    'bytesSignature': codec.hmacBytes('HmacSHA256', 'example-key', bytes)
};
```

结果：

```json
{
  "textSignature": "G4KRO7PmODFcmutDnoxR+c+1c+ZuBrI3BS9Wacso804=",
  "bytesSignature": "G4KRO7PmODFcmutDnoxR+c+1c+ZuBrI3BS9Wacso804="
}
```

HMAC 的返回值已经是 Base64 字符串，可直接保存或传递。若需要十六进制表示，先用 `codec.decodeBytes(...)` 解码签名，再用 `convert.byteToHex(...)` 转换。

算法有效时，内容为 `null` 返回 `null`；空内容会正常计算签名。不支持的算法会使调用失败，即使内容为 `null` 也是如此。

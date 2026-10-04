---
id: codec
title: 7.7 Encoding and digest functions
---

:::info Module dependency
These functions are provided by `net.hasor:dataql-engine`. Import `CodecUdfSource` to use them, and import `ConvertUdfSource` to format digests as hexadecimal strings.
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

Result:

```json
{
  "base64": "QUI=",
  "text": "AB",
  "url": "a+b%2Bc",
  "sha256": "38164FBD17603D73F696B8B4D72664D735BB6A7C88577687FD2AE33FD6964153"
}
```

## Base64

| DataQL call | Arguments | Return value |
| --- | --- | --- |
| `codec.encodeString(text)` | Text to encode | Base64 string |
| `codec.decodeString(text)` | Base64 string | Decoded text |
| `codec.encodeBytes(values)` | Byte list, such as the result of `decodeBytes` | Base64 string |
| `codec.decodeBytes(text)` | Base64 string | Decoded byte list |

Text encoding and decoding use the runtime's default character set. To decode with a specific character set, pass the result of `decodeBytes` to [convert.byteToString](convert.md#bytetostring).

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

Result:

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

All four functions return `null` for a `null` input. Encoding or decoding empty text returns `""`; encoding an empty byte list returns `""`, and decoding an empty Base64 string to bytes returns `[]`. Invalid Base64 content causes decoding to fail.

`encodeBytes`, `digestBytes`, and `hmacBytes` take byte lists, including the result of `decodeBytes`. Binary values produced by the conversion functions cannot be passed directly to these byte functions.

## URL encoding {#url-编码}

| DataQL call | Arguments | Return value |
| --- | --- | --- |
| `codec.urlEncode(text)` | Text to encode | String encoded with UTF-8 |
| `codec.urlDecode(text)` | Encoded text | String decoded with UTF-8 |
| `codec.urlEncodeBy(text, charset)` | Text to encode, character set name | String encoded with the specified character set |
| `codec.urlDecodeBy(text, charset)` | Encoded text, character set name | String decoded with the specified character set |

These functions use form URL encoding: a space becomes `+`, and an existing `+` becomes `%2B`. Decoding turns an unescaped `+` into a space. Encode individual parameter values before constructing a URL, and decode with the same character set used for encoding.

```javascript
import 'net.hasor.dataql.host.function.encryt.CodecUdfSource' as codec;
return {
    'encoded': codec.urlEncode('a b+c'),
    'decoded': codec.urlDecode('a+b%2Bc'),
    'gbkEncoded': codec.urlEncodeBy('数据查询', 'GBK'),
    'gbkDecoded': codec.urlDecodeBy('%CA%FD%BE%DD%B2%E9%D1%AF', 'GBK')
};
```

Result:

```json
{
  "encoded": "a+b%2Bc",
  "decoded": "a b+c",
  "gbkEncoded": "%CA%FD%BE%DD%B2%E9%D1%AF",
  "gbkDecoded": "数据查询"
}
```

A `null` `text` returns `null`, and an empty string remains empty. Unsupported character sets or incomplete percent escapes such as `%2` cause the corresponding call to fail.

## Digests {#摘要}

| DataQL call | Arguments | Return value |
| --- | --- | --- |
| `codec.digestString(algorithm, text)` | Algorithm name, text to hash | Digest byte list |
| `codec.digestBytes(algorithm, values)` | Algorithm name, byte list | Digest byte list |

`algorithm` is case-insensitive and supports `MD5`, `SHA`, `SHA1`, `SHA256`, and `SHA512`. `SHA` and `SHA1` produce the same result. Use the names listed here, such as `SHA256` rather than `SHA-256`. Text uses the runtime's default character set.

Digest functions return byte lists. Use `convert.byteToHex(...)` to obtain an uppercase hexadecimal string.

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

Result:

```json
{
  "md5": "B86FC6B051F63D73DE262D4C34E3A0A9",
  "sha1": "06D945942AA26A61BE18C3E22BF19BBCA8DD2B5D",
  "sha256": "38164FBD17603D73F696B8B4D72664D735BB6A7C88577687FD2AE33FD6964153",
  "sha256Bytes": "38164FBD17603D73F696B8B4D72664D735BB6A7C88577687FD2AE33FD6964153"
}
```

With a valid algorithm, `null` content returns `null`, while empty content is hashed normally. For example, `convert.byteToHex(codec.digestString('MD5', ''))` returns `D41D8CD98F00B204E9800998ECF8427E`. Unsupported algorithms cause the call to fail, including when content is `null`.

## HMAC

| DataQL call | Arguments | Return value |
| --- | --- | --- |
| `codec.hmacString(algorithm, key, text)` | Algorithm name, key text, text to sign | Base64 signature string |
| `codec.hmacBytes(algorithm, key, values)` | Algorithm name, key text, byte list to sign | Base64 signature string |

Supported algorithms are `HmacMD5`, `HmacSHA1`, `HmacSHA256`, and `HmacSHA512`; names are case-insensitive. `key` is a nonempty key string. Both the key and the text passed to `hmacString` use the runtime's default character set.

```javascript
import 'net.hasor.dataql.host.function.encryt.CodecUdfSource' as codec;
var bytes = codec.decodeBytes('SGVsbG8gRGF0YVFM');
return {
    'textSignature': codec.hmacString('HmacSHA256', 'example-key', 'Hello DataQL'),
    'bytesSignature': codec.hmacBytes('HmacSHA256', 'example-key', bytes)
};
```

Result:

```json
{
  "textSignature": "G4KRO7PmODFcmutDnoxR+c+1c+ZuBrI3BS9Wacso804=",
  "bytesSignature": "G4KRO7PmODFcmutDnoxR+c+1c+ZuBrI3BS9Wacso804="
}
```

The HMAC result is already a Base64 string and can be stored or transmitted directly. For a hexadecimal representation, decode the signature with `codec.decodeBytes(...)`, then convert it with `convert.byteToHex(...)`.

With a valid algorithm, `null` content returns `null`, while empty content is signed normally. Unsupported algorithms cause the call to fail, including when content is `null`.

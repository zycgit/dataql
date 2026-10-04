---
id: web
title: 7.9 Web 函数
---

:::info 依赖模块
本库由 `dataway-embedded` 模块提供，不属于 `dataql-engine` 内置函数。使用前需引入该模块，并在 Dataway HTTP 请求中执行正文、Header 和 Cookie 相关函数。

仅导入函数库不会创建 HTTP 上下文：无请求上下文时，正文和单值读取返回 `null`，列表和 Map 返回空集合；写入响应需要当前 HTTP 响应可用且尚未开始输出。`uploadFileInfo(file)` 还需要实际上传得到的文件对象。
:::

使用以下 DataQL 语句导入函数库：

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {"body": web.jsonBody()};
```

## 函数列表

下表省略导入别名 `web.`。`name` 为名称字符串，`map` 为以名称为键的对象；所有响应写入函数成功时返回 `true`，参数非法或响应不可写时抛出异常。

| DataQL 调用 | 入参和返回效果 |
| --- | --- |
| `jsonBody()` | 无参数，返回已解析的业务正文对象，不是 JSON 字符串 |
| `uploadFileInfo(file)` | 接收上传文件对象，返回名称、大小、内容类型和 SHA-256 |
| `header(name)` | 请求头的首个值；缺失时返回 `null` |
| `headerArray(name)` | 请求头的全部值列表；缺失时返回 `[]` |
| `headerMap()` | 无参数，返回请求头名称到首值的 Map |
| `headerArrayMap()` | 无参数，返回请求头名称到值列表的 Map |
| `cookie(name)` | Cookie 的首个值；缺失时返回 `null` |
| `cookieArray(name)` | 同名 Cookie 的全部值列表；缺失时返回 `[]` |
| `cookieMap()` | 无参数，返回 Cookie 名称到首值的 Map |
| `cookieArrayMap()` | 无参数，返回 Cookie 名称到值列表的 Map |
| `setHeader(name, value)` | 将非 null 的 `value` 转为字符串，替换该响应头的全部值 |
| `setHeaderAll(map)` | 按 Map 的键值逐个替换响应头 |
| `addHeader(name, value)` | 将非 null 的 `value` 转为字符串，追加一个响应头值 |
| `addHeaderAll(map)` | 批量追加响应头；值为列表时逐项追加 |
| `setCookie(name, value)`、`setCookie(name, value, options)` | 写入响应 Cookie；`value` 不能为 null，`options` 为可选属性 Map |
| `removeCookie(name)`、`removeCookie(name, options)` | 写入空值、`Max-Age=0` 的响应 Cookie；`options` 指定删除范围等属性 |

### 读取正文

假定 URL 包含 `?page=2`，JSON 正文为 `{"name":"Ada","tags":["java","dataql"]}`：

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {
    "body": web.jsonBody(),
    "page": ${page}
};
```

返回 `{"body":{"name":"Ada","tags":["java","dataql"]},"page":"2"}`。`jsonBody()` 不合并 query 参数，普通参数通过 `${name}` 读取。Perform、Smoke 中它返回模拟的业务正文；Header、Cookie 来自本次调试请求，写入作用于本次响应。

### 读取 Header

假定请求携带 `X-Tag: java` 和 `X-Tag: dataql`：

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
var headers = web.headerMap();
var headerArrays = web.headerArrayMap();
return {
    "first": web.header('X-TAG'),
    "all": web.headerArray('x-tag'),
    "fromMap": headers['x-tag'],
    "fromArrayMap": headerArrays['x-tag'],
    "missing": web.header('X-Missing')
};
```

返回 `{"first":"java","all":["java","dataql"],"fromMap":"java","fromArrayMap":["java","dataql"],"missing":null}`。名称忽略大小写，HTTP 入口的 Map 键名为小写；Map 也包含其他可见请求头。HTTP 入口不暴露原始 Authorization 和 Cookie 头，Cookie 使用专用函数读取。

### 写入 Header

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
run web.setHeader('X-Tag', 'java');
run web.addHeader('X-Tag', 'dataql');
run web.setHeaderAll({'X-Count': 2, 'X-Ready': true});
var written = web.addHeaderAll({'X-Count': [3, 4], 'X-Source': 'script'});
return {"written": written};
```

返回 `{"written":true}`；响应头 `X-Tag` 有 `java`、`dataql` 两个值，`X-Count` 有 `2`、`3`、`4` 三个值，`X-Ready` 为 `true`，`X-Source` 为 `script`。后续 `setHeader` 会覆盖同名头此前的全部值。`setHeaderAll` 不展开列表，写多值头请用 `addHeaderAll`。

### 读取 Cookie

假定请求携带 `Cookie: theme=dark; theme=light; token=a%2Bb`：

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {
    "first": web.cookie('theme'),
    "all": web.cookieArray('theme'),
    "map": web.cookieMap(),
    "arrayMap": web.cookieArrayMap()
};
```

返回：

```json
{
    "first": "dark",
    "all": ["dark", "light"],
    "map": {"theme": "dark", "token": "a%2Bb"},
    "arrayMap": {"theme": ["dark", "light"], "token": ["a%2Bb"]}
}
```

名称优先精确匹配，没有匹配时再忽略大小写；不同大小写的 Cookie 名称不会合并。值不自动 URL 解码。

### 写入和删除 Cookie

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
run web.setCookie('theme', 'dark', {
    'path': '/', 'maxAge': 3600, 'httpOnly': true, 'sameSite': 'Lax'
});
var removed = web.removeCookie('oldTheme', {'path': '/settings'});
return {"removed": removed};
```

返回 `{"removed":true}`，响应包含：

```http
Set-Cookie: theme=dark; Path=/; Max-Age=3600; HttpOnly; SameSite=Lax
Set-Cookie: oldTheme=; Path=/settings; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT
```

省略属性可写成 `web.setCookie('theme', 'dark')`、`web.removeCookie('oldTheme')`，默认路径为 `/`。写入响应不会改变本次请求中 `cookie()` 的读取结果。

## Cookie 选项

`setCookie` 和 `removeCookie` 共用以下 `options`：

| 选项 | 类型、默认值和效果 |
| --- | --- |
| `path` | 字符串，默认 `/`；设为 `null` 时不输出 Path |
| `domain` | 字符串，默认不指定 Domain |
| `maxAge` | 整数，单位秒；默认不指定，使用会话 Cookie；`0` 表示删除，负数不输出 Max-Age |
| `secure` | 布尔值，默认 `false`；`true` 时输出 Secure |
| `httpOnly` | 布尔值，默认 `false`；`true` 时输出 HttpOnly |
| `sameSite` | 字符串，默认不指定，可为 `Lax`、`Strict`、`None` |

选项键和 `sameSite` 值忽略大小写，不支持的选项会报错。`SameSite=None` 要求 `secure=true`。删除时应使用原 Cookie 的 `path`、`domain`；`removeCookie` 总会将 `maxAge` 设为 `0`。

Cookie 值不自动编码，含空格、分号或非 ASCII 字符等内容需先编码后传入，否则写入失败。

## 上传文件 {#file-info}

`uploadFileInfo(file)` 接收 multipart 上传得到的文件对象（不是文件名或本地路径），返回：

| 字段 | 含义 |
| --- | --- |
| `name` | 客户端提供的文件名 |
| `size` | 文件大小，单位字节 |
| `contentType` | 文件内容类型，未提供时为 `null` |
| `sha256` | 文件内容的 SHA-256，使用小写十六进制表示 |

例如，字段 `file` 上传 `upload.txt`，内容类型为 `text/plain`，内容为 `upload bytes`（12 字节，无换行）：

```javascript title="读取上传文件"
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {"file": web.uploadFileInfo(${file})};
```

返回：

```json
{
    "file": {
        "name": "upload.txt",
        "size": 12,
        "contentType": "text/plain",
        "sha256": "011364cdc7994ee7dfb266fd36a73e175b125f76292bb8ca47351e33086be044"
    }
}
```

同名上传两个文件时，参数为列表，可逐项调用：

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
var files = ${files};
return {"files": [web.uploadFileInfo(files[0]), web.uploadFileInfo(files[1])]};
```

函数读取完整内容计算摘要，调用后文件仍可在本次请求中使用，请求结束后统一清理。提交方式见[请求参数：文件上传](../../dataway/capabilities/development/request.md#file-upload)。使用 `return ${file};` 可直接返回原始字节及下载信息，见[二进制响应](../../dataway/capabilities/development/response.md#binary-response)。

## 二进制内容 {#binary}

转换函数库的 [`textToByte(text[, charset])`](convert.md#texttobyte) 将文本转换为二进制内容，默认 UTF-8；转换本身无需 HTTP 上下文。以下脚本在 Dataway 中返回可下载的文本：

```javascript title="下载文本"
import 'net.hasor.dataway.function.WebUdfSource' as web;
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
run web.setHeader('Content-Disposition', 'attachment; filename=hello.txt');
return convert.textToByte('Hello Dataway');
```

响应正文为 `Hello Dataway` 的 13 个 UTF-8 字节，下载文件名为 `hello.txt`。

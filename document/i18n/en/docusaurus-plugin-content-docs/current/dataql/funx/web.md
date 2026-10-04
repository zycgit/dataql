---
id: web
title: 7.9 Web functions
---

:::info Module dependency
This library is supplied by `dataway-embedded`; it is not built into `dataql-engine`. Add that module and execute body, Header, and Cookie functions within a Dataway HTTP request.

Importing the library does not create an HTTP context. Without a request context, body and single-value reads return `null`, while list and Map reads return empty collections. Response writes require the current HTTP response to be available and not yet started. `uploadFileInfo(file)` also requires an actual uploaded file object.
:::

Import the library with this DataQL statement:

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {"body": web.jsonBody()};
```

## Function list {#函数列表}

The table omits the import alias `web.`. `name` is a name string and `map` is an object keyed by names. All response-writing functions return `true` on success and throw an exception for invalid arguments or an unwritable response.

| DataQL call | Arguments and result |
| --- | --- |
| `jsonBody()` | No arguments; returns the parsed business body object, not a JSON string |
| `uploadFileInfo(file)` | Takes an uploaded file object; returns its name, size, content type, and SHA-256 |
| `header(name)` | First request-header value, or `null` when absent |
| `headerArray(name)` | List of all request-header values, or `[]` when absent |
| `headerMap()` | No arguments; returns a Map from request-header names to first values |
| `headerArrayMap()` | No arguments; returns a Map from request-header names to value lists |
| `cookie(name)` | First Cookie value, or `null` when absent |
| `cookieArray(name)` | List of all same-name Cookie values, or `[]` when absent |
| `cookieMap()` | No arguments; returns a Map from Cookie names to first values |
| `cookieArrayMap()` | No arguments; returns a Map from Cookie names to value lists |
| `setHeader(name, value)` | Converts non-null `value` to a string and replaces all values of that response header |
| `setHeaderAll(map)` | Replaces response headers using each Map entry |
| `addHeader(name, value)` | Converts non-null `value` to a string and appends one response-header value |
| `addHeaderAll(map)` | Appends response headers in bulk; list values are appended one item at a time |
| `setCookie(name, value)` / `setCookie(name, value, options)` | Writes a response Cookie; `value` must be non-null and `options` is an optional attribute Map |
| `removeCookie(name)` / `removeCookie(name, options)` | Writes an empty Cookie with `Max-Age=0`; `options` specifies scope and other attributes |

### Reading the body

Suppose the URL contains `?page=2` and the JSON body is `{"name":"Ada","tags":["java","dataql"]}`:

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {
    "body": web.jsonBody(),
    "page": ${page}
};
```

Returns `{"body":{"name":"Ada","tags":["java","dataql"]},"page":"2"}`. `jsonBody()` does not merge query parameters; read ordinary parameters with `${name}`. In Perform and Smoke, it returns the simulated business body. Headers and Cookies come from the current debugging request, and writes affect its response.

### Reading Headers

Suppose the request contains `X-Tag: java` and `X-Tag: dataql`:

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

Returns `{"first":"java","all":["java","dataql"],"fromMap":"java","fromArrayMap":["java","dataql"],"missing":null}`. Names are case-insensitive; Map keys from the HTTP entry point are lowercase. Maps also contain other visible request headers. The HTTP entry point hides raw Authorization and Cookie headers; use the Cookie functions instead.

### Writing Headers

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
run web.setHeader('X-Tag', 'java');
run web.addHeader('X-Tag', 'dataql');
run web.setHeaderAll({'X-Count': 2, 'X-Ready': true});
var written = web.addHeaderAll({'X-Count': [3, 4], 'X-Source': 'script'});
return {"written": written};
```

Returns `{"written":true}`. The response contains `X-Tag` values `java` and `dataql`, `X-Count` values `2`, `3`, and `4`, `X-Ready: true`, and `X-Source: script`. A later `setHeader` replaces all previous values with that name. `setHeaderAll` does not expand lists; use `addHeaderAll` for multiple values.

### Reading Cookies

Suppose the request contains `Cookie: theme=dark; theme=light; token=a%2Bb`:

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {
    "first": web.cookie('theme'),
    "all": web.cookieArray('theme'),
    "map": web.cookieMap(),
    "arrayMap": web.cookieArrayMap()
};
```

Returns:

```json
{
    "first": "dark",
    "all": ["dark", "light"],
    "map": {"theme": "dark", "token": "a%2Bb"},
    "arrayMap": {"theme": ["dark", "light"], "token": ["a%2Bb"]}
}
```

Lookup prefers an exact name, then falls back to a case-insensitive match. Names with different casing are not merged. Values are not automatically URL-decoded.

### Writing and removing Cookies

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
run web.setCookie('theme', 'dark', {
    'path': '/', 'maxAge': 3600, 'httpOnly': true, 'sameSite': 'Lax'
});
var removed = web.removeCookie('oldTheme', {'path': '/settings'});
return {"removed": removed};
```

Returns `{"removed":true}` and adds these response headers:

```http
Set-Cookie: theme=dark; Path=/; Max-Age=3600; HttpOnly; SameSite=Lax
Set-Cookie: oldTheme=; Path=/settings; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT
```

You can omit attributes with `web.setCookie('theme', 'dark')` or `web.removeCookie('oldTheme')`; the default path is `/`. Writing a response Cookie does not change what `cookie()` reads from the current request.

## Cookie options {#cookie-选项}

`setCookie` and `removeCookie` share these `options`:

| Option | Type, default, and effect |
| --- | --- |
| `path` | String, default `/`; `null` omits Path |
| `domain` | String; Domain is omitted by default |
| `maxAge` | Integer in seconds; omitted by default for a session Cookie; `0` deletes it, and a negative value omits Max-Age |
| `secure` | Boolean, default `false`; `true` adds Secure |
| `httpOnly` | Boolean, default `false`; `true` adds HttpOnly |
| `sameSite` | String, omitted by default; accepts `Lax`, `Strict`, or `None` |

Option keys and `sameSite` values are case-insensitive. Unsupported options cause an error. `SameSite=None` requires `secure=true`. To remove a Cookie, use its original `path` and `domain`; `removeCookie` always sets `maxAge` to `0`.

Cookie values are not automatically encoded. Values containing spaces, semicolons, non-ASCII characters, or other unsupported characters must be encoded before writing, otherwise the write fails.

## Uploaded files {#file-info}

`uploadFileInfo(file)` accepts a file object from a multipart upload, not a filename or local path. It returns:

| Field | Meaning |
| --- | --- |
| `name` | Filename supplied by the client |
| `size` | File size in bytes |
| `contentType` | File content type, or `null` if unspecified |
| `sha256` | SHA-256 of the file contents in lowercase hexadecimal |

For example, upload `upload.txt` in the `file` field, with content type `text/plain` and content `upload bytes` (12 bytes, no newline):

```javascript title="Inspect an uploaded file"
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {"file": web.uploadFileInfo(${file})};
```

Returns:

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

When two files share the same field name, the parameter is a list. Inspect each item separately:

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
var files = ${files};
return {"files": [web.uploadFileInfo(files[0]), web.uploadFileInfo(files[1])]};
```

The function reads the full contents to compute the digest. The file remains usable during the current request and is cleaned up when the request ends. See [request parameters: file uploads](../../dataway/capabilities/development/request.md#file-upload) for submission examples. Use `return ${file};` to return its original bytes and download metadata; see [binary responses](../../dataway/capabilities/development/response.md#binary-response).

## Binary content {#binary}

The conversion library's [`textToByte(text[, charset])`](convert.md#texttobyte) converts text to binary content, using UTF-8 by default. Conversion itself needs no HTTP context. This Dataway script returns a downloadable text file:

```javascript title="Download text"
import 'net.hasor.dataway.function.WebUdfSource' as web;
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
run web.setHeader('Content-Disposition', 'attachment; filename=hello.txt');
return convert.textToByte('Hello Dataway');
```

The response body contains the 13 UTF-8 bytes of `Hello Dataway`, with download filename `hello.txt`.

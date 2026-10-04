---
title: "5.2.2 Request parameters"
description: "Submit and read URL parameters, JSON, forms, uploads, headers and cookies."
---

Dataway merges URL query parameters and the body into script parameters: DataQL reads `${name}`, SQL binds `#{name}`, and Web functions read headers and cookies. Save and publish the example APIs before calling them. `cookies.txt` contains cookies saved after application login.

## URL parameters {#url-parameters}

Pass parameters in the query string:

```http title="Query parameters"
GET /api/search?tag=java&tag=dataql&name=Ada HTTP/1.1
Host: 127.0.0.1:8080
```

Values are URL-decoded into strings; repeated names produce lists. Both `name=` and a bare `name` produce an empty string:

```javascript title="Read query parameters"
return {"name": ${name}, "tags": ${tag}};
```

Here, `${name}` contains `"Ada"`, and `${tag}` contains `["java", "dataql"]`. For GET and HEAD, the console converts scalar Parameters values into a query string.

## POST JSON {#post-json}

Use `Content-Type: application/json` for numbers, booleans, nested objects and arrays:

```bash title="Submit JSON"
curl -b cookies.txt http://127.0.0.1:8080/api/search \
  -H 'Content-Type: application/json' \
  -d '{"id":1,"active":true,"filter":{"name":"Ada"},"tags":["java","dataql"]}'
```

```javascript title="Read JSON parameters"
return {
    "id": ${id},
    "active": ${active},
    "name": ${filter.name},
    "tags": ${tags}
};
```

The JSON root must be an object, field values retain their types, and an empty body becomes an empty object. SQL binds `#{id}` and `#{filter.name}`; `web.jsonBody()` retrieves the parsed body. See [Web functions](../../../dataql/funx/web.md).

The console's Parameters editor saves JSON examples for debugging and documentation. SQL mode uses their top-level keys to declare parameters; execution uses the current request's values.

## Forms {#forms}

Standard forms use `application/x-www-form-urlencoded`; curl's `--data-urlencode` encodes fields and sets Content-Type:

```bash title="Submit a form"
curl -b cookies.txt http://127.0.0.1:8080/api/form \
  --data-urlencode 'name=Ada' \
  --data-urlencode 'tag=java' \
  --data-urlencode 'tag=dataql'
```

```javascript title="Read form fields"
return {"name": ${name}, "tags": ${tag}};
```

Form values are strings, repeated names produce lists, and empty values remain `""`. Here, `${tag}` contains `["java", "dataql"]`. `multipart/form-data` carries text and files together. The console's Parameters panel sends JSON; HTTP clients and Swagger UI support form submission.

## File uploads {#file-upload}

All three framework [examples](https://gitee.com/zycgit/dataql/tree/dev/example) provide `POST /upload` with `title` and `file`. Submit with curl, or expand `/upload` in Swagger UI and click Try it out to select a file:

```bash title="Submit a file and text field"
curl -b cookies.txt http://127.0.0.1:8080/api/upload \
  -F 'title=Quarterly report' \
  -F 'file=@report.csv'
```

The client generates the multipart boundary. The script reads text from `${title}` and file metadata through `web.uploadFileInfo`:

```javascript title="upload.dql"
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {
    "title": ${title},
    "file": web.uploadFileInfo(${file})
};
```

`${file}` is a `net.hasor.dataway.function.WebFile`. `web.uploadFileInfo(file)` returns its name, size, content type and SHA-256; see [Web functions](../../../dataql/funx/web.md#file-info). WebFile provides `getName()`, `getSize()` and `getContentType()` for metadata, `openStream()` for reading, and `writeTo(output)` for copying. Files can be read repeatedly until the request ends.

### Multiple files and fields

A single file is a WebFile; repeated files produce `List<WebFile>`, and repeated text fields produce string lists. Empty files remain present with size 0.

```bash title="Multiple files with the same field name"
curl -b cookies.txt http://127.0.0.1:8080/api/batch-upload \
  -F 'files=@a.csv' -F 'files=@b.csv' \
  -F 'tag=finance' -F 'tag=monthly'
```

Create `/batch-upload` separately, call `web.uploadFileInfo(file)` for each item in `${files}`, and read text fields from `${tag}`.

### Buffering and cleanup

Files remain in memory until they exceed `uploadMemoryThreshold`, then spill into `uploadTempDirectory`; see [Upload configuration](../../configuration/core.md#upload). Request completion closes streams and deletes temporary files, including after failures. Persist required content beforehand. The host web framework limits total upload size.

Return `return ${file};` to download the uploaded bytes with their content type and filename. The request cleans up cached files after output completes. See [binary responses](response.md#binary-response).

## Headers {#headers}

Send business identifiers such as a tenant and tags in request headers:

```bash title="Send headers"
curl -b cookies.txt -X POST http://127.0.0.1:8080/api/headers \
  -H 'X-Tenant: demo' \
  -H 'X-Tag: java' \
  -H 'X-Tag: dataql'
```

```javascript title="Read headers"
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {
    "tenant": web.header('x-tenant'),
    "tags": web.headerArray('x-tag')
};
```

Header names are case-insensitive. `header(name)` returns the first value and `headerArray(name)` returns all values; missing names yield null and an empty list. `headerMap()` and `headerArrayMap()` return corresponding maps. Results exclude Authorization and Cookie headers; dedicated functions read cookies. The console sends selected Headers rows. The editor saves examples; list-panel changes affect only the current call.

## Cookies {#cookies}

Browsers send cookies according to domain, path and SameSite attributes. This example sets a cookie and calls an API from an authenticated same-origin page:

```javascript title="Call with a cookie"
document.cookie = 'theme=dark; Path=/; SameSite=Lax';
const response = await fetch('/api/preferences', {
    method: 'POST',
    credentials: 'same-origin'
});
console.log(await response.json());
```

```javascript title="Read cookies"
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {"theme": web.cookie('theme')};
```

`cookie(name)` returns the first value and `cookieArray(name)` returns repeated values; missing names yield null and an empty list. Names match exactly first, then case-insensitively; values retain their original encoding. The console uses browser cookies. See [Web functions](../../../dataql/funx/web.md) for setting and removing response cookies.

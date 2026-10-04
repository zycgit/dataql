---
title: "5.2.2 请求参数"
description: "介绍 URL、JSON、表单、文件上传、Header 和 Cookie 的提交与读取方式。"
---

Dataway 将 URL 查询参数和正文合并为脚本参数，DataQL 用 `${name}` 读取，SQL 用 `#{name}` 绑定值；Header 和 Cookie 通过 Web 函数读取。示例接口需先保存并发布，`cookies.txt` 为应用登录后保存的 Cookie 文件。

## URL 参数 {#url-parameters}

通过查询串传递参数，例如：

```http title="查询参数"
GET /api/search?tag=java&tag=dataql&name=Ada HTTP/1.1
Host: 127.0.0.1:8080
```

参数经 URL 解码后，单值为字符串，同名多值为列表，`name=` 或单独的 `name` 为空字符串：

```javascript title="读取查询参数"
return {"name": ${name}, "tags": ${tag}};
```

上例中 `${name}` 为 `"Ada"`，`${tag}` 为 `["java", "dataql"]`。控制台调用 GET、HEAD 接口时，将 Parameters 中的标量值转换为查询串。

## POST JSON {#post-json}

正文使用 `Content-Type: application/json`，支持数字、布尔值、嵌套对象和数组：

```bash title="提交 JSON"
curl -b cookies.txt http://127.0.0.1:8080/api/search \
  -H 'Content-Type: application/json' \
  -d '{"id":1,"active":true,"filter":{"name":"Ada"},"tags":["java","dataql"]}'
```

```javascript title="读取 JSON 参数"
return {
    "id": ${id},
    "active": ${active},
    "name": ${filter.name},
    "tags": ${tags}
};
```

JSON 顶层须为对象，字段值保留原有类型，空正文按空对象处理。SQL 使用 `#{id}`、`#{filter.name}` 绑定值；`web.jsonBody()` 获取已解析的完整正文，见 [Web 函数库](../../../dataql/funx/web.md)。

控制台 Parameters 保存 JSON 参数样例，供调试和文档使用；SQL 模式用样例的顶层字段名声明参数，执行时使用实际请求值。

## 表单 {#forms}

普通表单使用 `application/x-www-form-urlencoded`，curl 的 `--data-urlencode` 自动编码字段并设置 Content-Type：

```bash title="提交表单"
curl -b cookies.txt http://127.0.0.1:8080/api/form \
  --data-urlencode 'name=Ada' \
  --data-urlencode 'tag=java' \
  --data-urlencode 'tag=dataql'
```

```javascript title="读取表单域"
return {"name": ${name}, "tags": ${tag}};
```

表单域为字符串，同名多值转为列表，空值保留为 `""`；上例中 `${tag}` 为 `["java", "dataql"]`。`multipart/form-data` 可同时提交文本和文件。控制台 Parameters 发送 JSON，表单通过 HTTP 客户端或 Swagger UI 提交。

## 文件上传 {#file-upload}

三个框架的[示例工程](https://gitee.com/zycgit/dataql/tree/dev/example)均提供 `POST /upload`，接收 `title` 和 `file`。可用 curl 提交，或在 Swagger UI 展开 `/upload`，点击 Try it out 选择文件：

```bash title="提交文件和文本域"
curl -b cookies.txt http://127.0.0.1:8080/api/upload \
  -F 'title=季度报表' \
  -F 'file=@report.csv'
```

客户端自动生成 multipart boundary。脚本通过 `${title}` 读取文本，`web.uploadFileInfo` 读取文件信息：

```javascript title="upload.dql"
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {
    "title": ${title},
    "file": web.uploadFileInfo(${file})
};
```

`${file}` 为 `net.hasor.dataway.function.WebFile`，`web.uploadFileInfo(file)` 返回文件名、大小、内容类型和 SHA-256，见 [Web 函数库](../../../dataql/funx/web.md#file-info)。WebFile 提供 `getName()`、`getSize()`、`getContentType()` 读取属性，`openStream()` 读取内容，`writeTo(output)` 复制文件；请求结束前可重复读取。

### 多文件与表单域

单文件为 `WebFile`，同名多文件为 `List<WebFile>`，重复文本域为字符串列表；空文件保留，大小为 0。

```bash title="同名多文件"
curl -b cookies.txt http://127.0.0.1:8080/api/batch-upload \
  -F 'files=@a.csv' -F 'files=@b.csv' \
  -F 'tag=finance' -F 'tag=monthly'
```

`/batch-upload` 需单独创建，脚本逐项调用 `web.uploadFileInfo(file)` 处理 `${files}`，用 `${tag}` 读取表单域。

### 缓存与清理

文件先缓存在内存，超过 `uploadMemoryThreshold` 后写入 `uploadTempDirectory`，见[上传配置](../../configuration/core.md#upload)。请求结束时关闭流并删除临时文件，执行失败也会清理；需保留的内容应提前写入业务存储。上传总大小由宿主 Web 框架限制。

脚本使用 `return ${file};` 可直接下载上传内容，保留文件类型和文件名，输出完成后清理缓存。详见[二进制响应](response.md#binary-response)。

## Header {#headers}

通过请求头传递业务标识，例如租户和标签：

```bash title="发送 Header"
curl -b cookies.txt -X POST http://127.0.0.1:8080/api/headers \
  -H 'X-Tenant: demo' \
  -H 'X-Tag: java' \
  -H 'X-Tag: dataql'
```

```javascript title="读取 Header"
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {
    "tenant": web.header('x-tenant'),
    "tags": web.headerArray('x-tag')
};
```

Header 名称忽略大小写，`header(name)` 返回首值，`headerArray(name)` 返回全部值，缺失时分别返回 null 和空列表；`headerMap()`、`headerArrayMap()` 返回对应的 Map。结果不含 Authorization 和 Cookie 头，Cookie 使用专用函数读取。控制台 Headers 中勾选的行随请求发送，编辑页可保存样例，列表页修改仅影响本次调用。

## Cookie {#cookies}

浏览器按域名、路径和 SameSite 等属性携带 Cookie。以下示例在已登录的同源页面设置 Cookie 并调用 API：

```javascript title="携带 Cookie 调用"
document.cookie = 'theme=dark; Path=/; SameSite=Lax';
const response = await fetch('/api/preferences', {
    method: 'POST',
    credentials: 'same-origin'
});
console.log(await response.json());
```

```javascript title="读取 Cookie"
import 'net.hasor.dataway.function.WebUdfSource' as web;
return {"theme": web.cookie('theme')};
```

`cookie(name)` 返回首值，`cookieArray(name)` 返回同名多值，缺失时分别返回 null 和空列表。名称优先精确匹配，再忽略大小写查找，值保留原始编码。控制台沿用浏览器的 Cookie，设置和删除响应 Cookie 见 [Web 函数库](../../../dataql/funx/web.md)。

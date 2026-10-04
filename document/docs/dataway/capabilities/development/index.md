---
slug: /dataway/capabilities/development
title: "5.2 API 发布"
hide_table_of_contents: true
description: "介绍 API 的脚本支持、请求参数、结果响应、选项配置与调用示例。"
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

每个 API 保存脚本、参数样例和执行选项。发布后，可通过 HTTP 或应用内的 `ApiService` 调用。

## 使用指引

- [脚本支持](script.md)：编写 DataQL、SQL，组合查询并转换结果。
- [请求参数](request.md)：读取 URL 参数、JSON、表单、文件上传、Header 和 Cookie。
- [结果响应](response.md)：返回 JSON、错误、二进制内容或自定义 HTTP 响应。
- [API 选项](options.md)：配置参数包装、结构化响应和响应模板。
- [Java 调用](java.md)：通过 ApiService 按路径或 API 标识调用已发布的接口。

## 调用示例 {#invoke-example}

以下示例调用已发布的 `POST /hello`，脚本为 `return {"message": ${message}};`。默认地址为 `/api/hello`，前缀见[入口配置](../../integration/buildtools.md)。

调用方携带应用登录凭据，并具有 API 访问权限。下面使用[示例工程](https://gitee.com/zycgit/dataql/tree/dev/example)的 Cookie 登录：

<Tabs groupId="http-client">
<TabItem value="curl" label="curl" default>

```bash title="登录并调用"
# 登录示例应用，保存 Cookie。
curl -c cookies.txt -X POST http://127.0.0.1:8080/session/login \
  -d 'username=api&password=example-password'

# 调用已发布的 API。
curl -b cookies.txt http://127.0.0.1:8080/api/hello \
  -H 'Content-Type: application/json' \
  -d '{"message":"Hello Dataway"}'
```

</TabItem>
<TabItem value="javascript" label="JavaScript">

```javascript title="在已登录的同源页面中调用"
const response = await fetch('/api/hello', {
    method: 'POST',
    credentials: 'same-origin',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({message: 'Hello Dataway'})
});
if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
}
const result = await response.json();
if (!result.success) {
    throw new Error(result.message);
}
console.log(result.value);
```

</TabItem>
</Tabs>

使用默认响应模板时，返回结果示例如下：

```json title="响应示例"
{
  "success": true,
  "message": "OK",
  "code": 0,
  "lifeCycleTime": 2,
  "executionTime": 1,
  "value": {"message": "Hello Dataway"}
}
```

调用方先检查 HTTP 状态，再通过 `success` 判断执行状态、`value` 获取业务数据。JavaScript 示例输出 `{"message":"Hello Dataway"}`。自定义模板或关闭 Structure 后，按接口约定读取，详见[结果响应](response.md)。

接口按方法和路径匹配。GET 与 POST 可以指向不同接口；HEAD 需要定义对应方法，响应仅包含头部。请求格式见[请求参数](request.md)。

使用反向代理时，客户端请求对外地址。跨域、登录、CSRF 和请求大小限制由宿主应用配置。

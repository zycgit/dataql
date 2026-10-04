---
slug: /dataway/capabilities/development
title: "5.2 API publication"
hide_table_of_contents: true
description: "Script support, request parameters, response formats, API options and invocation examples."
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

Each API stores a script, parameter examples and execution options. After publication, clients use its defined HTTP method and path.

## Guide

- [Script support](script.md): write DataQL and SQL, combine queries and transform results.
- [Request parameters](request.md): read URL parameters, JSON, forms, uploads, headers and cookies.
- [API responses](response.md): return JSON, errors, binary data or custom HTTP responses.
- [API options](options.md): configure parameter wrapping, structured responses and response templates.
- [Java invocation](java.md): call published, enabled APIs by path or ID through `ApiService`.

## Invocation example {#invoke-example}

These examples call a published `POST /hello` whose script is `return {"message": ${message}};`. The default URL is `/api/hello`; see [Entry configuration](../../integration/buildtools.md) for prefixes.

Callers supply application credentials with API access permission. The following uses the [example application](https://gitee.com/zycgit/dataql/tree/dev/example)'s cookie login:

<Tabs groupId="http-client">
<TabItem value="curl" label="curl" default>

```bash title="Log in and invoke"
# Log in to the example application and save its cookie.
curl -c cookies.txt -X POST http://127.0.0.1:8080/session/login \
  -d 'username=api&password=example-password'

# Call the published API.
curl -b cookies.txt http://127.0.0.1:8080/api/hello \
  -H 'Content-Type: application/json' \
  -d '{"message":"Hello Dataway"}'
```

</TabItem>
<TabItem value="javascript" label="JavaScript">

```javascript title="Call from an authenticated same-origin page"
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

The default response template produces a response such as:

```json title="Example response"
{
  "success": true,
  "message": "OK",
  "code": 0,
  "lifeCycleTime": 2,
  "executionTime": 1,
  "value": {"message": "Hello Dataway"}
}
```

Check the HTTP status, then read `success` for execution status and `value` for business data. The JavaScript example prints `{"message":"Hello Dataway"}`. Follow the API contract when using a custom template or disabling Structure; see [API responses](response.md).

Routes match both method and path. GET and POST can address separate APIs. Define HEAD explicitly; its response contains headers only. See [Request parameters](request.md) for request formats.

Use the external URL when a reverse proxy is involved. The host application configures CORS, login, CSRF and request size limits.

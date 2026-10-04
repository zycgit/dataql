---
title: "5.4 API documents"
description: "Export published APIs as OpenAPI 3.2.1 and Swagger 2.0 documents."
---

Dataway generates standard documents for published, enabled APIs, ready for tools such as Swagger UI. Republishing, disabling or deleting an API updates the document.

## Supported standards

| Standard | Default URL |
| --- | --- |
| [OpenAPI 3.2.1](https://spec.openapis.org/oas/v3.2.1.html) | `/docs/openapi.json` |
| [Swagger 2.0](https://spec.openapis.org/oas/v2.0.html) | `/docs/swagger2.json` |

Both endpoints return JSON and support GET and HEAD. Use OpenAPI for definitions Swagger 2.0 cannot represent, including `oneOf`, multi-type schemas and TRACE.

## Enable and retrieve

Enable the endpoint with `dataway.docs-enabled` and set its prefix with `dataway.docs-prefix`. For Spring:

```yaml title="application.yml"
dataway:
  docs-enabled: true
  docs-prefix: /docs
```

Requests require `Operation.DOCUMENT` permission. Download a document using cookies saved after login:

```bash title="Download the OpenAPI document"
curl -b cookies.txt http://127.0.0.1:8080/docs/openapi.json -o openapi.json
```

See [DatawayConfig](../configuration/core.md#api-documents) for title, API version and public API URL settings.

## Swagger UI preview {#swagger-ui}

All three framework [examples](https://gitee.com/zycgit/dataql/tree/dev/example) provide `/swagger/index.html`. After login, expand an API, click Try it out, enter parameters and click Execute to view the request and response.

![Swagger UI: request URL, HTTP status, JSON body and response headers](/img/dataway/quickstart-invoke.png)

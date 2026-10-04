---
title: "5.2.4 API options"
description: "Configure parameter wrapping, result handlers, response content types and response templates."
---

API options control parameter wrapping and result formatting. [DatawayConfig](../../configuration/core.md#response) supplies parameter defaults; the selected handler’s constructor supplies result-option defaults. API options take precedence.

## Options

| Option | Default | Purpose |
| --- | --- | --- |
| `wrapAllParameters` | `false` | Wrap all merged parameters in one object, for both DataQL and SQL |
| `wrapParameterName` | `root` | Name of the wrapped parameter object |
| `responseFormat` | Structure constructor defaults or built-in template | Define response fields and their values; accepts a JSON object string |
| `resultHandler` | `structure` | Select the built-in `structure`, `raw`, `csv`, `text`, `verifyCode` handler or an application-registered name |

## Configuration

Edit JSON under More Settings → API Options and click Apply Options. Options are saved with the API and apply to public calls after publication. See [Visual operations](../management.md#result-panel) for console settings. When managing APIs in code, store options in `ApiDefinition.options`. See [Programmatic management](../programmatic.md):

```json title="ApiDefinition.options"
{
  "wrapAllParameters": true,
  "wrapParameterName": "root",
  "resultHandler": "structure",
  "responseFormat": "{\"ok\":\"@resultStatus\",\"data\":\"@resultData\"}"
}
```

Boolean options accept `true` or `false`. Explicit `null`, incorrect types, invalid wrapper names and invalid templates are rejected. Omit an option to use its application default.

## Result handlers {#result-handler}

`resultHandler` specifies a handler name and defaults to `structure`. See [Result handlers](../result-handlers.md) for built-in types, content types, examples and custom extensions.

## Parameter wrapping {#parameter-wrapping}

Enable `wrapAllParameters` and set `wrapParameterName` to `root`. Clients still submit the original parameters:

```json title="Request parameters"
{"id": 1, "name": "Ada"}
```

Before execution, Dataway wraps the merged parameters:

```json title="Parameters received by the script"
{"root": {"id": 1, "name": "Ada"}}
```

DataQL reads `${root.id}`, and SQL binds `#{root.id}`. With wrapping disabled, use `${id}` and `#{id}`. See [Request processing](../../principles/index.md#request-flow) for merging rules.

Wrapper names start with an ASCII letter or underscore and contain only ASCII letters, digits and underscores. Update script parameter paths when changing the wrapper name.

## Structured responses {#response-structure}

`resultHandler=structure` is the default handler. The response template places script data in a chosen field alongside execution status and timing.

Selecting Raw Value (`resultHandler=raw`) returns the script data directly. For example, `return {"message": "Hello Dataway"};` returns:

```json title="Raw Value response"
{"message": "Hello Dataway"}
```

See [API responses](response.md) for script errors, binary data and custom responses.

## Response templates {#response-template}

`responseFormat` accepts a JSON object string used by the `structure` handler. The default template is:

```json title="Default response template"
{
  "success": "@resultStatus",
  "message": "@resultMessage",
  "location": "@blockLocation",
  "code": "@resultCode",
  "lifeCycleTime": "@timeLifeCycle",
  "executionTime": "@timeExecution",
  "value": "@resultData"
}
```

The template defines field names. Placeholders provide field values:

| Placeholder | Value |
| --- | --- |
| `@resultStatus` | Whether execution succeeded |
| `@resultMessage` | Execution message |
| `@blockLocation`, `@codeLocation` | Error location |
| `@resultCode` | Execution result code |
| `@timeLifeCycle` | Total execution duration in milliseconds |
| `@timeExecution` | Script execution duration in milliseconds |
| `@resultData` | Script return value or error data |

For example, use `ok` and `data` as response fields:

```json title="Custom response template"
{
  "ok": "@resultStatus",
  "message": "@resultMessage",
  "data": "@resultData"
}
```

Replacement applies only to top-level placeholders. Nested objects and other values remain literals. JSON responses omit fields whose values are `null`.

---
title: "5.3.1 Structure"
description: "Structure applies a response template to script execution results. It is the default Dataway result handler."
---

## Introduction

Structure applies a response template to script execution results. It is the default Dataway result handler.

## Purpose

Ordinary values use `application/json; charset=utf-8`, with success status, result code, timing and `value` by default. Script failures use the same template and default to HTTP 200.

Successful binary and `ResultInfo` results pass through directly; see [Binary responses](../development/response.md#binary-response).

## Usage

Call `POST /result-structure` with `{"message":"Hello Dataway"}`:

```javascript
return {"message": ${message}};
```

The response places script data in `value`; durations are in milliseconds:

```json title="Response body"
{
  "success": true,
  "message": "OK",
  "code": 0,
  "lifeCycleTime": 2,
  "executionTime": 1,
  "value": {"message": "Hello Dataway"}
}
```

## Configuration

Select Structure in the console or set the API option below, then save and publish:

```json title="API options"
{"resultHandler": "structure"}
```

Configure the template with `responseFormat` or edit it in the console’s Structure tab. See [API options](../development/options.md#response-template) for the default template and placeholders.

Set the application’s default template through the handler constructor:

```java
import java.util.Map;
import net.hasor.dataway.result.structure.StructureResultHandler;

String template = """
        {"ok":"@resultStatus","data":"@resultData"}
        """;
StructureResultHandler handler = new StructureResultHandler(Map.of("responseFormat", template));
config.resultHandler("structure", handler);
```

An API’s `responseFormat` overrides this default. Leaving the console’s Structure tab empty omits the option and uses the handler default.

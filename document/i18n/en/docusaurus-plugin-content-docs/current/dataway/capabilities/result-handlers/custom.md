---
title: "5.3.6 Custom result handlers"
description: "Implement `ResultHandler` to customize result formats, HTTP status and response headers."
---

## Introduction

Implement `ResultHandler` to customize result formats, HTTP status and response headers.

## Purpose

The handler receives `ResultContext` with the value, success flag, result code, message, error location, timing, exception and resolved options, then returns `ResultInfo`.

If the handler throws or returns null, Dataway produces a Structure failure response without calling the handler again.

| API | Purpose |
| --- | --- |
| `ResultInfoUtils.json(status, data)` | Set status and write JSON |
| `ResultInfoUtils.toResult(binaryModel)` | Send binary content, including upload filename and content type |
| `ResultInfoUtils.binary(contentType, bytes)` | Send bytes with a content type |
| `ResultInfoUtils.stream(contentType, stream)` | Send and close an input stream |
| `response.setStatus(status)` | Set HTTP status, default 200 |
| `response.getHeaders().put(name, value)` | Set response headers |

## Usage

This handler reads HTTP status from its resolved options, returns JSON with `X-Result: created` on success, and delegates failures to Structure:

```java title="CreatedResultHandler.java"
package example;

import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.ResultInfoUtils;
import java.util.Map;
import net.hasor.dataway.result.AbstractResultHandler;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.result.structure.StructureResultHandler;

public class CreatedResultHandler extends AbstractResultHandler {
    public CreatedResultHandler(Map<String, ?> defaults) {
        super(defaults);
    }

    @Override
    public ResultInfo handle(ResultContext context) {
        if (!context.isSuccess()) {
            return new StructureResultHandler().handle(context);
        }
        int status = ((Number) context.getOptions().get("status")).intValue();
        ResultInfo response = ResultInfoUtils.json(status, context.getValue());
        response.getHeaders().put("X-Result", "created");
        return response;
    }
}
```

## Configuration

Register the handler as `created` in the application's `DatawayConfig`:

```java title="Register the result handler"
Map<String, Object> defaults = Map.of("status", 201);
config.resultHandler("created", new CreatedResultHandler(defaults));
```

Select `created` in the API's `resultHandler` option to send its execution results to `CreatedResultHandler`:

```json title="API options"
{"resultHandler": "created", "status": 202}
```

The handler appears in the console dropdown. The API overrides the constructor's status 201 with 202. Registering the same name replaces the handler; `default` is reserved.

`AbstractResultHandler` merges and isolates options. To validate them, override `prepareOptions` and check the map returned by `super.prepareOptions(options)`.

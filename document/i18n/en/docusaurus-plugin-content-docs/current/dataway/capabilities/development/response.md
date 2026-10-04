---
title: "5.2.3 API responses"
description: "JSON, failure, binary and custom HTTP responses."
---

Dataway supports JSON, text and binary responses. Select a [result handler](../result-handlers.md) to control output formatting. Web functions set headers and cookies or return binary content.

## JSON responses

The default response puts script data in `value` and adds execution status and timing. For `return {"message": ${message}};`:

```json title="Default response"
{
  "success": true,
  "message": "OK",
  "code": 0,
  "lifeCycleTime": 2,
  "executionTime": 1,
  "value": {"message": "Hello Dataway"}
}
```

Durations are in milliseconds. JSON omits null properties. Clients read fields according to the API contract; the default template uses `success` for execution status and `value` for business data.

## Failure responses

A script can raise a business error:

```javascript title="Business validation"
if (${id} <= 0) {
    throw 422, "id must be positive";
}
return ${id};
```

The default structure contains `success: false`, `code: 422`, the error data in `value`, and the error position in `location`. Execution failures use the response template and default to HTTP 200. Clients check both HTTP status and the template’s success flag.

With Raw Value selected, non-null error data is returned directly; null error data still produces an error structure. Authorization, routing, body parsing and compilation exceptions are passed to the host.

## Headers and cookies {#response-headers}

Call Web functions directly from the script:

```javascript title="Set response headers and cookies"
import 'net.hasor.dataway.function.WebUdfSource' as web;
run web.setHeader('X-Result', 'ready');
run web.setCookie('theme', 'dark', {'path': '/', 'sameSite': 'Lax'});
return {'message': 'ready'};
```

The settings are sent with the current response. The body still follows JSON output rules. See [Web functions](../../../dataql/funx/web.md).

## Binary responses {#binary-response}

DataQL preserves binary objects through BinaryModel, including variables, parameters, collections, UDF calls and lambdas. Structure and Raw Value send binary content directly.

### Manual generation

`convert.textToByte(text)` creates UTF-8 binary content. Use Web functions for headers and cookies:

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
run web.setHeader('Content-Type', 'text/plain; charset=UTF-8');
run web.setHeader('Content-Disposition', 'attachment; filename=hello.txt');
return convert.textToByte('Hello Dataway');
```

### Return an uploaded file

Return the uploaded WebFile directly and set its response type and download filename through headers:

```javascript
import 'net.hasor.dataway.function.WebUdfSource' as web;
run web.setHeader('Content-Type', 'application/octet-stream');
run web.setHeader('Content-Disposition', 'attachment; filename="download.bin"');
return ${file};
```

Submit a multipart/form-data file named `file`. `Content-Type` specifies the response type, `attachment` triggers a download, and `filename` specifies the download name. Omitting these two header settings preserves the upload's type and name; the bytes remain unchanged. The examples provide `POST /upload-download`. Upload caches are released after the response completes.

### Application UDF

An application UDF can generate file contents and return the bytes through `BinaryValue`. This Spring configuration registers a function named `report` that produces a UTF-8 people report. If the application already defines a `DatawayConfig` bean, add the function registration to its factory method.

```java title="Register the report function"
import java.nio.charset.StandardCharsets;
import net.hasor.dataql.domain.BinaryValue;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.service.DatawayConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DatawayConfiguration {
    @Bean
    public DatawayConfig datawayConfig(IdentityProvider identityProvider) {
        DatawayConfig config = new DatawayConfig();
        config.identityProvider(identityProvider);

        // DataQL calls this function as report(); each call generates file contents.
        config.function("report", (hints, params) -> {
            String content = "id,name\n1,Alice\n2,Bob\n";
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            return new BinaryValue(bytes);
        });
        return config;
    }
}
```

`IdentityProvider` is supplied by the application; see [Spring integration](../../integration/spring.md). The callback receives execution hints in `hints` and function arguments in `params`; this example does not read them. An application service can supply the report contents, with `BinaryValue` wrapping the bytes before return.

Create a DataQL API in the console, select Structure or Raw Value, and enter this script. The registered `report` function can be called directly. Web functions set the download content type and filename:

```javascript title="Generate and download the report"
import 'net.hasor.dataway.function.WebUdfSource' as web;

var reportData = report();
run web.setHeader('Content-Type', 'text/csv; charset=UTF-8');
run web.setHeader('Content-Disposition', 'attachment; filename="people.csv"');
return reportData;
```

Save and publish the API. Calling it returns the CSV contents with the download filename `people.csv`. Structure and Raw Value both send these binary bytes directly. The response body is:

```text
id,name
1,Alice
2,Bob
```

`new BinaryValue(inputStream)` accepts a one-shot stream. Its default content type is application/octet-stream. Dataway closes the response stream after successful output, failed writes and HEAD requests. Applications close streams that are not returned as responses. Subclass BinaryModel and implement `openStream()` for lazy resource access; override `getSize()` when the length is known.

Ordinary byte arrays keep their list conversion behavior in DataQL. Use BinaryModel for explicit binary transport. Binary values nested in JSON objects require an application handler or explicit encoding.

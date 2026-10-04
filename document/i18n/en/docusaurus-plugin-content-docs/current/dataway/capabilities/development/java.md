---
title: "5.2.5 Java invocation"
description: "Invoke published, enabled APIs from Java through ApiService."
---

`ApiService` calls published, enabled APIs directly from application code, without an HTTP request. Saving a draft does not change the running API; publish the changes before invoking them.

## Prepare the API

Create and publish an API with ID `hello`, method `POST`, path `/hello`, and this script:

```javascript
return {"message": ${message}};
```

Use `{"message":"Hello Dataway"}` as its request example and keep the default Structure result handler. See [Programmatic management](../programmatic.md) for the creation and publication example.

## Invoke by path

Obtain the existing `Dataway` instance from the framework container, then call `getApiService()`:

```java title="Invoke the published API"
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.script.ApiService;

ApiService api = dataway.getApiService();
Map<String, Object> parameters = Map.of("message", "Hello Dataway");

ResultInfo result = api.invokeByPath("POST", "/hello", parameters);
```

The path is the API's configured path, without the host prefix such as `/api`. Method and path together identify the API. The parameter map contains script arguments, so `${message}` reads `Hello Dataway`.

Java invocation is an internal application call. It requires no user identity and does not call `IdentityProvider` or `AuthorizationCheck`. HTTP entry points still identify and authorize callers.

## Invoke by ID

Use the API definition ID when it is already known:

```java title="Invoke the same API by ID"
ResultInfo result = api.invokeById("hello", parameters);
```

`apiID` identifies the API, not a publication or history record. Both methods use its active published definition. An unpublished draft cannot be invoked through `ApiService`.

## Read the result

`ResultInfo` contains the status, headers and response data. It is a Java object, not a serialized HTTP response:

```java title="Read the default Structure result"
int status = result.getStatus();
Map<String, String> headers = result.getHeaders();
Map<?, ?> body = (Map<?, ?>) result.getData();

boolean success = Boolean.TRUE.equals(body.get("success"));
if (success) {
    Map<?, ?> value = (Map<?, ?>) body.get("value");
    System.out.println(value.get("message")); // Hello Dataway
} else {
    System.out.println(body.get("message"));
}
```

This cast applies to the example's default Structure output. Read other results according to the selected [result handler](../result-handlers.md); text and binary results are not maps. The caller owns and closes any returned streams or binary resources.

Java invocation uses the same API interceptor chain and result handler as HTTP invocation. Interceptors receive an anonymous identity and can identify Java calls through `context.source()`, which returns `ApiCallSource.PROGRAMMATIC`. Web functions have no HTTP request or response context. An absent, unpublished or disabled API raises `DatawayException` with status `404`.

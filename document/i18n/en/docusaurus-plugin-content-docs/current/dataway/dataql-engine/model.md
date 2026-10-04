---
title: "8.2 Data models"
description: "DataModel types, Java conversions, result access and binary resources."
---

DataQL uses `DataModel` for runtime data and query results. Java callers obtain the model through `QueryResult.getData()` and ordinary Java data through `unwrap()`.

## Model types

| Model | Type check | Content |
| --- | --- | --- |
| `ValueModel` | `isValue()` | Strings, numbers, booleans and null |
| `ListModel` | `isList()` | Ordered lists |
| `ObjectModel` | `isObject()` | Fields in insertion order |
| `UdfModel` | `isUdf()` | Callable functions |
| `BinaryModel` | `isBinary()` | Files, bytes or input streams |

## Java conversion

`DomainHelper.convertTo(value)` converts Java data into models, recursively converting fields and list elements.

| Java input | Model |
| --- | --- |
| `null`, Boolean, Number | ValueModel; numbers retain their Java type |
| Character, CharSequence, UUID, Enum | String ValueModel; enums use `name()` |
| Date | Millisecond timestamp ValueModel |
| Map, Java Bean | ObjectModel; Map keys become strings; Bean conversion reads readable properties except `class` |
| Collection, arrays | ListModel; `char[]` becomes character strings, `byte[]` becomes numbers |
| Udf | UdfModel |
| DataModel | The original instance, including BinaryModel |

Input must have no cyclic references. Map keys cannot be null; keys that become identical strings use the last value.

## Read and modify

`ObjectModel.put` and `ListModel.add` convert values automatically. `get` returns DataModel; `getValue`, `getList`, `getObject` and `getUdf` return typed models.

```java
import java.util.Map;
import net.hasor.dataql.domain.DomainHelper;
import net.hasor.dataql.domain.ListModel;
import net.hasor.dataql.domain.ObjectModel;

ObjectModel user = (ObjectModel) DomainHelper.convertTo(Map.of("name", "Alice", "age", 18));
user.put("nickname", null);
String name = user.getValue("name").asString(); // Alice
int age = user.getValue("age").asInt();         // 18
boolean empty = user.getValue("nickname").isNull(); // true
boolean absent = user.get("unknown") == null;       // true

ListModel users = new ListModel();
users.add(user);
String firstName = users.getObject(0).getValue("name").asString(); // Alice
```

- Field names are case-sensitive. Missing fields return null and fail type checks; explicit null is stored as ValueModel.
- Java list indexes start at 0; out-of-range access throws `IndexOutOfBoundsException`. The engine handles script negative indexes and [INDEX_OVERFLOW](../../dataql/hints/hint_core.md#INDEX_OVERFLOW).
- Typed getters throw `ClassCastException` on a type mismatch. Check binary values with `get(...).isBinary()`.

### Value conversion

ValueModel provides `isString()`, `isNumber()`, `isBoolean()` and `isNull()`. Conversions leave the stored value unchanged:

| Method | Behavior |
| --- | --- |
| `asString()` | Calls `toString()`; null returns null |
| `asBoolean()` | Nonzero numbers are true; strings accept case-insensitive true/false and 1/0; null returns false |
| `asNumber()` | Returns the original Number without parsing strings; null returns integer 0 |
| `asInt()`, `asLong()`, `asDouble()`, etc. | Converts numeric strings and booleans (1/0) to the target number type; null returns 0 |

Numeric checks include compatible types: Byte passes `isInt()`. For the exact Java type, check `asOri().getClass()` after excluding null. Narrowing can truncate or overflow; invalid conversions throw exceptions. Script functions are covered in [Conversion functions](../../dataql/funx/convert.md).

## Unwrap results

| Model | `asOri()` | `unwrap()` |
| --- | --- | --- |
| ValueModel | Original value | Original value |
| ListModel / ObjectModel | Internal mutable List / Map of models | New ordinary List / Map, recursively unwrapped with null preserved |
| UdfModel | Original Udf | Original Udf |
| BinaryModel | The model itself | The model itself |

Use `put` and `add` to modify models and `unwrap()` to obtain ordinary data. Unwrapping neither serializes JSON nor reads binary streams.

## Binary resources

Create binary values explicitly with `BinaryValue`; ordinary `byte[]` uses list semantics. Binary values retain their identity in parameters, variables and function calls.

```java
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.hasor.dataql.domain.BinaryValue;

try (BinaryValue content = new BinaryValue("Hello DataQL".getBytes(StandardCharsets.UTF_8));
     InputStream input = content.openStream()) {
    String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    System.out.println(text);
}
```

- `BinaryValue(byte[])`: Streams can be reopened. The original array is retained; avoid modifying it after construction.
- `BinaryValue(InputStream)`: Opens only once. `getSize()` returns -1 for unknown size.
- Extend `BinaryModel`: Implement `openStream()` and optionally override `getSize()`.

Callers close the reading stream. A closed BinaryValue cannot be reopened. Binary values do not automatically participate in arithmetic or JSON encoding. See [Result responses](../capabilities/development/response.md#binary-response) for HTTP output.

## Function models

UdfModel wraps `Udf`. Script lambdas and imported functions can be passed as values. In Java, create a model with `DomainHelper.convertTo(udf)`; `UdfModel.call(Hints, UdfParams)` converts the function result into DataModel.

The call declares `throws Throwable`; Java callers must handle or declare it. Script calls receive parameters from the engine. See [Custom functions](../engine/functions.md) for implementation and registration, and [Application object imports](../engine/imports.md) for importing Java methods.

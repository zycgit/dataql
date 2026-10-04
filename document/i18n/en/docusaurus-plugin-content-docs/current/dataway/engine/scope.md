---
title: "9.6 Custom scopes"
---

Dataway uses `${...}` to read request parameters. `CustomizeScope` lets applications supply request defaults and custom values for `@{...}` and `#{...}`. API execution and console debugging follow the same rules.

The extension boundaries are:

- `$`: supply defaults only. Matching request values override them. Dataway still merges and wraps parameters; `CustomizeScope` cannot take over or disable this processing.
- `@` and `#`: applications define all keys and values. Dataway adds no predefined fields and does not merge request parameters into them.

Applications customize the contents of these three environments. DataQL fixes the symbols and access syntax; this interface cannot introduce new symbols.

## Configure a scope

Dataway calls `findCustomizeEnvironment(symbol)` with `$`, `@` or `#` and uses the returned Map keys as parameter names. This example puts a page-size default in `$`, an application name in `@`, and a region in `#`:

```java title="ApplicationScope.java"
package com.example.dataway;

import java.util.Map;
import net.hasor.dataql.kernel.CustomizeScope;

public class ApplicationScope implements CustomizeScope {
    @Override
    public Map<String, ?> findCustomizeEnvironment(String symbol) {
        return switch (symbol) {
            case "$" -> Map.of("pageSize", 20);
            case "@" -> Map.of("application", "orders");
            case "#" -> Map.of("region", "cn");
            default -> Map.of();
        };
    }
}
```

Register it before creating Dataway:

```java title="Register the scope"
import com.example.dataway.ApplicationScope;

config.customizeScope(new ApplicationScope());
```

The scope instance is shared across queries and should be thread-safe. An unconfigured scope or a `null` callback result supplies no custom values; `$` can still read request parameters.

## Script usage

```javascript
return {
    'pageSize': ${pageSize},
    'application': @{application},
    'region': #{region}
};
```

Without request parameters, `pageSize` uses its default of `20`. Send this JSON to the API:

```json title="Request body"
{
    "pageSize": 50,
    "application": "client-app",
    "region": "us"
}
```

```json title="Script result"
{
    "pageSize": 50,
    "application": "orders",
    "region": "cn"
}
```

`${pageSize}` uses the request value `50`, overriding the default `20`. The application values read through `@{application}` and `#{region}` are unaffected by matching request keys. Read the request values `client-app` and `us` through `${application}` and `${region}`. The default Structure response puts the result above in `value`.

## Usage rules

- Matching `$` keys use this precedence: request body → URL query parameters → defaults. Explicit `null` also overrides defaults. Place trusted identity data in `@` or `#`.
- With `wrapAllParameters` enabled and wrapper name `root`, use `${root}.pageSize`. The `@` and `#` environments stay unchanged. See [API options](../capabilities/development/options.md).
- Each `@` or `#` lookup invokes the scope implementation. Expression accessors `$.name`, `#.name` and `@[0]` read the [environment stack](../../dataql/syntax/valuescope.md) and do not invoke this interface.

For SQL placeholders and passing scope values to fragments, see [SQL execution](../../dataql/sql/execute.md#sql-parameters-scope).

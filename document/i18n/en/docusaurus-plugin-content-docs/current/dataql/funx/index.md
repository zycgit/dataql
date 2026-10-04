---
id: index
slug: /dataql/funx
title: 7. Built-in libraries
---

:::info Module dependencies
Sections 7.1–7.8 are provided by `dataql-engine`. Web functions (7.9) require `dataway-embedded` and a Dataway request context. Transaction functions (7.10) require `dataql-sqlproc`, a SQL data source and a transaction provider. Add and configure each extension before using its functions.
:::

The built-in libraries provide importable DataQL functions for strings, collections, dates and format conversions. Import a library with `import`, then call `library.function(...)`.

```js
import 'net.hasor.dataql.host.function.basic.StringUdfSource' as string;
return string.toUpperCase('DataQL');
```

The result is `"DATAQL"`. See [Imports and reuse](../syntax/imports.md) for import syntax, and [Functions](../syntax/function.md) for function arguments.

In signatures, `function(value[, option])` means `option` is optional; call `function(value)` or `function(value, option)`. Examples show script return values. A result handler determines any outer HTTP response structure.

## General-purpose libraries

These libraries ship with the DataQL engine. Each reference lists its resource name, parameters, return values and examples.

- [String functions](string.md): search, substring, join and case conversion.
- [Collection functions](collect.md): filtering, sorting, grouping and object/list conversions.
- [Number functions](number.md): constrain integer values to a range.
- [Date and time functions](datetime.md): timestamps, date fields and formatting.
- [JSON functions](json.md): convert JSON text and data objects.
- [Conversion functions](convert.md): numbers, booleans, text and binary data.
- [Encoding and digest functions](codec.md): Base64, URL encoding, digests and HMAC.
- [State functions](state.md): counters and UUIDs.

## Libraries supplied by extensions

- [Web functions](web.md): Dataway headers, cookies and uploaded files; require a Web request context.
- [Transaction functions](transactions.md): SQL executor transaction boundaries and propagation; require a configured transaction provider.

Applications can add libraries using the same import and call syntax. See [Library extensions](../../dataway/engine/libraries.md).

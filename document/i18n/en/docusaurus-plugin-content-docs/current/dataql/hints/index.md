---
id: index
slug: /dataql/hints
title: 5. Execution options
---

Hints configure script execution, including index bounds, numeric precision and rounding. Use `hint NAME = value`; names are case-sensitive and values are strings, numbers, booleans or `null`.

## Set an option

Place hints at the start of a script or block:

```js
hint INDEX_OVERFLOW = 'null';
var values = [10, 20];
return values[5];
```

The result is `null`. `hint NAME;` retains external configuration. `hint NAME = null;` removes the option in the current scope and restores its default behavior.

## Scope

Blocks inherit the hints active on entry. Changes are restored on exit. Functions inherit the current execution environment when called, then apply their own hints. Returning or throwing restores the caller's options.

```js
hint INDEX_OVERFLOW = 'near';
var values = [10, 20];
var optional = () -> {
    hint INDEX_OVERFLOW = 'null';
    return values[5];
};
return [optional(), values[5]];
```

The result is `[null, 20]`. The function uses `null`; its caller continues to use `near`.

## Reference

- [Engine hints](hint_core.md): indexing, numeric computation and fragment types.
- [SQL hints](hint_sql.md): SQL data sources, execution, results and pagination.

See [Core interfaces: Hint](../../dataway/dataql-engine/core.md#hint) for application defaults.

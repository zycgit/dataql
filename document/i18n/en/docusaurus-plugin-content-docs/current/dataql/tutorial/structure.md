---
id: structure
title: 1.2 Script structure
---

A script contains optional execution hints, optional imports, then executable statements: `hint → import → statements`.

```js
hint INDEX_OVERFLOW = 'null';
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;

var values = [10, 20];
return {"count": collect.size(values), "missing": values[5]};
```

The object has `count: 2` and `missing: null`. Whether JSON output retains null fields depends on host serialization.

## Declarations and execution

- `hint` sets execution options at the beginning of a script or block; see [Execution options](../hints/index.md).
- `import` loads a library, object or script resource after top-level hints; see [Imports and reuse](../syntax/imports.md).
- `var` saves an expression result, `run` discards it, and `return` returns it.
- `{...}` groups statements in a branch or function body.

Statements run in order. Functions and fragments run when called. `return` ends the current function or, at the top level, the query. `exit` ends the entire query from inside a function. See [Control flow](../syntax/statements.md).

## Names and formatting

Keywords and variable names are case-sensitive; object keys are strings. Examples use semicolons and braces around branches and function bodies. See [Lexical structure](../syntax/lexical.md) for comments, escapes and identifier rules.

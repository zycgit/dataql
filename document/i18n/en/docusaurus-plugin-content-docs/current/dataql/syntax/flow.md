---
id: flow
title: 3. Control flow and functions
---

Control execution order and reuse logic with functions, imports and fragments. `->` defines a script function, `import` loads a resource, and `@@` calls a registered language executor.

- [Control flow](statements.md): execution, branches, assertions, return and exit.
- [Functions](function.md): parameters, anonymous functions, outer variables and recursion.
- [Imports and reuse](imports.md): libraries and other DataQL scripts.
- [Code fragments](fragment.md): executor names, parameters and batch calls.

```js
var shipping = (amount) -> {
    if (amount >= 100) {
        return 0;
    }
    return 10;
};
return shipping(80);
```

The result is `10`. Use [result transformation](transform.md) or collection functions to process list items.

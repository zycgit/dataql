---
id: imports
title: 3.3 Imports and reuse
---

`import` binds a resource to a script name used to access fields or call functions. Imports follow top-level hints and precede executable statements.

## Libraries

```js
import 'net.hasor.dataql.host.function.basic.StringUdfSource' as string;
return string.toUpperCase('DataQL');
```

The result is `"DATAQL"`. `as string` assigns a script name. See [Built-in libraries](../funx/index.md) for resource names and functions.

## Application resources

Applications can register named objects or functions. If `catalog` provides a `findById` method:

```js
import 'catalog' as catalog;
return catalog.findById(1);
```

Resource names and available methods depend on the application. See [Application object imports](../../dataway/engine/imports.md) for registration.

## DataQL scripts

`import @'path' as name` imports a DataQL resource as a callable function. The host resource loader resolves its path.

```js title="/queries/greeting.dql"
return 'Hello, DataQL';
```

```js title="Calling script"
import @'/queries/greeting.dql' as greeting;
return greeting();
```

The result is `"Hello, DataQL"`. The application must supply the resource; see [Finder](../../dataway/engine/finder.md).

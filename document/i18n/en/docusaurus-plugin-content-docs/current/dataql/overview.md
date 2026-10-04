---
id: overview
title: 1. Getting started
---

{/* llms:start */}

DataQL (Data Query Language) is a language for querying and transforming data. Variables, expressions and functions organize computation; objects, lists and transformation templates shape the output. The SQL executor queries databases, and built-in libraries provide importable data-processing functions. Applications can extend both modules.

- [Your first script](tutorial/first-query.md): return a value, calculate a result and transform a list.
- [Language basics](syntax/lexical.md): writing rules, types, variables and operators.
- [Control flow and functions](syntax/flow.md): branches, functions, imports and code fragments.
- [Data access and transformation](syntax/data.md): parameters, nested data and output structures.
- [Execution options](hints/hint_core.md): indexing and numeric precision hints.
- [SQL executor](sql/execute.md): queries, dynamic rules, pagination and transactions.
- [Built-in libraries](funx/string.md): string, collection, date and JSON functions.

Run scripts in the [Dataway console](../dataway/intro/quickstart.md) or [embed the DataQL engine](../dataway/dataql-engine/execute.md) in an application.

{/* llms:end */}

## A DataQL script

```js
var people = [{"name": "Alice", "age": 25}, {"name": "Bob", "age": 30}];
return people => [{"name", "nextAge": age + 1}];
```

```json
[{"name":"Alice","nextAge":26},{"name":"Bob","nextAge":31}]
```

`var` stores data, `=>` transforms each item using a template, and `return` supplies the query result. Variables require no type declaration. Results can be individual values, objects or lists.

## Reading order

Start here, then read language basics, control flow and functions, and data access and transformation. Use the SQL chapter for database work, the libraries for individual functions, and the Hint reference for execution options.

See the [DataQL recipes](/blog/topics/dataql-recipes) for complete data-processing examples.

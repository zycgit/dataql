---
id: first-query
title: 1.1 Your first script
---

Run each example independently; none needs a database or custom application function. In the Dataway editor, select DataQL, paste a script and execute it. The results below are script return values. With the Structure handler selected, the console puts this value in the `value` field.

## Return a value

```js
return 'Hello, DataQL';
```

The result is `"Hello, DataQL"`. Quotes delimit a string; `return` supplies the result; a semicolon separates statements.

## Variables and expressions

```js
var price = 20;
var quantity = 3;
return {"quantity": quantity, "total": price * quantity};
```

```json
{"quantity":3,"total":60}
```

`var` defines a variable, `*` multiplies values, and `{...}` builds an object. Object keys are strings; values can be variables or expressions.

## Transform a list

```js
var people = [{"name": "Alice", "age": 25}, {"name": "Bob", "age": 30}];
return people => [{"name", "nextAge": age + 1}];
```

```json
[{"name":"Alice","nextAge":26},{"name":"Bob","nextAge":31}]
```

`=> [...]` produces an output for each item. `"name"` keeps the corresponding field; `"nextAge": age + 1` calculates a new one.

## Define a function

```js
var label = (name) -> {
    return 'Hello, ' + name;
};
return label('Alice');
```

The result is `"Hello, Alice"`. `->` defines the function; names in parentheses receive its arguments.

## Import a library

```js
import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
var people = [{"name": "Alice", "age": 25}, {"name": "Bob", "age": 30}];
var selected = collect.filter(people, (person) -> {
    return person.age >= 30;
});
return selected => [{"name"}];
```

The result is `[{"name":"Bob"}]`. `import` names the library, `filter` selects items with the supplied function, and the template selects output fields.

Continue with [Script structure](structure.md). Read [Parameters and context](../syntax/valuescope.md) to supply external data.

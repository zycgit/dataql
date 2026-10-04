---
id: index
slug: /dataql/syntax
title: 2. Language basics
---

This chapter covers how to write tokens, represent values, bind variables and evaluate expressions.

- [Lexical structure](lexical.md): comments, keywords, identifiers and escapes.
- [Types and literals](types.md): booleans, numbers, strings, null, objects and lists.
- [Numbers and precision](numbers.md): bases, numeric widths and rounding.
- [Variables and assignment](setter.md): declarations, rebinding and new data structures.
- [Expressions and operators](expression.md): arithmetic, comparisons, logic, bits and conditions.

```js
var price = 20;
var quantity = 3;
var total = price * quantity;
return {"total": total, "discounted": total >= 50};
```

The result is `{"total":60,"discounted":true}`. Variables hold values, expressions calculate values, and objects organize output fields.

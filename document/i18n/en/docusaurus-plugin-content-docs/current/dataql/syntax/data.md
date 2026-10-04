---
id: data
title: 4. Data access and transformation
---

DataQL reads data through paths and shapes results with `=>`. External parameters, current transformation items and local variables have distinct access forms.

- [Fields and indexes](getter.md): object fields, list indexes, missing values and bounds.
- [Parameters and context](valuescope.md): parameter scopes such as `${name}`, and `$`, `@`, `#` in transformations.
- [Result transformation](transform.md): select, rename, calculate and nest fields.

```js
var order = {"id": 10, "customer": {"name": "Alice"}, "items": [{"price": 20, "count": 3}]};
return order => {
    "id",
    "customerName": customer.name,
    "lines": items => [{"amount": price * count}]
};
```

```json
{"id":10,"customerName":"Alice","lines":[{"amount":60}]}
```

Object templates select fields; list templates transform items. Use [collection functions](../funx/collect.md) for filtering, sorting and grouping.

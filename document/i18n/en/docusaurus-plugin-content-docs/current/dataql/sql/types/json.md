---
id: json
title: 6.7.3 JSON Serialization
---

`net.hasor.dataql.sqlproc.types.json.JsonTypeHandler` serializes objects, lists and scalar values as JSON text using JDBC `setString`. Reading parses JSON text into script-compatible objects, lists or scalars.

It uses DataQL's shared Jackson utility. No separate JSON implementation is selected. It is not the default Map or List handler, so JDBC arrays and ordinary objects are not implicitly serialized.

## Writing objects

Create this H2 table:

```sql
CREATE TABLE preferences (id INT PRIMARY KEY, document VARCHAR(4000));
```

```javascript
var save = @@insertSql(id, document)<%
    INSERT INTO preferences(id, document)
    VALUES (#{id}, #{document, jdbcType=VARCHAR,
        typeHandler=net.hasor.dataql.sqlproc.types.json.JsonTypeHandler})
%>;
return save(1, {'name':'Alice', 'tags':['java','dataql'], 'nickname':null});
```

The affected-row count is `1`; the stored text is:

```json
{"name":"Alice","tags":["java","dataql"]}
```

Null object members are omitted; null list elements are preserved. A null parameter binds SQL NULL. The string `"null"` serializes as a JSON string and is not SQL NULL.

## Reading objects

A parameter's `typeHandler` affects binding only. VARCHAR results are still strings, which can be parsed explicitly:

```javascript
import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;
var find = @@selectSql(id)<%
    SELECT document FROM preferences WHERE id = #{id}
%>;
var document = json.fromJson(find(1));
return {'name':document.name, 'firstTag':document.tags[0]};
```

The result is `{"name":"Alice","firstTag":"java"}`. The default single-row/single-column unwrapping yields the text. With unwrapping disabled, extract `document` from the row first. See [JSON functions](../../funx/json.md).

Applications can register `JsonTypeHandler` for result types; see [custom handlers](../../../dataway/engine/sql-types.md). Registration affects every matching column, so making all VARCHAR columns JSON is usually inappropriate.

## Lists and native JSON columns

After declaring `save` above, another unused ID can store a list:

```javascript
return save(2, [1, null, 3]);
```

The stored text is `[1,null,3]`.

The handler serializes values; the database still determines native JSON conversion. For PostgreSQL JSONB, use an explicit CAST and a JSONB `document` column:

```javascript
var save = @@insertSql(id, document)<%
    INSERT INTO preferences(id, document)
    VALUES (#{id}, CAST(#{document,
        typeHandler=net.hasor.dataql.sqlproc.types.json.JsonTypeHandler} AS jsonb))
%>;
return save(1, {'theme':'dark'});
```

Read `document::text` and parse it with `json.fromJson`. To store an already serialized JSON string, bind it as an ordinary string; passing it through the handler again encodes its quotes.

## Nulls and errors

SQL NULL and JSON text `null` both read as null. Invalid JSON fails parsing; blank text is not valid JSON. Column length, native JSON constraints and driver support are database-specific.

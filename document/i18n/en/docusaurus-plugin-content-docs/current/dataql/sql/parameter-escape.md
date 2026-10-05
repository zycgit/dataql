---
id: parameter-escape
title: 6.2.6 Parameter marker escaping
---

Prefix literal `?`, `:name` or `&name` in a command with a backslash. The executor removes the escaping backslash and emits the marker without consuming a parameter.

| Literal output | DataQL fragment text | Equivalent Java string |
| --- | --- | --- |
| `?` | `\?` | `"\\?"` |
| `:name` | `\:name` | `"\\:name"` |
| `&name` | `\&name` | `"\\&name"` |

Inside `<% ... %>`, write one backslash. Scripts embedded in Java strings must also follow Java string escaping.

:::info[Specialized data sources require dbVisitor drivers]

The specialized data source examples on this page use [dbVisitor JDBC drivers](https://www.dbvisitor.net/docs/drivers/about). Add the appropriate driver separately and supply its connections through `ConnectionProvider`:

- Elasticsearch: [jdbc-elastic](https://www.dbvisitor.net/docs/drivers/elastic/about).
- MongoDB: [jdbc-mongo](https://www.dbvisitor.net/docs/drivers/mongo/about).
- Redis: [jdbc-redis](https://www.dbvisitor.net/docs/drivers/redis/about).
- Milvus: [jdbc-milvus](https://www.dbvisitor.net/docs/drivers/milvus/about).

DataQL parses fragments and binds parameters. Command syntax, supported operations, and JDBC capabilities are defined by each driver's documentation. These examples do not imply support for arbitrary non-relational data sources or automatic conversion of arbitrary SQL into native commands.

:::

## Elasticsearch request paths

With the Elasticsearch JDBC adapter, query parameters in the request URL are command text:

```javascript
var save = @@updateSql(id, name)<%
    PUT /users/_doc/1\?refresh=true\&pretty
    {"id":#{id},"name":#{name}}
%>;
return save(1, 'Alice');
```

The driver receives:

```text
PUT /users/_doc/1?refresh=true&pretty
{"id":?,"name":?}
```

Values are `[1,"Alice"]`; the URL question mark does not consume a binding. This is adapter-native syntax, not a command for a relational database.

## JSON colons

Field colons immediately before `?` or `#{...}` need no escaping:

```text
{"id":?,"name":#{name}}
```

With short named parameters, separate the field colon and parameter colon with whitespace:

```text
{"id": :id,"name": :name}
```

Markers inside JSON strings remain text. Marker characters inside bound values need no template escaping.

## Redis keys and Milvus operators

```javascript
var save = @@updateSql(name)<%
    HSET user\:1 name #{name}
%>;
return save('Alice');
```

The Redis driver receives `HSET user:1 name ?` and `["Alice"]`.

Escape the literal question mark in Milvus `<?>` as `<\?>`:

```sql
SELECT * FROM users WHERE id = #{id} ORDER BY text <\?> #{query}
```

The result is `SELECT * FROM users WHERE id = ? ORDER BY text <?> ?`. These examples describe parsing; command execution and results depend on the corresponding JDBC adapter.

## XML

XML entities are decoded before SQL parsing. Write `&amp;` for an ampersand and `\&amp;` for an escaped literal ampersand:

```xml
PUT /users/_doc/1\?refresh=true\&amp;pretty
{"id":#{id}}
```

Inside CDATA, use `\&pretty` directly. CDATA handles XML syntax only; SQL parameters and rules are still parsed.

## Quotes, comments and consecutive backslashes

- Single/double quotes and SQL `--` or `/* ... */` comments preserve their contents, including backslashes and parameter markers.
- PostgreSQL `::` casts are preserved: `#{id}::bigint` generates `?::bigint`.
- Before a parameter marker, an odd number of backslashes uses the final one as an escape. An even number preserves all backslashes and still parses the marker as a parameter.

The table shows fragment source characters:

| Input | Parsed text | Binds a parameter |
| --- | --- | --- |
| `\?` | `?` | No |
| `\\?` | `\\?` | Yes |
| `\\\?` | `\\?` | No |

This backslash-removal rule applies to `?`, `:` and `&`. For fixed strings containing `#{...}`, `${...}` or `@{...}`, use SQL string literals; do not assume other markers follow the same escape behavior.

---
id: vectors
title: 6.7.6 Vector Handlers
---

Vector parameters are DataQL numeric lists. Handlers convert and bind values; the database provides dimensional constraints, indexes and similarity operations.

| Database column | Handler | Input |
| --- | --- | --- |
| PostgreSQL pgvector vector(n) | vector.PgVectorTypeHandler | Numeric list converted to Float32 |
| ClickHouse Array(Float32) | vector.ChVectorTypeHandler | Numeric list converted to Float32 |

The class prefix is `net.hasor.dataql.sqlproc.types`. These are explicit handlers and do not replace ARRAY handling for every List.

## PostgreSQL pgvector

Install and enable pgvector, then prepare a table:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE vector_documents (id INT PRIMARY KEY, embedding vector(3));
```

```javascript
var save = @@insertSql(id, embedding)<%
    INSERT INTO vector_documents(id, embedding)
    VALUES (#{id}, #{embedding,
        typeHandler=net.hasor.dataql.sqlproc.types.vector.PgVectorTypeHandler})
%>;
return save(1, [0.1, 0.2, 0.3]);
```

The affected-row count is `1`. The handler binds text such as `[0.1,0.2,0.3]` using JDBC OTHER.

Use the same handler for the query vector:

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
var find = @@selectSql(embedding)<%
    SELECT id, embedding::text AS vector_text
    FROM vector_documents
    ORDER BY embedding <-> #{embedding,
        typeHandler=net.hasor.dataql.sqlproc.types.vector.PgVectorTypeHandler}
    LIMIT 3
%>;
return find([0.1, 0.2, 0.3]);
```

Rows are ordered by distance; `vector_text` is a string. A parameter handler does not automatically read result columns. Parse the text with [json.fromJson](../../funx/json.md) to obtain a list, or configure an appropriate application result mapping.

## ClickHouse

Prepare an Array(Float32) column:

```sql
CREATE TABLE vector_documents (
    id UInt64,
    embedding Array(Float32)
) ENGINE = MergeTree ORDER BY id;
```

```javascript
var save = @@insertSql(id, embedding)<%
    INSERT INTO vector_documents(id, embedding)
    VALUES (#{id}, #{embedding,
        typeHandler=net.hasor.dataql.sqlproc.types.vector.ChVectorTypeHandler})
%>;
return save(1, [1, 0.25, 0.5]);
```

Values are normalized to Float32 and bound as JDBC ARRAY. For `SELECT embedding FROM vector_documents WHERE id = 1`, drivers reporting ARRAY metadata can use the default array handler to return a list.

## Values and precision

Every element must be a non-null number that becomes a finite Float32 value. Strings, null elements, NaN, infinity and overflow fail. Converting Double or BigDecimal to Float32 loses precision.

PgVectorTypeHandler accepts a null vector as SQL NULL. ChVectorTypeHandler rejects null vectors. Handlers do not validate dimension counts; vector(n) and other database constraints do. Whether an empty vector is valid also depends on the destination column.

ClickHouse handler-created and read JDBC arrays are freed. Applications provide connections, drivers and database extensions; the handlers do not supply them.

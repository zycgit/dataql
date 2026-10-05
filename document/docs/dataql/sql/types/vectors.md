---
id: vectors
title: 6.7.6 向量类型处理器
---

向量参数使用 DataQL 数值列表。处理器负责转换和绑定，数据库负责维度约束、索引和相似度计算。

| 数据库字段 | 处理器 | 参数格式 |
| --- | --- | --- |
| PostgreSQL pgvector 的 vector(n) | vector.PgVectorTypeHandler | 数值列表，按 Float32 转换 |
| ClickHouse Array(Float32) | vector.ChVectorTypeHandler | 数值列表，按 Float32 转换 |

完整类名前缀为 `net.hasor.dataql.sqlproc.types`。这两个处理器按需指定，不会替换所有 List 的默认 ARRAY 处理器。

## PostgreSQL pgvector

数据库需要安装并启用 pgvector 扩展，然后准备表：

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

返回影响行数 `1`。处理器将列表转换为 `[0.1,0.2,0.3]` 形式，以 JDBC OTHER 类型绑定。

可把相同处理器用于查询向量：

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

返回按距离排序的行列表，`vector_text` 为字符串。显式参数处理器不会自动接管查询列；需要脚本列表时，可对文本调用 [json.fromJson](../../funx/json.md)，或在应用中配置合适的结果映射。

## ClickHouse

准备 `Array(Float32)` 列：

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

处理器将数值统一为 Float32，通过 JDBC ARRAY 绑定。查询 `SELECT embedding FROM vector_documents WHERE id = 1` 时，支持 ARRAY 元数据的驱动可由默认数组处理器读取为列表。

## 输入与精度

每个元素必须是非空且可表示为有限 Float32 的数值。字符串、null 元素、NaN、无穷大及转换后溢出的值会被拒绝。转换为 Float32 会降低 Double、BigDecimal 的精度。

`PgVectorTypeHandler` 接受 null 向量，以 SQL NULL 绑定；`ChVectorTypeHandler` 不接受 null 向量。处理器本身不检查向量维数；PostgreSQL vector(n) 等约束由数据库校验。空列表能否存储也由目标字段决定。

ClickHouse 处理器创建和读取的 JDBC Array 都会释放。数据库连接、驱动版本和扩展安装由应用配置，处理器不提供向量数据库或 JDBC 驱动。

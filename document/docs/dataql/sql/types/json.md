---
id: json
title: 6.7.3 JSON 序列化处理器
---

`net.hasor.dataql.sqlproc.types.json.JsonTypeHandler` 将脚本对象、列表或基本值序列化为 JSON 文本，通过 JDBC `setString` 写入。读取时将 JSON 文本还原为对象、列表或基本值。

它复用 DataQL 的 Jackson JSON 工具，无需为该处理器另选 JSON 实现。处理器不按 Map 或 List 自动注册，避免把普通对象参数和 JDBC ARRAY 都当作 JSON。

## 写入对象

以下示例使用 H2 的文本列：

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

返回影响行数 `1`，`document` 中保存：

```json
{"name":"Alice","tags":["java","dataql"]}
```

对象中的 null 字段省略，列表中的 null 元素保留。参数本身为 null 时写入 SQL NULL；字符串 `"null"` 经序列化后是 JSON 字符串，不等同于 SQL NULL。

## 读取对象

占位符中的 `typeHandler` 只处理该参数。普通 VARCHAR 结果仍返回字符串，可通过 JSON 函数解析：

```javascript
import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;
var find = @@selectSql(id)<%
    SELECT document FROM preferences WHERE id = #{id}
%>;
var document = json.fromJson(find(1));
return {'name':document.name, 'firstTag':document.tags[0]};
```

返回 `{"name":"Alice","firstTag":"java"}`。单行单列默认拆包为字符串；若关闭拆包，应从返回行对象中获取 `document`。函数用法见 [JSON 函数](../../funx/json.md)。

应用也可为结果类型注册 `JsonTypeHandler`，见[自定义类型处理器](../../../dataway/engine/sql-types.md)。这会影响匹配该类型的所有列，通常不应把全站 VARCHAR 都设为 JSON。

## 列表与原生 JSON 列

将列表传给同一 `save` 片段即可保存 JSON 数组：

```javascript
return save(2, [1, null, 3]);
```

保存的文本为 `[1,null,3]`。此调用需接在前面的 `save` 声明之后，使用尚未占用的主键。

处理器负责序列化，数据库仍负责原生 JSON 字段的类型转换。PostgreSQL JSONB 可使用显式 CAST：

```javascript
var save = @@insertSql(id, document)<%
    INSERT INTO preferences(id, document)
    VALUES (#{id}, CAST(#{document,
        typeHandler=net.hasor.dataql.sqlproc.types.json.JsonTypeHandler} AS jsonb))
%>;
return save(1, {'theme':'dark'});
```

此例的 `document` 应建为 JSONB。查询时可使用 `SELECT document::text ...` 配合 `json.fromJson`。只想存原始 JSON 字符串时，使用普通字符串绑定；把已序列化字符串再交给 JSON 处理器，会再次编码引号。

## 空值与异常

SQL NULL 读取为 null，JSON 文本 `null` 也解析为 null。非法 JSON 在解析时抛出异常；空白文本不是合法 JSON。文本列长度、原生 JSON 约束以及驱动支持由数据库决定。

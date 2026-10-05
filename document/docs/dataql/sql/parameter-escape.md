---
id: parameter-escape
title: 6.2.6 参数符号转义
---

命令本身包含 `?`、`:name`、`&name` 时，在符号前加反斜杠可以保留字面文本。SQL 执行器移除用于转义的反斜杠，该符号不占用绑定参数。

| 要输出的文本 | DataQL 片段正文 | Java 字符串中的同一段文本 |
| --- | --- | --- |
| `?` | `\?` | `"\\?"` |
| `:name` | `\:name` | `"\\:name"` |
| `&name` | `\&name` | `"\\&name"` |

`<% ... %>` 内是片段原文，直接写一个反斜杠即可。通过 Java 字符串拼接脚本时，还需遵守 Java 自身的转义规则。

:::info[特殊数据源依赖 dbVisitor 驱动]

本页的特殊数据源示例使用 [dbVisitor JDBC 驱动](https://www.dbvisitor.net/docs/drivers/about)，需按数据源单独引入并通过 `ConnectionProvider` 提供连接：

- Elasticsearch：[jdbc-elastic](https://www.dbvisitor.net/docs/drivers/elastic/about)。
- MongoDB：[jdbc-mongo](https://www.dbvisitor.net/docs/drivers/mongo/about)。
- Redis：[jdbc-redis](https://www.dbvisitor.net/docs/drivers/redis/about)。
- Milvus：[jdbc-milvus](https://www.dbvisitor.net/docs/drivers/milvus/about)。

DataQL 负责解析片段和绑定参数，命令语法、支持操作及 JDBC 能力以对应驱动文档为准。这些示例不代表支持任意非关系型数据源，也不代表能将任意 SQL 转为其原生命令。

:::

## Elasticsearch 请求地址

连接使用 Elasticsearch JDBC 适配器时，URL 中的查询参数属于命令文本：

```javascript
var save = @@updateSql(id, name)<%
    PUT /users/_doc/1\?refresh=true\&pretty
    {"id":#{id},"name":#{name}}
%>;
return save(1, 'Alice');
```

驱动收到：

```text
PUT /users/_doc/1?refresh=true&pretty
{"id":?,"name":?}
```

绑定参数为 `[1, "Alice"]`。URL 中的问号不会消费 `id` 的位置。此处使用对应 JDBC 适配器的原生命令语法，普通关系数据库不能执行该命令。

## JSON 冒号与参数

MongoDB、Elasticsearch 命令中，字段冒号紧接 `?` 或 `#{...}` 时无需转义：

```text
{"id":?,"name":#{name}}
```

使用 `:name` 简写时，字段冒号和参数冒号之间留出空格：

```text
{"id": :id,"name": :name}
```

JSON 字符串内的冒号、问号属于字符串内容，不参与参数解析。参数值中的这些字符也不需要手动转义，直接绑定原值即可。

## Redis 键名和 Milvus 运算符

```javascript
var save = @@updateSql(name)<%
    HSET user\:1 name #{name}
%>;
return save('Alice');
```

Redis 驱动收到 `HSET user:1 name ?`，绑定参数为 `["Alice"]`。

Milvus 命令中的 `<?>` 包含字面问号时，写为 `<\?>`：

```sql
SELECT * FROM users WHERE id = #{id} ORDER BY text <\?> #{query}
```

生成 `SELECT * FROM users WHERE id = ? ORDER BY text <?> ?`。这些示例说明参数解析结果，数据库命令及返回值由相应 JDBC 适配器处理。

## XML 中的写法

XML 先解析实体字符，再交给 SQL 参数解析器。因此 `&` 写为 `&amp;`，字面与号的转义写为 `\&amp;`：

```xml
PUT /users/_doc/1\?refresh=true\&amp;pretty
{"id":#{id}}
```

也可将命令文本放进 CDATA，此时直接写 `\&pretty`。CDATA 只处理 XML 语法，里面的 SQL 参数和规则仍会解析。

## 引号、注释和连续反斜杠

- 单引号、双引号、`--` 和 `/* ... */` 注释内部保持原文，不执行参数绑定或上述反斜杠转义。
- PostgreSQL 的 `::` 类型转换保持原样，例如 `#{id}::bigint` 生成 `?::bigint`。
- 参数符号前连续出现奇数个反斜杠时，最后一个用于转义，其余保留；偶数个反斜杠全部保留，后面的符号继续作为参数解析。

下表表示片段原文，反斜杠个数按显示字符计算：

| 输入 | 解析后的文本 | 是否绑定参数 |
| --- | --- | --- |
| `\?` | `?` | 否 |
| `\\?` | `\\?` | 是 |
| `\\\?` | `\\?` | 否 |

这套移除反斜杠的规则针对 `?`、`:`、`&`。需要输出包含 `#{...}`、`${...}`、`@{...}` 的固定字符串时，将它们放在 SQL 字符串字面量中；不要按同样方式推断其他符号的转义行为。

---
title: "5.2.1 脚本支持"
description: "使用 DataQL 和 SQL 脚本实现 API。"
---

DataQL 负责读取参数、组合查询和转换结果。SQL 模式直接保存并执行原始 SQL，适合单次数据库操作。接口创建和定义管理见 [可视化操作](../management.md)。

脚本使用 `${name}` 读取 DataQL 参数，SQL 使用 `#{name}` 绑定值。输入由请求提供，返回值交给统一的响应处理。

## DataQL 脚本

```javascript title="问候接口"
return {"message": ${message}};
```

```json title="请求参数"
{"message": "Hello Dataway"}
```

脚本返回 `{"message":"Hello Dataway"}`，默认 Structure 将它放入响应的 `value`。语法和内置函数见 [DataQL 语言](../../../dataql/overview.md)。

## SQL 脚本

应用接入 `dataql-sqlproc` 并提供 ConnectionProvider 后，在接口中选择 SQL 类型：

```sql title="按 ID 查询人员"
SELECT id, name FROM example_people WHERE id = #{id}
```

```json title="请求样例"
{"id": 1}
```

`#{id}` 绑定实际请求的参数值。保存请求样例时包含 SQL 使用的参数名，发布接口按这些名称向 SQL 片段传参。

在 DataQL 中使用 SQL 时，通过 `@@selectSql` 定义查询片段，调用后继续处理结果。下面的脚本使用示例工程中的 `ds1` 数据源：

```javascript title="DataQL 中调用 SQL"
hint FRAGMENT_SQL_DATA_SOURCE = "ds1"

var person = @@selectSql(id)<%
    SELECT id AS "id", name AS "name"
    FROM example_people
    WHERE id = #{id}
%>;
return person(${id});
```

同一脚本可以组合多个查询。数据源选择见[数据源接入](../datasources.md)，提交与回滚见[事务函数库](../../../dataql/funx/transactions.md)。

脚本调试、保存和发布的步骤见[可视化操作](../management.md)。调试会实际执行脚本中的数据库操作。

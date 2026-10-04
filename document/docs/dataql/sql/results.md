---
id: results
title: 6.5 结果与主键
---

查询先将每行转换为按列标签组织的对象，再根据拆包选项返回列表、对象或单值。插入、更新和删除返回影响行数。

## 查询结果

| 查询结果 | `off` | `row` | `column`（默认） |
| --- | --- | --- | --- |
| 没有记录 | `[]` | `{}` | null |
| 一行一列 | 对象列表 | 对象 | 单值 |
| 一行多列 | 对象列表 | 对象 | 对象 |
| 多行 | 对象列表 | 对象列表 | 对象列表 |

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
```

需要稳定的列表结构时使用 `off`。分页的 `data()` 始终返回当前页列表。

## 列名转换

`FRAGMENT_SQL_COLUMN_CASE` 支持 `default`、`upper`、`lower`、`hump`。默认保留驱动列标签，`hump` 将下划线转为小驼峰。

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'hump';
var find = @@selectSql()<% SELECT id AS user_id, name AS user_name FROM people ORDER BY id %>;
return find();
```

结果中的字段为 `userId`、`userName`。SQL 别名优先于原列名；转换后重名的列保留首次出现的值，应通过别名消除冲突。

## 主键查询 {#select-key}

`insertXml` 可用 `selectKey` 在插入前或后执行查询，将查询结果写入片段参数。下面使用 H2 序列生成主键：

```sql
CREATE SEQUENCE people_ids START WITH 100;
```

```javascript
var add = @@insertXml(name, age)<%
    <selectKey keyProperty="newId" order="before">
        SELECT NEXT VALUE FOR people_ids
    </selectKey>
    INSERT INTO people(id, name, age) VALUES (#{newId}, #{name}, #{age})
%>;
var find = @@selectSql(name)<% SELECT id FROM people WHERE name = #{name} %>;
run add('Carol', 20);
return find('Carol');
```

`order="before"` 先查询主键再插入，`after` 在插入后查询，两次语句使用同一连接。`keyProperty` 指定写入的参数；多字段结果可用 `keyColumn` 指定列名。`insertXml` 的返回值仍是影响行数，参数写回不会自动成为脚本的返回值。

当前 JDBC 执行链未接通 `getGeneratedKeys()`。`useGeneratedKeys`、`keyProperty`、`keyColumn` Hint 虽可解析，不能据此直接取回自增主键。需要主键时使用适合数据库的 `selectKey` 或明确的查询语句。

## 指定输出

```javascript
hint bindOut = '#result-set-1';
var find = @@selectSql()<% SELECT count(*) FROM people %>;
return find();
```

`bindOut` 可在查询及通用执行中选择指定结果名；存储过程输出和多结果编号见[存储过程与多结果](procedures.md)。分页不支持同时设置 `bindOut`。

---
title: 6.3.2 规则嵌套
---

规则内容可包含其他规则，组合条件、集合和参数生成。外层先决定是否使用内容，再解析内层；`text`、`iftext` 原样输出内容，不执行内层规则。

## 条件与集合

下面在列表非空时展开 IN 条件。示例表沿用 [SQL 执行](../execute.md)中的 `people`。

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(ids)<%
    SELECT id, name FROM people WHERE enabled = 1
    @{ifand, ids != null and ids.size() > 0, id IN @{in, #{ids}}}
    ORDER BY id
%>;
return find([1, 2]);
```

生成 `WHERE enabled = 1 AND id IN (?, ?)`，参数为 `[1, 2]`。`ifand` 先检查列表，再让 `in` 展开占位符，最后补上 AND。列表为空时只保留 `enabled = 1`；需要空结果时，应在调用查询前处理空列表。

也可将完整条件放入 `in`，由外层 `and` 处理连接符：

```sql
SELECT * FROM people WHERE enabled = 1
@{and, @{in, id IN #{ids}}}
```

这种写法在空列表时内部 SQL 为空，外层也不输出。写作 `@{and, id IN @{in, #{ids}}}` 时，如果列表只有 null，外层会因参数全部为 null 省略条件；需要明确业务语义时使用 `ifand`。

## 分组条件

连接规则不改变 SQL 的 AND、OR 优先级。以下用括号将两个可选条件组成一组：

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(name, minAge)<%
    SELECT id, name FROM people WHERE enabled = 1
    @{if, name != null or minAge != null,
        AND (1 = 0
            @{or, name = #{name}}
            @{or, age >= #{minAge}}
        )
    }
    ORDER BY id
%>;
return find('Alice', 30);
```

生成 `WHERE enabled = 1 AND (1 = 0 OR name = ? OR age >= ?)`，参数为 `["Alice", 30]`。两个参数都为 null 时，外层 `if` 跳过整组内容，避免留下空括号。

## 分支与集合

`case` 只执行首个选中的分支。把空集合的行为写成明确分支，可保证它返回空结果：

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(ids)<%
    SELECT id, name FROM people WHERE enabled = 1
    @{case, ,
        @{when, ids != null and ids.size() > 0,
            @{in, AND id IN #{ids}}
        }
        @{else, AND 1 = 0}
    }
    ORDER BY id
%>;
return find([]);
```

空列表生成 `AND 1 = 0`，结果为 `[]`；传入 `[1]` 时生成 `AND id IN (?)`，绑定 1。未选中的分支不会执行其参数生成或宏查找。

## 循环与分隔符

`pairs` 的每轮模板都可以继续使用条件规则。下面用序号决定是否输出 `UNION ALL`：

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var rows = @@selectSql(names)<%
    @{pairs, #{names},
        @{iftext, i > 0, UNION ALL}
        SELECT CAST(#{v} AS VARCHAR) AS name
    }
%>;
return rows(['Alice', 'Bob']);
```

`CAST` 为 UNION 查询中的绑定参数明确 VARCHAR 类型。

生成 `SELECT CAST(? AS VARCHAR) AS name UNION ALL SELECT CAST(? AS VARCHAR) AS name`，结果为 `[{"name":"Alice"},{"name":"Bob"}]`。空列表不会生成 SQL，应在脚本中跳过调用。

## 参数生成与判空

```sql
SELECT * FROM payload_log WHERE 1 = 1
@{ifand, content != null, digest = @{md5, #{content}}}
```

`content = "abc"` 时，内层 MD5 生成一个摘要参数，外层生成 `AND digest = ?`。`content = null` 时，外层条件不成立，MD5 不执行。

若使用 `@{and, digest = @{md5, #{content}}}`，内层先把 null 转成空字符串的摘要，外层看到的是非 null 摘要，会保留该条件。需要检查原始值时，应把检查写在 `if` 或 `ifand` 的条件中。

## 使用边界

- SQL 字符串或注释内的规则不会执行，例如 `'@{uuid32}'` 是字符串字面量。
- `text`、`iftext` 中不解析占位符或内层规则。需要解析时改用 `if`。
- 动态规则中优先使用名称参数，便于嵌套和分支共享参数。
- 规则只处理自己的 SQL 片段，不能删除外部遗留的 AND、逗号或括号。将可选连接符和内容放进同一个条件分支。

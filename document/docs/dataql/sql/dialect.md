---
id: dialect
title: 6.6 分页与方言
---

分页查询按页读取记录，并返回总记录数。SQL 执行器使用数据库方言生成总数查询和分页 SQL，脚本使用同一套分页方法。以下示例使用 [SQL 执行](execute.md#示例数据)中的 `people` 表。

## 查询一页数据 {#query-page}

启用 `FRAGMENT_SQL_QUERY_BY_PAGE` 后，`selectSql` 或 `selectXml` 返回延迟执行的 `PageQuery`。设置页码和页大小，再调用 `data()`：

```javascript
hint FRAGMENT_SQL_QUERY_BY_PAGE = true;
hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1;
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql(minAge)<%
    SELECT id, name FROM people WHERE age >= #{minAge} ORDER BY id
%>;
var page = find(20);
run page.setPageInfo({'currentPage':2, 'pageSize':1});
var rows = page.data();
var info = page.pageInfo();
return {'rows':rows, 'total':info.totalCount, 'currentPage':info.currentPage};
```

```json title="脚本结果"
{"rows":[{"id":2,"name":"Bob"}],"total":2,"currentPage":2}
```

`find(20)` 创建分页对象，尚未查询数据。`data()` 执行总数查询和当前页查询，`pageInfo()` 随后读取分页信息，不重复查询。分页结果保留列表，示例关闭单行结果拆包，避免一页一行时变成单个对象。

总数 SQL 相当于对原查询计数，分页 SQL 相当于只取第二条记录；两者都保留原始条件与参数。应用不必在原 SQL 中另加 `LIMIT`、`OFFSET` 或 `ROWNUM`。

## 页码和分页参数 {#page-options}

`FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET` 默认 `0`，第一页编号为 `0`；设为 `1` 后第一页编号为 `1`。页码偏移应在声明 SQL 片段前设置。

| `setPageInfo` 参数 | 含义 |
| --- | --- |
| `currentPage` | 当前页号，按所选起始页码填写 |
| `pageSize` | 每页条数，正常分页使用正整数 |
| `totalCount` | 可选的已有总数；当前脚本入口执行 `data()` 时仍刷新总数 |

`setPageInfo` 可只更新页码或页大小，成功返回 `true`。传入空对象、`null`，或只传 `totalCount`，返回 `false`。页码小于起始页码时按第一页处理；它不会自动限制到最后一页。页大小为零或负数会停止分页截取，可能查询全部记录，因此面向外部请求时先校验并限制页大小。

下面从请求参数取得页码和页大小，约定第一页为 `1`，每页最多 `100` 条：

```javascript
hint FRAGMENT_SQL_QUERY_BY_PAGE = true;
hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1;
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var currentPage = ${page};
var pageSize = ${size};
if (currentPage == null || currentPage < 1) {
    currentPage = 1;
}
if (pageSize == null || pageSize < 1 || pageSize > 100) {
    throw 400, 'size must be between 1 and 100';
}
var find = @@selectXml(minAge)<%
    SELECT id, name FROM people
    <where><if test="minAge != null">age &gt;= #{minAge}</if></where>
    ORDER BY id
%>;
var page = find(${minAge});
run page.setPageInfo({'currentPage':currentPage, 'pageSize':pageSize});
var rows = page.data();
return {'rows':rows, 'page':page.pageInfo()};
```

传入 `{"page":1,"size":10,"minAge":25}`，得到 Alice、Bob 及分页信息。请求中的页码和页大小应使用整数。

## 分页信息 {#page-info}

`pageInfo()` 返回以下字段：

| 字段 | 含义 |
| --- | --- |
| `enable` | 页大小大于零时为 `true` |
| `pageSize` | 当前页大小 |
| `totalCount` | 满足条件的总记录数 |
| `currentPage` | 当前页号，包含起始页码偏移 |
| `recordPosition` | 当前页第一条记录的零基偏移，例如第二页、每页10条时为10 |
| `totalPage` | 当前实现的页码上界，等于实际页数加起始页码偏移 |

`totalPage` 不是始终可直接展示的“总页数”：起始页码为 `0` 时，它等于实际页数；起始页码为 `1` 时，它等于实际页数加 `1`。有数据时最后一页编号为 `totalPage - 1`。例如两条记录、每页一条、页码从 `1` 开始时，`totalPage` 为 `3`，最后一页是 `2`。需要展示总页数时，根据 `totalCount` 和 `pageSize` 计算，或减去配置的起始页码偏移。

在首次 `data()` 之前调用 `pageInfo()`，且未提供正数总记录数时，也会执行查询，包含当前页数据查询。通常先调用一次 `data()`，再读取 `pageInfo()` 即可。

## 翻页 {#navigation}

导航方法只改变页号，返回变更后的页号；获取新页仍需调用 `data()`。

| 方法 | 作用 |
| --- | --- |
| `firstPage()` | 移到第一页 |
| `previousPage()` | 移到上一页，低于起始页码时停在第一页 |
| `nextPage()` | 移到下一页，不自动限制最后一页 |
| `lastPage()` | 按已知总记录数移到最后一页 |

```javascript
hint FRAGMENT_SQL_QUERY_BY_PAGE = true;
hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1;
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql()<% SELECT id, name FROM people ORDER BY id %>;
var page = find();
run page.setPageInfo({'currentPage':1, 'pageSize':1});
var first = page.data();
run page.nextPage();
var second = page.data();
return {'first':first, 'second':second};
```

分别得到 Alice 和 Bob 两页。调用 `lastPage()` 前先执行 `data()` 或 `pageInfo()` 获取总数。超出最后一页时通常返回空结果；再次调用 `data()` 会重新查询，并非读取此前的数据缓存。

## 方言选择 {#dialect}

默认根据 JDBC URL 和驱动信息选择方言。需要显式指定时，在声明 SQL 片段前设置：

```javascript
hint FRAGMENT_SQL_PAGE_DIALECT = 'postgresql';
```

| 数据库 | 别名 |
| --- | --- |
| MySQL、MariaDB | `mysql`、`mariadb` |
| PostgreSQL、Kingbase | `postgresql`、`kingbase` |
| H2、HSQLDB | `h2`、`hsql` |
| Oracle、DB2 | `oracle`、`db2` |
| SQL Server | `sqlserver`、`jtds` |
| SQLite、Derby | `sqlite`、`derby` |
| DM、Impala、Informix、XuGu | `dm`、`impala`、`informix`、`xugu` |
| ClickHouse、Hive | `clickhouse`、`hive` |
| MongoDB | `mongo` |
| Elasticsearch 6 / 7 / 8 | `elastic6`、`elastic7`、`elastic8` |
| Milvus | `milvus` |

:::info[特殊数据源依赖 dbVisitor 驱动]

MongoDB、Elasticsearch、Milvus 分别需要引入 dbVisitor 的 [jdbc-mongo](https://www.dbvisitor.net/docs/drivers/mongo/about)、[jdbc-elastic](https://www.dbvisitor.net/docs/drivers/elastic/about)、[jdbc-milvus](https://www.dbvisitor.net/docs/drivers/milvus/about)，并通过 `ConnectionProvider` 提供对应连接。命令语法和支持范围以[驱动文档](https://www.dbvisitor.net/docs/drivers/about)为准；选择方言本身不会引入驱动或让任意 SQL 都可执行。Redis 命令示例使用 [jdbc-redis](https://www.dbvisitor.net/docs/drivers/redis/about)，当前未提供自动分页方言。

:::

Hive 使用 `LIMIT offset, rows`，需数据库支持该分页语法。

## 查询注意事项 {#notes}

- 使用稳定的 `ORDER BY`，排序字段不唯一时增加主键作为次级排序，例如 `ORDER BY age, id`。
- 总数查询和当前页查询是两条语句；并发写入时观察到的数据可能变化。需要同一事务的读一致性时，在事务回调内部调用 `data()`，并按数据库能力选择隔离级别。
- 避免在分页 SQL 后附带其他 SQL 语句。含分组、去重、联合查询等复杂语句时，先在目标数据库验证 count 和分页结果。
- 仅在需要分页的 SQL 片段范围内启用分页 Hint；其他普通查询应关闭或在未启用分页的作用域中声明。

Java 方言的实现和注册见 [SQL 方言](../../dataway/engine/sql-dialects.md)。

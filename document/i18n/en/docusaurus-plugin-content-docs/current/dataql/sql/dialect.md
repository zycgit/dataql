---
id: dialect
title: 6.6 Pagination and Dialects
---

Pagination reads a page of records and obtains the total count. The SQL executor uses a database dialect to generate count and page SQL; scripts use the same pagination methods across databases. Examples use the `people` table from [SQL Execution](execute.md#sample-data).

## Query one page {#query-page}

With `FRAGMENT_SQL_QUERY_BY_PAGE` enabled, `selectSql` and `selectXml` return a lazy `PageQuery`. Set its page number and size, then call `data()`:

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

```json title="Script result"
{"rows":[{"id":2,"name":"Bob"}],"total":2,"currentPage":2}
```

`find(20)` creates the object without querying. `data()` executes the count and page queries; the following `pageInfo()` reads metadata without querying again. The example disables single-row unpacking so a one-row page remains a list.

The count query counts the original results; the page query reads only the second record. Both preserve the original conditions and parameters. Do not add another `LIMIT`, `OFFSET`, or `ROWNUM` clause to the original SQL.

## Page numbering and options {#page-options}

`FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET` defaults to `0`, making the first page `0`. Set it to `1` for one-based page numbers, before declaring the SQL fragment.

| `setPageInfo` key | Meaning |
| --- | --- |
| `currentPage` | Page number, using the configured starting number |
| `pageSize` | Rows per page; use a positive integer for pagination |
| `totalCount` | Optional known total; the current script entry still refreshes it when `data()` runs |

`setPageInfo` can update just the page number or size and returns `true` when applied. Null, an empty object, or only `totalCount` returns `false`. Numbers below the starting page are clamped to the first page; values above the final page are not clamped. A zero or negative size disables page limiting and can fetch all records, so validate external page-size input.

This example accepts one-based page numbers and at most 100 rows per page:

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

Input `{"page":1,"size":10,"minAge":25}` returns Alice, Bob, and page metadata. Supply integer page numbers and sizes.

## Page metadata {#page-info}

`pageInfo()` returns:

| Field | Meaning |
| --- | --- |
| `enable` | `true` when the page size is positive |
| `pageSize` | Current size |
| `totalCount` | Total matching rows |
| `currentPage` | Current page number, including its starting offset |
| `recordPosition` | Zero-based row offset; page two with size 10 starts at 10 |
| `totalPage` | Current implementation's exclusive page-number bound: actual page count plus the starting offset |

`totalPage` is not always a display-ready count of pages. With zero-based numbering it equals the actual count; with one-based numbering it equals that count plus one. For nonempty results the final page number is `totalPage - 1`. For two records, size one, and one-based numbering, `totalPage` is `3` and the last page is `2`. Calculate a display page count from `totalCount` and `pageSize`, or subtract the configured starting offset.

Before the first `data()` call, `pageInfo()` also executes a query if no positive total has been supplied. That execution includes fetching page data, not just counting. Normally call `data()` once and then inspect `pageInfo()`.

## Navigation {#navigation}

Navigation methods change and return the page number. Call `data()` to fetch that page.

| Method | Effect |
| --- | --- |
| `firstPage()` | Move to the first page |
| `previousPage()` | Move back, stopping at the first page |
| `nextPage()` | Advance without enforcing a last-page limit |
| `lastPage()` | Move to the last page using the known total |

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

This returns Alice and Bob as separate pages. Obtain a total through `data()` or `pageInfo()` before calling `lastPage()`. Pages beyond the end usually return empty results. Each `data()` call queries again instead of reading an earlier cached list.

## Dialect selection {#dialect}

By default, the executor selects a dialect from the JDBC URL and driver information. To choose explicitly, set this Hint before the SQL fragment:

```javascript
hint FRAGMENT_SQL_PAGE_DIALECT = 'postgresql';
```

| Database | Alias |
| --- | --- |
| MySQL, MariaDB | `mysql`, `mariadb` |
| PostgreSQL, Kingbase | `postgresql`, `kingbase` |
| H2, HSQLDB | `h2`, `hsql` |
| Oracle, DB2 | `oracle`, `db2` |
| SQL Server | `sqlserver`, `jtds` |
| SQLite, Derby | `sqlite`, `derby` |
| DM, Impala, Informix, XuGu | `dm`, `impala`, `informix`, `xugu` |
| ClickHouse, Hive | `clickhouse`, `hive` |
| MongoDB | `mongo` |
| Elasticsearch 6 / 7 / 8 | `elastic6`, `elastic7`, `elastic8` |
| Milvus | `milvus` |

:::info[Specialized data sources require dbVisitor drivers]

MongoDB, Elasticsearch, and Milvus require dbVisitor's [jdbc-mongo](https://www.dbvisitor.net/docs/drivers/mongo/about), [jdbc-elastic](https://www.dbvisitor.net/docs/drivers/elastic/about), and [jdbc-milvus](https://www.dbvisitor.net/docs/drivers/milvus/about), respectively. Add the driver separately and supply its connections through `ConnectionProvider`. Command syntax and support boundaries follow the [driver documentation](https://www.dbvisitor.net/docs/drivers/about); selecting a dialect does not install a driver or make arbitrary SQL executable. Redis command examples use [jdbc-redis](https://www.dbvisitor.net/docs/drivers/redis/about); no automatic pagination dialect is provided for Redis.

:::

Hive uses `LIMIT offset, rows`, which must be supported by the server.

## Query considerations {#notes}

- Use a stable `ORDER BY`. Add a primary-key tiebreaker when needed, such as `ORDER BY age, id`.
- Count and page retrieval are separate statements. Concurrent writes can affect the observed results. For transactional read consistency, call `data()` inside the transaction callback and choose isolation supported by the database.
- Avoid appending additional statements to paginated SQL. Validate rewritten count and page results for grouping, distinct, unions, and other complex queries against the target database.
- Enable pagination only around fragments that need it. Disable the Hint for ordinary queries or declare them in a scope where pagination is not enabled.

For Java dialect implementation and registration, see [SQL Dialects](../../dataway/engine/sql-dialects.md).

---
id: dialect
title: 6.6 Pagination and dialects
---

With pagination enabled, a select fragment returns `PageQuery`. Set a page number and positive page size, then fetch its data.

## Query a page

```javascript
hint FRAGMENT_SQL_QUERY_BY_PAGE = true;
hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1;
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectSql()<% SELECT id, name FROM people ORDER BY id %>;
var page = find();
run page.setPageInfo({'currentPage':2, 'pageSize':1});
var rows = page.data();
var info = page.pageInfo();
return {'rows':rows, 'total':info.totalCount};
```

The result contains Bob and a total of 2. `data()` executes count and page queries; the following `pageInfo()` reuses the total. Another `data()` call queries again. Use a stable ORDER BY for pagination.

## Page controls

The page-number offset defaults to 0; set it to 1 for one-based page numbers. `setPageInfo` accepts `currentPage`, `pageSize` and optionally `totalCount`. Current script pagination still refreshes the count; supplying a total does not disable the count query.

| Method | Purpose |
| --- | --- |
| `setPageInfo({...})` | Set page parameters |
| `data()` | Fetch the current page |
| `pageInfo()` | Read metadata, fetching when the total is not yet known |
| `firstPage()`, `previousPage()` | Move to first or previous page |
| `nextPage()`, `lastPage()` | Move to next or last page |

Obtain the total before calling `lastPage()`.

## Dialects

Selection uses the JDBC URL and driver. Override it with `FRAGMENT_SQL_PAGE_DIALECT`.

| Database | Aliases |
| --- | --- |
| MySQL, MariaDB | `mysql`, `mariadb` |
| PostgreSQL, Kingbase | `postgresql`, `kingbase` |
| H2, HSQLDB | `h2`, `hsql` |
| Oracle, DB2 | `oracle`, `db2` |
| SQL Server | `sqlserver`, `jtds` |
| SQLite, Derby | `sqlite`, `derby` |
| DM, Impala, Informix, XuGu | `dm`, `impala`, `informix`, `xugu` |

A dialect rewrites count and page SQL. Validate complex queries on the target database.

See [SQL dialects](../../dataway/engine/sql-dialects.md) for custom implementation and registration.

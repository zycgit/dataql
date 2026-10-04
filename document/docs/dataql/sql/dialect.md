---
id: dialect
title: 6.6 分页与方言
---

启用分页后，`selectSql` 或 `selectXml` 返回 `PageQuery`。设置页码和每页条数，再调用 `data()` 获取列表。

## 分页查询

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

示例返回 Bob 和总记录数 2。`data()` 执行总数查询和当前页查询，随后 `pageInfo()` 读取已取得的总数，不重复执行 SQL。再次调用 `data()` 会重新查询；列表翻页应使用稳定的 `ORDER BY`。

## 页码与导航

`FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET` 默认 0，设为 1 后第一页为 1。`setPageInfo` 接受 `currentPage`、`pageSize`，并可附带 `totalCount`；当前脚本分页仍会刷新总数，不能将传入 `totalCount` 当作关闭 count 查询的开关。

| 方法 | 用途 |
| --- | --- |
| `setPageInfo({...})` | 设置页码和正整数页大小 |
| `data()` | 查询当前页 |
| `pageInfo()` | 读取分页信息；尚未取得总数时触发查询 |
| `firstPage()`、`previousPage()` | 移到第一页、上一页 |
| `nextPage()`、`lastPage()` | 移到下一页、最后一页 |

先取得总记录数再调用 `lastPage()`。分页信息包含当前页、页大小、总记录数和页码范围等字段。

## 方言选择

默认按 JDBC URL 和驱动信息选择方言。可显式指定：

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

方言负责生成 count SQL 和分页 SQL。复杂查询应结合目标数据库验证重写后的语句。

自定义方言的实现与注册见 [SQL 方言](../../dataway/engine/sql-dialects.md)。

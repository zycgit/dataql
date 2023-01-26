---
id: hint_sql
sidebar_position: 2
title: SQL HINT
description: DataQL SQL 执行器相关的 Hint。
---
# SQL 执行器相关

## FRAGMENT_SQL_COLUMN_CASE

SqlFragment 返回的列信息大小写模式：`default`、`upper`、`lower`、`hump`

| 选项值       | 默认  | 含义                                                        |
|-----------|-----|-----------------------------------------------------------|
| `default` | 是   | 全部列名保持大小写敏感，数据库什么样返回就什么样                                  |
| `upper`   |     | 全部列名保持大写，如果在转换过程中发生冲突，那么会产生覆盖问题                           |
| `lower`   |     | 全部列名保持小写，如果在转换过程中发生冲突，那么会产生覆盖问题                           |
| `hump`    |     | 全部列名做一次驼峰转换。如：`goods_id => goodsId`、`GOODS_id => goodsId` |

## FRAGMENT_SQL_DATA_SOURCE

SQL执行器使用的数据源名字，默认为：`""`。

```js title='配置多个数据源'
public class MyModule implements Module {
    public void loadModule(ApiBinder apiBinder) throws Throwable {
        DataSource defaultDs = ...;
        DataSource dsA = ...;
        DataSource dsB = ...;
        apiBinder.installModule(new JdbcModule(Level.Full, defaultDs)); // 默认数据源
        apiBinder.installModule(new JdbcModule(Level.Full, "ds_A", dsA)); // 数据源A
        apiBinder.installModule(new JdbcModule(Level.Full, "ds_B", dsB)); // 数据源B
    }
}
```

```js title='在DataQL中选择数据源'
// 如果不设置 FRAGMENT_SQL_DATA_SOURCE 使用的是 defaultDs 数据源。
// - 设置值为 "ds_A" ，使用的是 dsA 数据源。
// - 设置值为 "ds_B" ，使用的是 dsB 数据源。
hint FRAGMENT_SQL_DATA_SOURCE = "ds_A"

// 声明一个 SQL
var dataSet = @@sql() <% select * from category limit 10; %>
// 使用 特定数据源来执行SQL。
return dataSet();
```

## FRAGMENT_SQL_MULTIPLE_QUERIES

`dataql-fx` `4.1.14` 之后的新特性，SqlFragment 当遇到多条 SQL 同时执行时，结果集的行为模式：`first`、`last`、`all`

| 选项值     | 默认  | 含义                                |
|---------|-----|-----------------------------------|
| `first` |     | 当遇到多条 SQL 同时执行时，结果集的行为是，返回第一个结果。  |
| `last`  | 是   | 当遇到多条 SQL 同时执行时，结果集的行为是，返回最后一个结果。 |
| `all`   |     | 当遇到多条 SQL 同时执行时，结果集的行为是，返回所有结果。   |

## FRAGMENT_SQL_OPEN_PACKAGE

SqlFragment 返回值拆包方式。

| 选项值      | 默认  | 含义                                                                            |
|----------|-----|-------------------------------------------------------------------------------|
| `off`    |     | 返回值不拆开，无论返回数据，都以 `List/Map` 形式返回。                                             |
| `row`    |     | 返回值拆分到行，如果返回值是多条记录那么行为和 off 相同。<br/>当返回 0 或 1 条记录时，自动解开最外层的 List，返回一个 Object。 |
| `column` | 是   | 最小粒度到列。当返回结果只有一行一列数据时。只返回具体值。<br/>例如： `select count(*)` 返回 int 类型             |

## FRAGMENT_SQL_PAGE_DIALECT

SqlFragment 分页查询在改写分页查询语句时使用的方言（默认：`空`，需要明确指定）

通常情况下，一个应用程序的数据库类型是确定的，因此方言参数也通常通过 Hasor 环境变量形式预先设置。
这个 Hint 的作用是，可以临时改变方言。或者是在全局未指定方言的情况下设置分页方言。
如果全局已经设置了方言参数，那么也可以通过这个 Hint 来改变默认配置。

| 数据库                         | 选项值             | 对应的方言类                                                |
|-----------------------------|-----------------|-------------------------------------------------------|
| PostgreSQL                  | `postgresql`    | `net.hasor.dataql.fx.db.dialect.PostgreSqlDialect`    |
| H2 Database Engine          | `h2`            | `net.hasor.dataql.fx.db.dialect.PostgreSqlDialect`    |
| HSQLDB(HyperSQL DataBase)   | `hsqldb`        | `net.hasor.dataql.fx.db.dialect.PostgreSqlDialect`    |
| Apache Phoenix              | `phoenix`       | `net.hasor.dataql.fx.db.dialect.PostgreSqlDialect`    |
| MySQL                       | `mysql`         | `net.hasor.dataql.fx.db.dialect.MySqlDialect`         |
| MariaDB                     | `mariadb`       | `net.hasor.dataql.fx.db.dialect.MySqlDialect`         |
| SQLite                      | `sqlite`        | `net.hasor.dataql.fx.db.dialect.MySqlDialect`         |
| HerdDB                      | `herddb`        | `net.hasor.dataql.fx.db.dialect.MySqlDialect`         |
| Microsoft® SQL Server® 2012 | `sqlserver2012` | `net.hasor.dataql.fx.db.dialect.SqlServer2012Dialect` |
| Apache Derby                | `derby`         | `net.hasor.dataql.fx.db.dialect.SqlServer2012Dialect` |
| OracleDialect               | `oracle`        | `net.hasor.dataql.fx.db.dialect.OracleDialect`        |
| IBM DB2                     | `db2`           | `net.hasor.dataql.fx.db.dialect.Db2Dialect`           |
| IBM Informix                | `informix`      | `net.hasor.dataql.fx.db.dialect.InformixDialect`      |

:::tip
当 DataQL 中内置分页字典不能满足要求时，可以在项目中重写一个分页方言。然后通过这个 Hint 配置全类路径的方式引用它。
:::

## FRAGMENT_SQL_QUERY_BY_PAGE

SqlFragment 查询执行是否使用分页模式（默认：`不使用`）

| 选项值     | 默认  | 含义                                         |
|---------|-----|--------------------------------------------|
| `TRUE`  |     | 在执行 `select` 语句时采用分页模式执行，分页模式请参考 SQL执行器章节。 |
| `FALSE` | 是   | 不启用分页模式。                                   |

## FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET

SqlFragment 在执行分页查询时，设置的当前页码偏移量。原始的 `currentPage` 规定启始页码是从 `0` 开始。在某些场景下 `1` 开始会比较好理解，这时候就可以设施偏移量为 `1`。

当设置偏移量之后，真实的 currentPage 值计算方式为：`yourCurrentPage - FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET` 结果如果小于等于 `0`，那么设置为 `0`

```js
hint FRAGMENT_SQL_QUERY_BY_PAGE = true
hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1

// 步骤 1：定义分页SQL
hint FRAGMENT_SQL_QUERY_BY_PAGE = true
var dimSQL = @@sql(userName)<%
    select * from user_info where `name` like concat('%',#{userName},'%')
%>;

// 步骤 2：获取分页对象
var queryPage = dimSQL(${userName});

// 步骤 3：设置分页信息
run queryPage.setPageInfo({
    "pageSize" : 5, // 页大小
    "currentPage" : 1 // 第1页，在设置 FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET 之前 第一页要设置为 0
});
```

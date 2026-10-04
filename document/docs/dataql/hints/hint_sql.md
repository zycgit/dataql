---
id: hint_sql
title: 6.10 SQL Hint
---

本页列出 `dataql-sqlproc` 支持的 SQL 执行选项。先通过 ConnectionProvider 接入数据源，见[SQL 执行器](../../dataway/dataql-engine/sql.md)。

长名称可在脚本 Hint 中设置；表中列有短名称的选项也接受短名称，同时设置时短名称优先。`selectKey` 可通过其属性指定子查询选项；XML 片段不接受外层 Mapper 语句标签。

## 数据源 {#FRAGMENT_SQL_DATA_SOURCE}

`FRAGMENT_SQL_DATA_SOURCE` 默认空字符串，传给 `ConnectionProvider.findConnection(name, hints)` 选择连接。

```javascript
hint FRAGMENT_SQL_DATA_SOURCE = 'ds1';
var query = @@selectSql()<% SELECT 1 AS result_value %>;
return query();
// 1
```

应用按名称提供数据源，配置方式见[数据源接入](../../dataway/capabilities/datasources.md)。

## 语句执行

| Hint | 短名称 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `FRAGMENT_SQL_STATEMENT` | `statementType` | `prepared` | `statement`、`prepared`、`callable`，分别使用对应 JDBC 语句；call 片段使用 callable |
| `FRAGMENT_SQL_TIMEOUT` | `timeout` | `-1` | 查询超时，单位秒；-1 不设置 |
| `FRAGMENT_SQL_FETCH_SIZE` | `fetchSize` | `256` | JDBC 每批获取行数提示，效果由驱动决定 |
| `FRAGMENT_SQL_RESULT_SET_TYPE` | `resultSetType` | 空 | 结果集类型：`forwardOnly`、`scrollSensitive`、`scrollInsensitive`；空值使用驱动默认 |

```javascript
hint timeout = 5;
hint fetchSize = 100;
var query = @@selectSql(value)<% SELECT #{value} AS result_value %>;
return query(42);
// 42
```

## 列名转换 {#FRAGMENT_SQL_COLUMN_CASE}

`FRAGMENT_SQL_COLUMN_CASE` 默认 `default`。

| 值 | 结果 |
| --- | --- |
| `default` | 保留驱动返回的列标签 |
| `upper` | 大写 |
| `lower` | 小写 |
| `hump` | 下划线转小驼峰，例如 USER_ID 转为 userId |

转换后的重名字段保留首次出现的值，应通过 SQL 列别名避免冲突。

## 结果拆包 {#FRAGMENT_SQL_OPEN_PACKAGE}

`FRAGMENT_SQL_OPEN_PACKAGE` 默认 `column`，控制普通查询结果的结构。

| 结果行数 | `off` | `row` | `column` |
| --- | --- | --- | --- |
| 0 行 | `[]` | `{}` | null |
| 1 行 1 列 | 对象列表 | 单个对象 | 单个值 |
| 1 行多列 | 对象列表 | 单个对象 | 单个对象 |
| 多行 | 对象列表 | 对象列表 | 对象列表 |

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var query = @@selectSql()<% SELECT 1 AS user_id %>;
return query();
// [{"user_id":1}]
```

更新类语句返回影响行数。分页的 `data()` 返回当前页列表，不按单行单列拆包。

## 输出参数与生成键

| Hint | 短名称 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `FRAGMENT_SQL_BIND_OUT` | `bindOut` | 空 | 指定返回的输出名称，多个名称以逗号分隔 |
| `FRAGMENT_SQL_KEY_GENERATED` | `useGeneratedKeys` | `false` | 生成键开关，当前执行链尚未接通 |
| `FRAGMENT_SQL_KEY_PROPERTY` | `keyProperty` | 空 | 生成键写回参数的属性名 |
| `FRAGMENT_SQL_KEY_COLUMN` | `keyColumn` | 空 | 生成键的列名，多列按逗号分隔并与属性顺序对应 |
| `FRAGMENT_SQL_ORDER` | `order` | `after` | selectKey 执行时机：before 或 after |

`bindOut` 可选取执行参数或 JDBC 返回的结果；结果集名称为 `#result-set-1` 等，更新计数名称为 `#update-count-1` 等，编号按结果顺序递增。`bindOut` 不适用于分页及普通增删改的影响行数返回。当前执行链尚未接通 JDBC 生成键读取；可用的 `selectKey` 配置见[结果与主键](../sql/results.md)。

```javascript
hint bindOut = '#result-set-1';
var query = @@selectSql()<% SELECT 1 AS result_value %>;
return query();
// {"#result-set-1":1}
```

## 分页开关 {#FRAGMENT_SQL_QUERY_BY_PAGE}

`FRAGMENT_SQL_QUERY_BY_PAGE` 默认 false。设为 true 后，select 片段返回分页对象，先调用 `setPageInfo`，再调用 `data()` 获取数据。

```javascript
hint FRAGMENT_SQL_QUERY_BY_PAGE = true;
hint FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET = 1;
var query = @@selectSql()<%
    SELECT 1 AS id UNION ALL SELECT 2 AS id ORDER BY id
%>;
var page = query();
run page.setPageInfo({'currentPage':1, 'pageSize':1});
return page.data();
// [{"ID":1}]，列标签大小写由驱动决定
```

## 页码偏移 {#FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET}

`FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET` 默认 0，表示第一页的页码。设为 1 后第一页使用 1。内部页码按 `max(传入页码 - 偏移量, 0)` 计算。

## 分页方言 {#FRAGMENT_SQL_PAGE_DIALECT}

`FRAGMENT_SQL_PAGE_DIALECT` 默认空，从 JDBC URL 和驱动信息推断分页方言。需要明确指定时，传入以下别名或 PageDialect 实现类名：

| 数据库 | 别名 |
| --- | --- |
| MySQL、MariaDB | `mysql`、`mariadb` |
| PostgreSQL、Kingbase | `postgresql`、`kingbase` |
| H2、HSQLDB | `h2`、`hsql` |
| Oracle、DB2 | `oracle`、`db2` |
| SQL Server | `sqlserver`、`jtds` |
| SQLite、Derby | `sqlite`、`derby` |
| DM、Impala、Informix、XuGu | `dm`、`impala`、`informix`、`xugu` |

## 事务隔离 {#FRAGMENT_SQL_TRANSACTION_ISOLATION}

`FRAGMENT_SQL_TRANSACTION_ISOLATION`，短名称 `isolation`，默认 DEFAULT。支持 READ_UNCOMMITTED、READ_COMMITTED、REPEATABLE_READ、SERIALIZABLE。

此选项由 [TransactionUdfSource](../funx/transactions.md) 交给事务提供者，在创建事务时使用；单独设置不会开启事务。加入已有事务时遵循宿主事务规则。

## 片段格式 {#FRAGMENT_SQL_FORMAT}

当前 SQL 片段按注册名区分格式：`selectSql`、`updateSql` 等使用文本，`selectXml`、`updateXml` 等使用动态 XML。`FRAGMENT_SQL_FORMAT` 虽保留在枚举中，当前片段入口不读取它，设置该 Hint 不会切换格式。

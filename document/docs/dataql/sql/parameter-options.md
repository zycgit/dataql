---
id: parameter-options
title: 6.2.5 参数选项
---

在 `#{表达式, 选项名=值}` 中配置一个参数的绑定方式。多个选项用逗号分隔，配置名、JDBC 类型名和 `mode` 值不区分大小写；处理器类名使用完整 Java 类名。选项值不加引号。

```sql
WHERE created_at >= #{since, jdbcType=TIMESTAMP}
```

参数选项作用于一个占位符。连接选择、超时和分页等整次 SQL 执行的设置见 [SQL Hint](../hints/hint_sql.md)。

## 选项一览

| 选项 | 取值 | 作用 |
| --- | --- | --- |
| `jdbcType` | JDBC 类型名或整数代码 | 明确数据库类型，如 `VARCHAR`、`TIMESTAMP`、`12` |
| `typeHandler` | `TypeHandler` 实现类全名 | 指定该参数的转换处理器 |
| `mode` | `IN`、`OUT`、`INOUT`、`CURSOR` | 参数方向；未设置时按输入参数处理 |
| `name` | 输出名称 | 指定输出参数在结果对象中的键名 |
| `typeName` | 数据库类型名 | 注册输出参数时传给 JDBC 驱动 |
| `scale` | 整数 | 注册数值输出参数时指定小数位数 |

SQL 执行器按 DataQL 值的实际类型选择处理器，不提供 `javaType`、实体映射或 `rowMapper` 参数选项。

## jdbcType：明确数据库类型

日期参数可以是文本或毫秒时间戳，通过 `jdbcType` 选择日期处理器：

```javascript
var read = @@selectSql(value)<%
    SELECT CAST(#{value, jdbcType=DATE} AS DATE)
%>;
return read('2026-10-05');
```

处理器使用 JDBC 日期绑定；DataQL 中的结果为毫秒时间戳。常用类型包括 `VARCHAR`、`INTEGER`（也可写 `INT`）、`BIGINT`、`DECIMAL`、`DATE`、`TIME`、`TIMESTAMP`、`BLOB`、`ARRAY`。`jdbcType=VARCHAR` 与 `jdbcType=12` 等价。

插入或更新 null 时，显式类型可避免由驱动推断：

```javascript
var clear = @@updateSql(id, name)<%
    UPDATE people SET name = #{name, jdbcType=VARCHAR} WHERE id = #{id}
%>;
return clear(1, null);
```

本例将名称写为 SQL NULL，返回影响行数 `1`。

## typeHandler：指定转换

以下 H2 示例将 DataQL 对象序列化为 JSON 文本：

```javascript
var encode = @@selectSql(document)<%
    SELECT CAST(#{document,
        jdbcType=VARCHAR,
        typeHandler=net.hasor.dataql.sqlproc.types.json.JsonTypeHandler} AS VARCHAR)
%>;
return encode({'name':'Alice','tags':['java','dataql']});
```

结果是 JSON 字符串 `{"name":"Alice","tags":["java","dataql"]}`。`typeHandler` 优先于自动选择，只作用于该参数；它不会自动改变查询结果列的处理器。返回对象需要同时配置相应的结果类型映射，见 [SQL 类型处理器](../../dataway/engine/sql-types.md)。

处理器应位于应用 classpath。使用公开无参构造方法即可按类名创建；无法加载或创建处理器时，SQL 执行失败。

## mode 和 name：存储过程输出

`callSql`、`callXml` 使用支持存储过程的 JDBC 驱动。假设数据库中已定义 `add_one(IN input_value INT, OUT output_value INT)`，输出值为输入值加一：

```javascript
hint bindOut = 'answer';
var calculate = @@callSql(value)<%
    {call add_one(
        #{value, mode=IN, jdbcType=INTEGER},
        #{output, mode=OUT, jdbcType=INTEGER, name=answer}
    )}
%>;
return calculate(41);
```

预期结果为 `{"answer":42}`。`OUT` 不读取同名输入值，所以无需声明 `output` 形参。`name=answer` 指定结果键名，`bindOut` 选择要返回的输出；未设置 `name` 时使用表达式名称。

输入输出参数用 `INOUT`，需要提供初值：

```javascript
hint bindOut = 'answer';
var calculate = @@callSql(value)<%
    {call increment_value(#{value, mode=INOUT, jdbcType=INTEGER, name=answer})}
%>;
return calculate(41);
```

该例要求数据库事先提供 `increment_value(INOUT value INT)` 过程。输出参数除游标外需指定 `jdbcType`。建过程示例及多结果读取见[存储过程与多结果](procedures.md)。

## typeName 和 scale

数据库要求命名类型时设置 `typeName`；数值输出可设置 `scale`：

```sql
#{total, mode=OUT, jdbcType=DECIMAL, scale=2}
#{value, mode=OUT, jdbcType=STRUCT, typeName=APP.ADDRESS_TYPE}
```

这些选项用于 JDBC 输出参数注册，具体取值由数据库和驱动决定。两者同时存在时优先使用 `typeName`，其次是 `scale`，均未设置则只使用 `jdbcType`。特殊数据库对象的读取仍需匹配的处理器。

## CURSOR：游标输出

假设 Oracle 过程 `find_people` 接收最低年龄并输出 `SYS_REFCURSOR`：

```javascript
hint bindOut = 'rows';
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@callSql(minAge)<%
    {call find_people(#{minAge}, #{rows, mode=CURSOR})}
%>;
return find(25);
```

执行器按驱动注册游标，并将它读取为普通查询结果；`off` 保持行列表，结果形式为 `{"rows":[...]}`。这一用法需在支持游标输出的数据库上执行，H2 示例环境不能验证 Oracle 存储过程。

## ARG 规则

`@{arg, , 表达式, 选项}` 与 `#{表达式, 选项}` 都构造一个绑定参数：

```sql
WHERE age >= @{arg, , minAge, jdbcType=INTEGER}
```

规则名后保留一个空位置，参数表达式和选项放在规则正文中。它同样生成 `WHERE age >= ?`。日常参数绑定使用 `#{...}` 即可。

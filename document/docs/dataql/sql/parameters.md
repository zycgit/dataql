---
id: parameters
title: 6.2 参数绑定
---

片段声明的形参构成 SQL 参数上下文，支持对象属性和集合下标访问。

## 命名参数

```javascript
var find = @@selectSql(filter)<%
    SELECT name FROM people
    WHERE id = #{filter.id, jdbcType=BIGINT} AND age >= :filter.minAge
%>;
return find({'id':1, 'minAge':20});
```

`#{filter.id}` 和 `:filter.minAge` 都转换为 JDBC `?` 并绑定值。业务数据应使用参数绑定。

| 写法 | 含义 |
| --- | --- |
| `#{name}`、`:name` | 绑定参数，可读取对象属性 |
| `#{ids[0]}` | 绑定集合中的一个值 |
| `?` | 依次读取 `arg0`、`arg1` 等片段形参 |
| `${name}`、`&name` | 将值直接拼入 SQL 文本 |

位置参数需要对应的形参名：

```javascript
var find = @@selectSql(arg0)<% SELECT name FROM people WHERE id = ? %>;
return find(1);
```

文本替换适合由应用白名单控制的表名、列名等 SQL 标识符，不能直接拼接用户输入。

## JDBC 参数选项

```sql
WHERE id = #{id, jdbcType=BIGINT}
```

| 选项 | 作用 |
| --- | --- |
| `jdbcType` | JDBC 类型名或数值，例如 `VARCHAR`、`BIGINT` |
| `typeHandler` | `TypeHandler` 实现类全名 |
| `mode` | 存储过程参数方向：`IN`、`OUT`、`INOUT` |
| `name` | 输出参数名称 |
| `typeName` | JDBC 数据库类型名，用于输出注册 |
| `scale` | 输出数值的小数位数 |

空值的 JDBC 类型应显式指定；自定义处理器见 [SQL 类型处理器](../../dataway/engine/sql-types.md)。`javaType` 不属于当前占位符支持的选项。

## 与脚本参数衔接

```javascript
var find = @@selectSql(id)<% SELECT name FROM people WHERE id = #{id} %>;
return find(${id});
```

`${id}` 在片段外由 DataQL 读取；进入片段后使用命名绑定。`CustomizeScope` 的接入见[自定义作用域](../../dataway/engine/scope.md)。

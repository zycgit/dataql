---
id: named
title: 6.2.2 名称参数
---

`#{name}`、`:name`、`&name` 都读取形参 `name` 并生成 JDBC `?`。`#{...}` 还支持表达式和参数选项，适合统一使用。以下示例使用 [people 表](../execute.md#示例数据)。

## 绑定与复用

```javascript
var find = @@selectSql(age)<%
    SELECT name FROM people WHERE age >= #{age} AND age <= :age
%>;
return find(25);
```

生成 `WHERE age >= ? AND age <= ?`，绑定 `[25,25]`，返回 `Alice`。SQL 中的顺序决定绑定顺序，形参在声明中的位置只决定调用时的传值顺序。

`&name` 同样是值绑定，不是文本替换。在 XML 中写为 `&amp;name`，避免与 XML 实体语法冲突。

## 对象属性与集合下标

```javascript
var find = @@selectSql(filter, ids)<%
    SELECT name FROM people
    WHERE age >= #{filter.minAge} AND id = #{ids[0]}
%>;
return find({'minAge':20}, [1,2]);
```

绑定 `[20,1]`，返回 `Alice`。对象属性使用点号，列表使用从 0 开始的下标；特殊名称的键可写为 `#{filter['min-age']}`。

`#{ids}` 绑定整个列表，不会自动变成多个占位符。`WHERE id IN (#{ids})` 也不会自动展开；应使用[集合规则](../rules.md)或 [foreach](../mybaits.md)。

## 特殊属性名称

OGNL 将 `size`、`keys`、`keySet`、`values`、`isEmpty` 识别为参数集合的特殊属性。同名业务参数使用中括号取值，嵌套对象也使用 `filter['size']` 等写法：

```javascript
var find = @@selectSql(values)<%
    SELECT name FROM people WHERE id = #{['values']}
%>;
return find(1);
```

这里绑定业务参数 `values` 的值 `1`，返回 `Alice`。直接写 `#{values}` 则读取全部片段参数组成的集合。

## 模糊查询

将通配符作为参数值的一部分：

```javascript
var find = @@selectSql(keyword)<%
    SELECT name FROM people WHERE name LIKE #{'%' + keyword + '%'}
%>;
return find('Ali');
```

生成 `WHERE name LIKE ?`，绑定 `%Ali%`，返回 `Alice`。`'%#{keyword}%'` 属于 SQL 字符串，不会执行绑定。

## 参数表达式

`#{...}` 中使用 OGNL 读取属性、调用常见字符串方法或计算值：

```javascript
var find = @@selectSql(minAge, name)<%
    SELECT name FROM people
    WHERE age >= #{minAge + 1} AND name = #{name.trim()}
%>;
return find(24, ' Alice ');
```

绑定 `[25,"Alice"]`，返回 `Alice`。带空格、运算符或方法调用的表达式统一使用 `#{...}`，简写形式用于名称和属性路径。

表达式接收的是片段形参，不直接读取整个 HTTP 请求。API 参数应在调用片段时传入。

## 空值与缺失参数

普通参数绑定不会删除 SQL。`name = #{name}` 在参数为 null 时仍生成 `name = ?`；SQL 等号比较 null 不会匹配空值行。

```javascript
var find = @@selectSql(name)<%
    SELECT count(*) FROM people WHERE 1 = 1
    @{if, name != null, AND name = #{name}}
    @{if, name == null, AND name IS NULL}
%>;
return find(null);
```

以上显式区分普通值和数据库 NULL。需要忽略空筛选条件时使用 `and`、`ifand` 等[动态规则](../rules.md)。给列赋空值时，可以用 `#{name, jdbcType=VARCHAR}` 明确类型。

调用前应保证必填值和属性路径有效；参数绑定本身不承担业务必填校验。访问空对象属性或越界下标可能导致表达式求值失败。

---
id: MyBatis
title: 6.4 XML 动态 SQL
---

XML 动态 SQL 根据参数选择条件、展开集合并处理多余的连接词。它适合可选查询条件、批量插入和部分字段更新。`selectXml`、`insertXml`、`updateXml` 等片段直接包含 SQL 和标签，无需 `mapper`、`select` 等外层元素。

以下示例使用 [SQL 执行](execute.md#示例数据)中的 `people` 表。标签生成 SQL 后，`#{name}` 仍按预编译参数绑定；`${name}` 属于[SQL 文本替换](parameters.md)，不会因为使用 XML 而自动转为安全绑定。

## 条件判断：if {#if}

`test` 是必填的 OGNL 表达式。成立时输出标签正文，不成立时不输出 SQL，也不绑定正文中的参数。

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectXml(minAge, name)<%
    SELECT id, name FROM people WHERE enabled = 1
    <if test="minAge != null">AND age &gt;= #{minAge}</if>
    <if test="name != null and name != ''">AND name = #{name}</if>
    ORDER BY id
%>;
return find(25, null);
```

本次生成 `SELECT id, name FROM people WHERE enabled = 1 AND age >= ? ORDER BY id`，绑定参数为 `[25]`，返回 Alice、Bob。传入 `find(null, 'Alice')` 时，只生成姓名条件。

常用条件包括 `value != null`、`name != ''`、`ids.size() > 0`、`filter.minAge != null`，可用 `and`、`or` 组合。访问可空对象的属性前先检查对象，例如 `filter != null and filter.minAge != null`。标签中的表达式使用片段参数名，不使用 DataQL 的 `${...}` 访问写法。

## 多分支：choose、when、otherwise {#choose}

`choose` 按顺序检查 `when.test`，只输出第一个成立的分支；全部不成立时输出 `otherwise`。`choose` 和 `otherwise` 不需要属性。

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectXml(name, minAge)<%
    SELECT id, name FROM people
    <where>
        <choose>
            <when test="name != null and name != ''">name = #{name}</when>
            <when test="minAge != null">age &gt;= #{minAge}</when>
            <otherwise>enabled = 1</otherwise>
        </choose>
    </where>
    ORDER BY id
%>;
return find('Alice', 30);
```

即使 `minAge` 也有值，本次只生成 `WHERE name = ?`，参数为 `['Alice']`。两个参数都为空时，生成 `WHERE enabled = 1`。

当前实现要求保留 `otherwise`。不需要默认条件时使用 `<otherwise/>`，避免所有 `when` 都未命中时缺少默认节点。

## 条件区域：where {#where}

`where` 没有属性。正文有内容时添加 `WHERE`，并移除开头的一个 `AND` 或 `OR`；正文为空时整个区域不输出。

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectXml(minAge, name)<%
    SELECT id, name FROM people
    <where>
        <if test="minAge != null">AND age &gt;= #{minAge}</if>
        <if test="name != null and name != ''">AND name = #{name}</if>
    </where>
    ORDER BY id
%>;
return find(null, 'Bob');
```

生成 `SELECT id, name FROM people WHERE name = ? ORDER BY id`。如果调用 `find(null, null)`，生成的 SQL 没有 `WHERE`，返回全部记录。更新、删除语句使用可选条件时，应先要求必要的筛选参数，避免条件全部消失。

## 更新区域：set {#set}

`set` 没有属性。正文有内容时添加 `SET`，并移除末尾逗号。每个可选赋值后都可以写逗号。

```javascript
var change = @@updateXml(id, name, age)<%
    UPDATE people
    <set>
        <if test="name != null">name = #{name},</if>
        <if test="age != null">age = #{age},</if>
    </set>
    WHERE id = #{id}
%>;
return change(1, null, 26);
```

生成 `UPDATE people SET age = ? WHERE id = ?`，绑定参数为 `[26, 1]`，返回影响行数 `1`。调用前至少提供一个更新字段；所有字段都为空时，标签不会补出有效的更新语句。需要把某列主动设为 SQL `NULL` 时，应另外传递更新开关，不能同时用该字段的 `null` 表示“不更新”。

## 前后缀处理：trim {#trim}

`trim` 在正文有内容时添加前后缀，并按配置裁剪开头或末尾的文本。

| 属性 | 作用 |
| --- | --- |
| `prefix` | 添加到正文前的文本 |
| `suffix` | 添加到正文后的文本 |
| `prefixOverrides` | 待裁剪的前缀，多个候选用 `\|` 分隔 |
| `suffixOverrides` | 待裁剪的后缀，多个候选用 `\|` 分隔 |

四个属性均可省略；裁剪忽略大小写，每端只移除第一个匹配项。正文为空时不添加前后缀。下面把可选条件包成括号：

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectXml(minAge, name)<%
    SELECT id, name FROM people WHERE enabled = 1
    <trim prefix="AND (" suffix=")" prefixOverrides="AND | OR">
        <if test="minAge != null">OR age &gt;= #{minAge}</if>
        <if test="name != null">OR name = #{name}</if>
    </trim>
    ORDER BY id
%>;
return find(30, 'Alice');
```

生成 `... WHERE enabled = 1 AND (age >= ? OR name = ?) ORDER BY id`，参数为 `[30, 'Alice']`。`where` 相当于 `prefix="WHERE" prefixOverrides="AND | OR"`；`set` 相当于 `prefix="SET" suffixOverrides=","`。

## 集合展开：foreach {#foreach}

`foreach` 为每个元素生成一段 SQL，并按顺序绑定各元素的参数。

| 属性 | 作用 |
| --- | --- |
| `collection` | 必填，待遍历值的 OGNL 表达式，例如 `ids`、`filter.ids` |
| `item` | 必填，正文中访问当前元素的变量名 |
| `open`、`close` | 可选，整次循环前后输出的文本 |
| `separator` | 可选，相邻元素之间的分隔文本 |

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectXml(ids)<%
    SELECT id, name FROM people
    <where>
        <choose>
            <when test="ids != null and ids.size() > 0">
                id IN
                <foreach collection="ids" item="id" open="(" close=")" separator=",">
                    #{id}
                </foreach>
            </when>
            <otherwise>1 = 0</otherwise>
        </choose>
    </where>
    ORDER BY id
%>;
return find([1, 2]);
```

生成 `... WHERE id IN (?, ?) ORDER BY id`，参数为 `[1, 2]`。空列表或 `null` 生成 `WHERE 1 = 0`，返回空列表。这个分支明确了空集合的业务含义。

`foreach` 接受集合和数组；其他非空值按一个元素处理，Map 不会自动按键值对遍历。当前不提供 `index`、`nullable` 属性。值为 `null` 时标签不输出；空集合仍输出 `open`、`close`，直接用于 `IN` 会形成 `IN ()`。分隔符按元素位置添加，正文中的 `if` 不会自动跳过对应分隔符，因此应在循环前准备好集合。

循环也可生成多行 `VALUES`：

```javascript
var add = @@insertXml(rows)<%
    INSERT INTO people(name, age) VALUES
    <foreach collection="rows" item="row" separator=",">
        (#{row.name}, #{row.age})
    </foreach>
%>;
return add([{'name':'Carol', 'age':20}, {'name':'David', 'age':28}]);
```

生成 `INSERT INTO people(name, age) VALUES (?, ?), (?, ?)`，参数为 `['Carol', 20, 'David', 28]`，返回 `2`。调用前保证 `rows` 非空，并确认目标数据库支持多行 `VALUES`。

## 表达式变量：bind {#bind}

`bind` 计算 `value` 中的 OGNL 表达式，把结果保存到 `name` 指定的变量中，不直接输出 SQL。两个属性均需要填写，变量可由后续条件、占位符和规则读取。

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectXml(name)<%
    SELECT id, name FROM people
    <where>
        <if test="name != null and name != ''">
            <bind name="pattern" value="'%' + name + '%'"/>
            name LIKE #{pattern}
        </if>
    </where>
    ORDER BY id
%>;
return find('Ali');
```

生成 `... WHERE name LIKE ? ORDER BY id`，参数为 `['%Ali%']`，返回 Alice。`pattern` 属于此次 SQL 构建上下文，不会成为脚本中的 `var` 变量；避免用同名 `bind` 覆盖仍需使用的片段参数。

## 公共片段：include {#include}

`include` 的 `refid` 指向应用注册的 SQL 宏。先按 [SQL 片段](../../dataway/engine/sql-macros.md)注册 `adult`，内容为 `age >= 18`，再调用：

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectXml()<%
    SELECT id, name FROM people WHERE <include refid="adult"/> ORDER BY id
%>;
return find();
```

生成 `... WHERE age >= 18 ORDER BY id`。宏内可使用当前片段参数；此入口引入的是注册的 SQL 文本，不读取 MyBatis Mapper 文件，也不支持 Mapper 的 `namespace`、嵌套 `property` 配置。

## 主键查询：selectKey {#select-key}

`insertXml` 可在正文顶层声明 `selectKey`，在插入前或插入后执行查询。`keyProperty` 指定回填参数，`keyColumn` 指定查询结果列，`order` 指定 `BEFORE` 或 `AFTER`；还可设置 `statementType`、`timeout`、`fetchSize`、`resultSetType`。

主键回填和完整插入示例见[结果与主键](results.md#select-key)。这里的标签用于 SQL 执行，不包含 MyBatis 的 `resultType`、`resultMap` 或实体映射能力。

## XML 字符与标签组合 {#xml-text}

SQL 文本中的 `<`、`&` 分别写成 `&lt;`、`&amp;`，或把纯 SQL 文本放入 CDATA。标签属性中的比较可以写 `&lt;`、`&gt;`；CDATA 不能代替属性转义。

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
var find = @@selectXml(maxAge)<%
    SELECT name FROM people
    <where>
        <if test="maxAge != null"><![CDATA[AND age < #{maxAge}]]></if>
    </where>
    ORDER BY id
%>;
return find(30);
```

只将纯文本放入 CDATA；CDATA 中的 `<if>` 会作为 SQL 文本发送，不会作为标签执行。动态标签可以嵌套，并与 [SQL 规则](rules.md)组合使用。支持的标签以本页为准，未知标签会在解析阶段报错。

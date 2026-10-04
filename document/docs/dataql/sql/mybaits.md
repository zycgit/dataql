---
id: MyBatis
title: 6.4 XML 动态 SQL
---

`selectXml`、`insertXml`、`updateXml` 等片段直接包含 SQL 和动态标签。正文不需要 `mapper` 或 `select` 外层标签。

## 条件和集合

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectXml(minAge, ids)<%
    SELECT id, name FROM people
    <where>
        <if test="minAge != null">AND age &gt;= #{minAge}</if>
        <if test="ids != null and ids.size() > 0">
            AND id IN
            <foreach collection="ids" item="id" open="(" close=")" separator=",">
                #{id}
            </foreach>
        </if>
    </where>
    ORDER BY id
%>;
return find(25, [1,2]);
```

`where` 在有条件时添加 `WHERE`，并去掉开头的 `AND` 或 `OR`。`foreach` 将集合元素绑定为独立参数，`item` 是循环变量。

## 动态更新

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

`set` 添加 `SET` 并去掉末尾逗号。调用方应至少提供一个更新字段；全部字段为空会形成无效更新语句。

## 分支和变量

```javascript
var find = @@selectXml(name)<%
    <bind name="pattern" value="'%' + name + '%'"/>
    SELECT count(*) FROM people
    <where>
        <choose>
            <when test="name != null and name != ''">name LIKE #{pattern}</when>
            <otherwise>age &gt;= 30</otherwise>
        </choose>
    </where>
%>;
return find('Ali');
```

`bind` 将表达式结果保存为后续 SQL 可使用的变量，`choose` 选择第一个成立的 `when`，其余情况执行 `otherwise`。

## 标签参考

| 标签 | 主要属性 | 作用 |
| --- | --- | --- |
| `if` | `test` | 按 OGNL 条件输出内容 |
| `choose`、`when`、`otherwise` | `when.test` | 多分支选择 |
| `foreach` | `collection`、`item`、`open`、`close`、`separator` | 遍历集合并连接内容 |
| `trim` | `prefix`、`suffix`、`prefixOverrides`、`suffixOverrides` | 添加或裁剪前后缀；多个裁剪值用 `\|` 分隔 |
| `where` | 无 | 添加 WHERE，裁剪首个逻辑连接词 |
| `set` | 无 | 添加 SET，裁剪末尾逗号 |
| `bind` | `name`、`value` | 定义表达式变量 |
| `include` | `refid` | 引入[SQL 片段](../../dataway/engine/sql-macros.md) |
| `selectKey` | `keyProperty`、`keyColumn`、`order` | 插入前后执行[主键查询](results.md#select-key) |

XML 中的小于号使用 `&lt;`，包含复杂比较符时可将 SQL 文本放入 CDATA。标签属性中的条件使用 OGNL；`foreach` 当前没有 `index` 属性。MyBatis 的 Mapper 接口、`resultMap` 等机制不在此入口范围内。

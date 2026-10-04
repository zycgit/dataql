---
id: MyBatis
title: 6.4 XML dynamic SQL
---

XML fragments directly contain SQL and dynamic tags. Do not wrap them in `mapper` or `select` elements.

## Conditions and iteration

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

`where` adds WHERE only when needed and removes the first AND or OR. `foreach` binds each item separately.

## Updates

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

`set` adds SET and removes a trailing comma. Require at least one update field to avoid an empty assignment list.

## Branches and variables

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

`bind` defines a variable. `choose` selects the first matching `when`, falling back to `otherwise`.

| Tag | Attributes | Purpose |
| --- | --- | --- |
| `if` | `test` | OGNL condition |
| `choose`, `when`, `otherwise` | `when.test` | Branching |
| `foreach` | `collection`, `item`, `open`, `close`, `separator` | Collection expansion |
| `trim` | `prefix`, `suffix`, `prefixOverrides`, `suffixOverrides` | Add or remove surrounding tokens |
| `where`, `set` | None | Standard clause trimming |
| `bind` | `name`, `value` | Expression variable |
| `include` | `refid` | [SQL fragments](../../dataway/engine/sql-macros.md) |
| `selectKey` | `keyProperty`, `keyColumn`, `order` | [Key query](results.md#select-key) |

Separate trim overrides with a pipe. Escape XML comparison characters or place SQL text in CDATA. `foreach` has no `index` attribute. Mapper interfaces and MyBatis `resultMap` are outside this API.

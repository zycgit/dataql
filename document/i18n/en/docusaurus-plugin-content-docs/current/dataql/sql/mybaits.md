---
id: MyBatis
title: 6.4 XML Dynamic SQL
---

XML dynamic SQL selects conditions, expands collections, and removes redundant SQL separators. Use it for optional filters, multirow inserts, and updates of selected fields. Fragments such as `selectXml`, `insertXml`, and `updateXml` contain SQL and dynamic tags directly, without a `mapper` or `select` wrapper.

Examples use the `people` table from [SQL Execution](execute.md#sample-data). After tags generate the statement, `#{name}` remains a prepared-statement parameter. `${name}` performs [SQL text substitution](parameters.md); using XML does not turn substituted text into a bound value.

## Conditional content: if {#if}

The required `test` attribute contains an OGNL expression. A true expression includes the body; a false expression includes neither the SQL nor its bound parameters.

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

This produces `SELECT id, name FROM people WHERE enabled = 1 AND age >= ? ORDER BY id`, with `[25]`, returning Alice and Bob. Calling `find(null, 'Alice')` includes only the name condition.

Common conditions include `value != null`, `name != ''`, `ids.size() > 0`, and `filter.minAge != null`. Combine them with `and` or `or`. Check nullable objects before accessing their properties: `filter != null and filter.minAge != null`. Expressions reference fragment parameter names directly, without DataQL's `${...}` syntax.

## Branches: choose, when, otherwise {#choose}

`choose` evaluates `when.test` in order and includes only the first matching branch. If none matches, it includes `otherwise`. Neither `choose` nor `otherwise` has attributes.

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

Although both parameters have values, the statement uses only `WHERE name = ?` with `['Alice']`. With both parameters null, it uses `WHERE enabled = 1`.

The current implementation requires an `otherwise` node. Use `<otherwise/>` if no default SQL is needed, so an unmatched `choose` still has a default node.

## Conditions: where {#where}

`where` takes no attributes. It adds `WHERE` when its body produces SQL and removes one leading `AND` or `OR`. It emits nothing when the body is empty.

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

This produces `SELECT id, name FROM people WHERE name = ? ORDER BY id`. Calling `find(null, null)` removes the entire `WHERE` and returns every row. For updates and deletes, require the necessary filters before execution to prevent an empty filter from affecting every row.

## Update assignments: set {#set}

`set` takes no attributes. It adds `SET` around nonempty content and removes a trailing comma. Each optional assignment can end with a comma.

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

The generated statement is `UPDATE people SET age = ? WHERE id = ?`, with `[26, 1]`, and returns the affected-row count `1`. Require at least one assignment before calling the fragment. An empty body does not produce a valid update automatically. To assign SQL `NULL` deliberately, use a separate update flag instead of also interpreting null as “leave unchanged.”

## Prefixes and suffixes: trim {#trim}

`trim` adds prefixes and suffixes around nonempty content and removes matching text at the edges.

| Attribute | Purpose |
| --- | --- |
| `prefix` | Text added before the body |
| `suffix` | Text added after the body |
| `prefixOverrides` | Prefixes to remove, separated by `\|` |
| `suffixOverrides` | Suffixes to remove, separated by `\|` |

All attributes are optional. Matching is case-insensitive; only the first matching candidate is removed at each edge. An empty body adds no prefix or suffix. This example groups optional alternatives:

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

It generates `... WHERE enabled = 1 AND (age >= ? OR name = ?) ORDER BY id`, with `[30, 'Alice']`. `where` corresponds to `prefix="WHERE" prefixOverrides="AND | OR"`; `set` corresponds to `prefix="SET" suffixOverrides=","`.

## Collection expansion: foreach {#foreach}

`foreach` generates a SQL segment for each element and binds its values in order.

| Attribute | Purpose |
| --- | --- |
| `collection` | Required OGNL expression yielding the input, such as `ids` or `filter.ids` |
| `item` | Required variable name for the current element |
| `open`, `close` | Optional text before and after the entire loop |
| `separator` | Optional separator between elements |

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

The result is `... WHERE id IN (?, ?) ORDER BY id`, with `[1, 2]`. A null or empty list generates `WHERE 1 = 0` and returns an empty list. This explicitly defines what an empty filter means.

Collections and arrays are traversed; any other nonnull value is treated as one element. Maps are not traversed as key/value entries. There are no `index` or `nullable` attributes. A null input emits nothing, whereas an empty collection still emits `open` and `close`, potentially producing `IN ()`. Separators follow element positions: an `if` inside the loop does not automatically remove the separator for skipped content. Prepare the collection before looping.

A loop can also generate multiple `VALUES` rows:

```javascript
var add = @@insertXml(rows)<%
    INSERT INTO people(name, age) VALUES
    <foreach collection="rows" item="row" separator=",">
        (#{row.name}, #{row.age})
    </foreach>
%>;
return add([{'name':'Carol', 'age':20}, {'name':'David', 'age':28}]);
```

It generates `INSERT INTO people(name, age) VALUES (?, ?), (?, ?)`, with `['Carol', 20, 'David', 28]`, and returns `2`. Require a nonempty input and a database supporting multirow `VALUES`.

## Expression variables: bind {#bind}

`bind` evaluates the OGNL expression in `value` and stores it under `name`, without producing SQL. Supply both attributes. Subsequent conditions, placeholders, and rules can access the variable.

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

It generates `... WHERE name LIKE ? ORDER BY id` with `['%Ali%']`, returning Alice. `pattern` belongs to this SQL-building context, not the script's `var` scope. Avoid overwriting fragment parameters that are still needed.

## Shared SQL: include {#include}

The `refid` attribute references an application-registered SQL macro. Register `adult` as `age >= 18` following [SQL Fragments](../../dataway/engine/sql-macros.md), then call:

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'lower';
var find = @@selectXml()<%
    SELECT id, name FROM people WHERE <include refid="adult"/> ORDER BY id
%>;
return find();
```

It generates `... WHERE age >= 18 ORDER BY id`. A macro can reference the current fragment's parameters. This imports registered SQL text; it does not load MyBatis Mapper files or support Mapper namespaces or nested `property` declarations.

## Key queries: selectKey {#select-key}

`insertXml` accepts a top-level `selectKey` to query a key before or after an insert. `keyProperty` names the parameter to populate; `keyColumn` names the result column; `order` selects `BEFORE` or `AFTER`. Additional attributes are `statementType`, `timeout`, `fetchSize`, and `resultSetType`.

For complete examples and key writeback, see [Results and Generated Keys](results.md#select-key). This SQL tag does not provide MyBatis `resultType`, `resultMap`, or entity mapping.

## XML characters and composition {#xml-text}

In SQL text, encode `<` and `&` as `&lt;` and `&amp;`, or wrap plain SQL text in CDATA. Attribute expressions can use `&lt;` and `&gt;`; CDATA cannot replace attribute escaping.

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

Only wrap plain text in CDATA. An `<if>` inside CDATA is sent as SQL text, not executed as a tag. Tags can nest and combine with [SQL Rules](rules.md). Only the tags documented here are supported; unknown tags fail during parsing.

---
id: results
title: 6.5 Results and keys
---

Query rows are converted to objects using column labels. Unpacking determines whether the result is a list, object or scalar. Writes return affected-row counts.

## Unpacking

| Rows | `off` | `row` | `column` (default) |
| --- | --- | --- | --- |
| None | `[]` | `{}` | null |
| One row, one column | Object list | Object | Scalar |
| One row, several columns | Object list | Object | Object |
| Several rows | Object list | Object list | Object list |

Set `FRAGMENT_SQL_OPEN_PACKAGE = 'off'` for a stable list shape. Page data always remains a list.

## Column names

`FRAGMENT_SQL_COLUMN_CASE` supports `default`, `upper`, `lower` and `hump`.

```javascript
hint FRAGMENT_SQL_OPEN_PACKAGE = 'off';
hint FRAGMENT_SQL_COLUMN_CASE = 'hump';
var find = @@selectSql()<% SELECT id AS user_id, name AS user_name FROM people ORDER BY id %>;
return find();
```

The fields become `userId` and `userName`. SQL column labels take precedence over physical names. If normalized labels collide, the first value is retained; use distinct aliases.

## Key queries {#select-key}

For H2, create a sequence before running this example:

```sql
CREATE SEQUENCE people_ids START WITH 100;
```

```javascript
var add = @@insertXml(name, age)<%
    <selectKey keyProperty="newId" order="before">
        SELECT NEXT VALUE FOR people_ids
    </selectKey>
    INSERT INTO people(id, name, age) VALUES (#{newId}, #{name}, #{age})
%>;
var find = @@selectSql(name)<% SELECT id FROM people WHERE name = #{name} %>;
run add('Carol', 20);
return find('Carol');
```

`selectKey` runs before or after an insert on the same connection and writes its value to `keyProperty` in the fragment parameter map. `keyColumn` selects columns for multiple properties. `insertXml` still returns affected rows; writing a parameter does not make it the script return value.

JDBC `getGeneratedKeys()` is not currently connected to execution. Although the generated-key hints are parsed, they do not retrieve auto-generated IDs. Use an appropriate `selectKey` or explicit database query.

## Selected outputs

```javascript
hint bindOut = '#result-set-1';
var find = @@selectSql()<% SELECT count(*) FROM people %>;
return find();
```

Queries and general execution can select named results with `bindOut`. See [Procedures](procedures.md) for numbering and output parameters. Pagination cannot be combined with `bindOut`.

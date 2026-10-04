---
title: "5.2.1 Script support"
description: "Implement APIs with DataQL and SQL scripts."
---

DataQL reads parameters, combines queries and transforms results. SQL mode stores and executes original SQL for a database operation. See [Visual operations](../management.md) for creating definitions.

DataQL reads parameters with `${name}`; SQL binds values with `#{name}`. Requests supply inputs, and the common response handling formats returned values.

## DataQL scripts

```javascript title="Greeting API"
return {"message": ${message}};
```

```json title="Request parameters"
{"message": "Hello Dataway"}
```

The script returns `{"message":"Hello Dataway"}`. The default Structure puts it in `value`. See [DataQL](../../../dataql/overview.md) for syntax and built-in functions.

## SQL scripts

After adding `dataql-sqlproc` and providing a ConnectionProvider, select SQL for the API:

```sql title="Find a person by ID"
SELECT id, name FROM example_people WHERE id = #{id}
```

```json title="Request example"
{"id": 1}
```

`#{id}` binds the actual request value. Include each SQL parameter name in the saved request example; published SQL APIs use those names as fragment arguments.

Use `@@selectSql` to define a SQL query fragment inside DataQL, then process its result. This script uses the example application's `ds1` data source:

```javascript title="Call SQL from DataQL"
hint FRAGMENT_SQL_DATA_SOURCE = "ds1"

var person = @@selectSql(id)<%
    SELECT id AS "id", name AS "name"
    FROM example_people
    WHERE id = #{id}
%>;
return person(${id});
```

One script can combine several queries. See [Data source integration](../datasources.md) for source selection and [Transaction functions](../../../dataql/funx/transactions.md) for commits and rollbacks.

See [Visual operations](../management.md) for debugging, saving and publishing scripts. Debugging executes the script's database operations.

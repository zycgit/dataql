---
title: "9.11 SQL Type Handlers"
---

:::info[Requires the SQL executor]

This extension requires the [SQL executor](../dataql-engine/sql.md) (`dataql-sqlproc`). Configure [SQL data sources in Dataway](../capabilities/datasources.md) first.

:::

`TypeHandler` converts between Java values, JDBC parameters and result columns. Extend a built-in handler to customize one operation, or extend `AbstractTypeHandler` to implement parameter writing and result reading.

## Implement a handler

This application class extends `StringTypeHandler` to uppercase bound strings while preserving standard result reads.

```java
package com.example.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import net.hasor.dataql.sqlproc.types.string.StringTypeHandler;

/** Converts a bound string to uppercase while keeping standard string reads. */
public class UpperTextHandler extends StringTypeHandler {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, Object value, Integer jdbcType) throws SQLException {
        statement.setString(index, value.toString().toUpperCase(Locale.ROOT));
    }
}
```

## Register in Dataway

`config` is the application's registered `DatawayConfig`. Add this configuration before Dataway is created to make the handler the default for string parameters:

```java
import com.example.sql.UpperTextHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

config.configureHost(host -> {
    TypeHandlerRegistry.DEFAULT.register(String.class, new UpperTextHandler());
});
```

This configuration applies to Spring, Solon and Hasor. `TypeHandlerRegistry.DEFAULT` is shared, so registration affects string parameters using that registry. Register before the first SQL execution. With the standalone engine, call the same `register` method before creating queries.

The registry also accepts a JDBC type or a JDBC/Java type combination. Handlers can be shared across queries and should be thread-safe.

## Use in a script

Initialize the `people` table from [SQL execution](../../dataql/sql/execute.md) in the business database, then create a DataQL API in the console:

```javascript
var add = @@insertSql(name)<%
    INSERT INTO people(name, age) VALUES (#{name}, 20)
%>;
var find = @@selectSql()<% SELECT name FROM people WHERE age = 20 %>;
run add('carol');
return find();
```

Debug the API or publish and call it. The stored name is `CAROL`, and the script reads that value back.

## Select per parameter

For a single parameter, put the handler on the application classpath and specify its full class name in the placeholder. No default registration is required:

```sql
INSERT INTO people(name, age)
VALUES (#{name, typeHandler=com.example.sql.UpperTextHandler}, 20)
```

An explicit handler takes precedence over the default and applies only to that parameter binding. For null parameters, `jdbcType` can specify the database type. See [Type handling](../../dataql/sql/types.md) for parameter options.

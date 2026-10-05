---
title: "9.11 SQL Type Handlers"
---

:::info[Requires the SQL executor]

This extension requires the [SQL executor](../dataql-engine/sql.md), `dataql-sqlproc`. Configure [data-source access](../capabilities/datasources.md) before using it in Dataway.

:::

TypeHandler controls JDBC parameter binding and result reading. Use it for special column formats, business codes or proprietary database types. Standard conversions are described under [type handling](../../dataql/sql/types.md).

## Extension interface

TypeHandler has no generic parameter and defines four methods:

| Method | Purpose |
| --- | --- |
| setParameter(PreparedStatement, int, Object, Integer) | Bind an IN parameter, including null |
| getResult(ResultSet, String) | Read by column name |
| getResult(ResultSet, int) | Read by column index |
| getResult(CallableStatement, int) | Read an OUT parameter |

Usually extend `AbstractTypeHandler<T>`. It handles null binding and exception wrapping; implement `setNonNullParameter` and three `getNullableResult` methods. The subclass handles read-side nulls. Check `wasNull()` after primitive getters such as getInt or getBoolean.

Input has passed through the DataQL data model and is usually String, Number, Map, List or BinaryModel. Do not require an original entity, enum instance or Reader. Results must also be script-compatible; materialize JDBC LOBs and streams before the connection closes.

## Implement a handler

This handler writes uppercase text and reads stored text unchanged:

```java title="UpperTextHandler.java"
package com.example.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import net.hasor.dataql.sqlproc.types.AbstractTypeHandler;

public class UpperTextHandler extends AbstractTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index,
            String value, Integer jdbcType) throws SQLException {
        statement.setString(index, value.toUpperCase(Locale.ROOT));
    }

    @Override
    public String getNullableResult(ResultSet result, String columnName) throws SQLException {
        return result.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet result, int columnIndex) throws SQLException {
        return result.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return statement.getString(columnIndex);
    }
}
```

getString returns null for SQL NULL. The base class uses jdbcType for null binding, or infers VARCHAR from the String generic argument. Supply the JDBC type explicitly for nullable custom values.

## Use on one parameter

Put the handler on the application classpath and create a DataQL API:

```javascript
var query = @@selectSql(name)<%
    SELECT CAST(#{name, jdbcType=VARCHAR,
        typeHandler=com.example.sql.UpperTextHandler} AS VARCHAR)
%>;
return query('Alice');
```

This H2 example returns `"ALICE"`. No global registration or Bean-container registration is required: the SQL executor creates the handler by class name. Prefer a public no-argument constructor.

The option affects only that placeholder's binding. Reading a `SELECT name ...` column uses the registry and does not inherit the handler of a WHERE parameter.

## Register default mappings

For shared conversion behavior, initialize the registry where the application configures DatawayConfig:

```java title="Register in Dataway"
import java.sql.Types;
import com.example.sql.UpperTextHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;
import net.hasor.dataway.service.DatawayConfig;

DatawayConfig config = new DatawayConfig();
config.configureHost(host -> {
    UpperTextHandler handler = new UpperTextHandler();
    TypeHandlerRegistry registry = TypeHandlerRegistry.DEFAULT;
    registry.register(String.class, handler);
    registry.register(Types.VARCHAR, String.class, handler);
});
```

This covers both ordinary String parameters and String parameters with explicit VARCHAR. Existing CHAR, NVARCHAR and other combination mappings are more specific and are not overridden by the String default alone.

Spring, Solon and Hasor register this DatawayConfig through their usual application configuration. For standalone DataQL, call the same register methods before the first SQL query.

| Registration | Scope |
| --- | --- |
| register(Class, handler) | Default for a Java type |
| register(int jdbcType, handler) | JDBC-only lookup, including typed null parameters |
| register(int jdbcType, Class, handler) | A Java/JDBC combination, ahead of the Java-only default |

Result selection also considers the Java class reported by the driver. For VARCHAR string results, use the matching combination when appropriate; a JDBC-only registration cannot override a Java mapping that is selected earlier. See [selection order](../../dataql/sql/types/mappings.md).

TypeHandlerRegistry.DEFAULT is shared by SQL execution. Register before running queries; changes affect all queries using that registry. Prefer parameter options for local behavior. Registration is explicit: there is no type-mapping annotation or entity-property scan.

## PostgreSQL arrays {#postgres-array}

To fix the element type for empty or database-specific arrays, provide a no-argument PgArrayTypeHandler subclass:

```java title="IntArrayHandler.java"
package com.example.sql;

import net.hasor.dataql.sqlproc.types.array.PgArrayTypeHandler;

public class IntArrayHandler extends PgArrayTypeHandler {
    public IntArrayHandler() {
        super("int4", 1);
    }
}
```

```javascript
var query = @@selectSql(items)<%
    SELECT #{items, jdbcType=ARRAY, typeHandler=com.example.sql.IntArrayHandler}
%>;
return query([]);
```

This binds an empty PostgreSQL int4 array; `[1,null,3]` also works. Registration using `register(Types.ARRAY, Collection.class, new IntArrayHandler())` changes every Collection parameter with explicit ARRAY, so use it only when those parameters share an element type.

## Instances and resources

Handlers created by class name are normally cached and reused across queries. Implementations must be safe for concurrent use and should not retain the current parameter, ResultSet or Connection in fields.

The factory can also call a public constructor taking ResolvableType or Class. `@NoCache` disables automatic instance caching for such cases. An instance supplied to register is still reused for its registration scope. Ordinary conversion does not require type-dependent construction.

Do not close a caller's Statement or Connection. Release JDBC Array, SQLXML, Blob and other owned resources after completing the required operations. Returned values must not depend on closed JDBC streams.

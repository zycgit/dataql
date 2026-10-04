---
title: "6.3.3 Custom providers"
description: "Implement ApiDataAccessLayer to connect custom storage or extend an existing provider."
---

Implement `ApiDataAccessLayer` to connect an application storage service or wrap an existing provider with capabilities such as logging. Dataway uses this interface to query records and write batches.

## Implementation contract

Implement `ApiDataAccessLayer` with these contracts:

- `listObjects`: match filters using case-sensitive equality combined with AND; each record includes `ID` and `REVISION`.
- `write`: apply each ordered batch atomically, undoing the whole batch on failure. Creation starts at revision 1; updates compare and increment the revision; deletes compare it before removal.
- Preserve omitted fields and clear explicit nulls. INFO routes are unique by `(METHOD, PATH)`; route or revision conflicts throw `DataConflictException`.
- `create()` may return a custom `DataMutation` subtype. `configureMapping` applies name mappings; providers without mapping support reject non-empty overrides.

The store must ensure batch atomicity, for example through database transactions or Nacos snapshot CAS.

## Example: log metadata writes

This wrapper logs the number of changes after a batch write returns successfully. It delegates queries, name mappings and mutation creation to the underlying provider, preserving its transactions, concurrency checks and extension points.

```java title="LoggingDataAccessLayer.java"
import java.util.List;
import java.util.Map;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.DataMutation;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;

public class LoggingDataAccessLayer implements ApiDataAccessLayer {
    private static final System.Logger LOGGER = System.getLogger(LoggingDataAccessLayer.class.getName());
    private final ApiDataAccessLayer delegate;

    public LoggingDataAccessLayer(ApiDataAccessLayer delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Map<FieldDef, String>> listObjects(EntityType type, Map<FieldDef, String> conditions) {
        return this.delegate.listObjects(type, conditions);
    }

    @Override
    public void write(List<DataMutation> mutations) {
        this.delegate.write(mutations);
        LOGGER.log(System.Logger.Level.INFO, "Applied metadata batch: {0} mutations", mutations.size());
    }

    @Override
    public DataMutation create() {
        return this.delegate.create();
    }

    @Override
    public void configureMapping(Map<EntityType, String> tables, Map<EntityType, Map<FieldDef, String>> fields) {
        this.delegate.configureMapping(tables, fields);
    }
}
```

## Connect to Dataway

This example stores metadata through JDBC. `source` is a data source with the metadata tables already created:

```java title="DatawayConfig"
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;

ApiDataAccessLayer storage = new JdbcDataAccessLayer(source);
ApiDataAccessLayer metadata = new LoggingDataAccessLayer(storage);
Dataway dataway = new DatawayConfig()
        .dataAccessLayer(metadata)
        .createDataway();
```

For Nacos, replace `storage` with `NacosDataAccessLayer`. See bean registration in [Spring](../../integration/spring.md#metadata-storage), [Solon](../../integration/solon.md#metadata-storage) and [Hasor](../../integration/hasor.md#metadata-storage).

This example reuses an existing provider's storage capabilities. A new storage backend must implement queries and atomic batch writes, and verify revision conflicts, duplicate routes, batch rollback and clearing fields with explicit nulls.

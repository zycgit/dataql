---
title: "6.3.2 Nacos provider"
---

## Dependency and provisioning

```groovy
implementation 'net.hasor:dataway-meta-nacos:@project.docsVersion@'
```

The application creates ConfigService, configures its address, namespace and credentials, and manages its lifecycle.

See the complete [Spring Boot + Nacos example](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-nacos-example).

On first deployment, create a configuration in the target namespace with group `HASOR_DATAWAY` and dataId `dataway-store.json`. Generate its content from an empty snapshot:

```java
import net.hasor.dataway.dal.nacos.NacosSnapshot;

String initialContent = NacosSnapshot.empty().serialize();
```

Create an empty snapshot only during initial provisioning. The DAL requires the target configuration to exist.

## Create the store

```java
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.dal.nacos.NacosDataAccessLayer;

NacosDataAccessLayer metadata = new NacosDataAccessLayer(
        configService, "dataway-store.json", "HASOR_DATAWAY", 3000);
```

Arguments are the client, dataId, group and positive read timeout in milliseconds. Register the store as a bean or supply it to DatawayConfig.

## Snapshots and concurrency

INFO, RELEASE, route uniqueness and revisions share one JSON snapshot. A write reads it, validates the batch and publishes through CAS using the original content digest. CAS failure reports a conflict.

Concurrent writes can conflict and require reloading. Check the outcome after a timeout. The full history counts toward Nacos configuration size limits; use JDBC for larger stores.

Reads use ConfigService.getConfig and follow the client failover policy.

## Name mappings

Default entities are `INFO` and `RELEASE`. If renamed to `api_definitions` and `api_releases`, provision matching content:

```java
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;

String initialContent = JsonUtils.writeValueAsString(Map.of(
        "format", 1,
        "generation", "initial",
        "records", Map.of("api_definitions", Map.of(), "api_releases", Map.of())));
```

Configure tableMapping and fieldMapping in DatawayConfig; see [mapping rules](../mapping.md).

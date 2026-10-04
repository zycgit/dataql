---
title: "6.3.3 自定义提供者"
description: "实现 ApiDataAccessLayer 接入自定义存储，或扩展已有提供者。"
---

实现 `ApiDataAccessLayer` 可接入应用的存储服务，也可包装已有提供者，增加日志等能力。Dataway 通过该接口查询记录、批量保存变更。

## 实现约定

实现 `ApiDataAccessLayer` 时需遵循以下约定：

- `listObjects`：查询条件按大小写敏感的等值 AND 匹配，每条记录包含 `ID` 和 `REVISION`。
- `write`：按顺序原子执行整个批次，失败时整批撤销。创建记录从版本 1 开始，更新需比较并递增版本，删除需比较版本。
- 更新时，未传入的字段保持原值，显式 null 清空字段。INFO 的 `(METHOD, PATH)` 唯一，路由或版本冲突抛出 `DataConflictException`。
- `create()` 可返回自定义 `DataMutation` 子类型；`configureMapping` 处理名称映射，不支持映射时拒绝非空配置。

批次原子性由存储端保证，例如数据库事务或 Nacos 快照 CAS。

## 示例：记录元数据写入日志

下面的访问层在批次写入成功返回后记录变更数量。查询、名称映射和变更对象创建委托给原提供者，保留其事务、并发检查和扩展能力。

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

## 接入 Dataway

以下示例使用已建表的数据源 `source`，通过 JDBC 保存元数据：

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

使用 Nacos 时，将 `storage` 换为 `NacosDataAccessLayer`。框架中的 Bean 注册方式见 [Spring](../../integration/spring.md#元数据存储)、[Solon](../../integration/solon.md#元数据存储)、[Hasor](../../integration/hasor.md#元数据存储)。

该示例复用原提供者的存储能力。接入新的存储服务时，需自行实现查询和原子批次写入，并验证版本冲突、重复路由、批次失败回滚和显式 null 清空字段等行为。

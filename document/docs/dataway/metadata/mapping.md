---
title: "6.1 表与字段映射"
description: "了解元数据的默认存储结构，通过映射配置自定义表名、列名和 Nacos 实体键。"
---

Dataway 将元数据分为接口定义和发布记录两类。保存接口时更新定义，发布时生成记录；数据库和 Nacos 分别使用表和 JSON 对象保存这些数据。

## 默认存储结构

使用数据库时，默认有两张表：

- `interface_info`：保存可编辑的接口定义，`api_id` 为接口 ID，`api_path` 为请求路径，`api_script` 为原始脚本。
- `interface_release`：保存发布快照和历史，`pub_id` 为发布记录 ID，`pub_api_id` 关联接口，`pub_script` 保存发布时的原始脚本。

完整表结构见[数据库建表脚本](providers/jdbc.md#schema)，可按数据库类型选择并复制。

使用 Nacos 时，两类数据保存在同一份 JSON 快照的 `records` 中：`INFO` 存放接口定义，`RELEASE` 存放发布记录。每类数据按记录 ID 组织，记录内使用 `ID`、`PATH`、`SCRIPT` 等字段。例如接口 `hello` 的脚本位于 `records.INFO.hello.SCRIPT`。

这些表名、列名和实体键均可自定义。项目有统一命名规范，或已有存储使用不同名称时，可通过映射告诉 Dataway 到哪里读写数据；使用默认名称时无需配置。

## 配置映射

`EntityType` 表示数据类别，`INFO` 对应接口定义，`RELEASE` 对应发布记录；`FieldDef` 表示字段含义，如 `ID`、`SCRIPT`。通过 `DatawayConfig` 将它们对应到实际存储名称：

- `tableMapping`：设置数据库表名，或 Nacos `records` 下的实体键。
- `fieldMapping`：设置数据库列名，或 Nacos 记录内的字段名。

```java title="自定义存储名称"
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;

config.tableMapping(EntityType.INFO, "api_definitions")
        .tableMapping(EntityType.RELEASE, "api_releases")
        .fieldMapping(EntityType.INFO, FieldDef.ID, "definition_id")
        .fieldMapping(EntityType.RELEASE, FieldDef.API_ID, "definition_ref")
        .fieldMapping(EntityType.RELEASE, FieldDef.SCRIPT, "original_script");
```

上面的配置对应以下变化，两种存储使用相同的映射后名称：

| 配置对象 | 数据库：映射前 | Nacos：映射前 | 映射后 |
| --- | --- | --- | --- |
| `INFO` 表 / 实体 | `interface_info` | `INFO` | `api_definitions` |
| `RELEASE` 表 / 实体 | `interface_release` | `RELEASE` | `api_releases` |
| `INFO.ID` 字段 | `api_id` | `ID` | `definition_id` |
| `RELEASE.API_ID` 字段 | `pub_api_id` | `API_ID` | `definition_ref` |
| `RELEASE.SCRIPT` 字段 | `pub_script` | `SCRIPT` | `original_script` |

例如，发布脚本在数据库中的位置由 `interface_release.pub_script` 变为 `api_releases.original_script`；在 Nacos 中由 `records.RELEASE.<记录ID>.SCRIPT` 变为 `records.api_releases.<记录ID>.original_script`。未配置的名称沿用默认值。

在创建 Dataway 前完成配置，每个访问层实例使用一套映射。配置决定读写位置，实际表结构和已保存数据的名称需由应用同步调整。

## 存储字典

以下列出默认名称。`tableMapping` 配置表 / 实体名称，`fieldMapping` 配置字段名称。

### 表与实体

| EntityType | 保存内容 | 数据库表名 | Nacos 实体键 |
| --- | --- | --- | --- |
| `INFO` | 可编辑的接口定义 | `interface_info` | `records.INFO` |
| `RELEASE` | 发布快照和历史 | `interface_release` | `records.RELEASE` |

### 字段

Nacos 字段名与 `FieldDef` 相同；“—”表示该实体不使用此字段。

| FieldDef / Nacos 字段 | INFO 数据库列 | RELEASE 数据库列 |
| --- | --- | --- |
| `ID` | `api_id` | `pub_id` |
| `API_ID` | — | `pub_api_id` |
| `METHOD` | `api_method` | `pub_method` |
| `PATH` | `api_path` | `pub_path` |
| `STATUS` | `api_status` | `pub_status` |
| `COMMENT` | `api_comment` | `pub_comment` |
| `TYPE` | `api_type` | `pub_type` |
| `SCRIPT` | `api_script` | `pub_script` |
| `SCHEMA` | `api_schema` | `pub_schema` |
| `SAMPLE` | `api_sample` | `pub_sample` |
| `OPTION` | `api_option` | `pub_option` |
| `REVISION` | `api_revision` | `pub_revision` |
| `CREATE_TIME` | `api_create_time` | — |
| `GMT_TIME` | `api_gmt_time` | — |
| `RELEASE_TIME` | — | `pub_release_time` |

### 命名规则

| 项目 | 规则 |
| --- | --- |
| 数据库表名前缀 | 单参数构造方法使用默认表名；指定 `tablePrefix` 时加在默认表名前。`tableMapping` 设置完整表名，不再添加前缀 |
| 数据库表名格式 | 支持 `table`、`schema.table`、`catalog.schema.table`，具体取决于数据库 |
| 数据库名称字符 | 表名各段和列名以字母或下划线开头，仅包含字母、数字和下划线 |
| 数据库列名唯一性 | 同一实体的列名不能重复，比较时忽略大小写 |
| Nacos 名称 | 大小写敏感，点号为名称中的普通字符 |
| Nacos 名称唯一性 | 实体键不能重复，同一实体的字段名不能重复 |
| Nacos 初始快照 | 使用配置后的名称，见[初始化示例](providers/nacos.md#名称映射) |

空名称、不支持的字段和冲突映射会在初始化时被拒绝。

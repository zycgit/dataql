---
title: "6.1 Table and field mappings"
description: "Understand the default metadata layout and configure database table names, columns and Nacos entity keys."
---

Dataway stores API definitions and release records. Saving updates a definition; publishing creates a release record. Database storage uses tables, while Nacos uses JSON objects.

## Default storage layout

Database storage uses two tables by default:

- `interface_info`: editable definitions, with `api_id` for the API ID, `api_path` for the request path and `api_script` for the original script.
- `interface_release`: release snapshots and history, with `pub_id` for the release ID, `pub_api_id` linking to the API and `pub_script` holding the script at publication time.

See [table creation scripts](providers/jdbc.md#schema) for the complete schema, ready to copy for each database.

Nacos stores both kinds of data under `records` in one JSON snapshot: `INFO` holds definitions and `RELEASE` holds releases. Each collection is keyed by record ID, with fields such as `ID`, `PATH` and `SCRIPT`. For example, API `hello` stores its script at `records.INFO.hello.SCRIPT`.

These table names, columns and entity keys are configurable. Use mappings when your project has naming conventions or existing storage with different names. Default names require no configuration.

## Configure mappings

`EntityType` identifies the data category: `INFO` for definitions and `RELEASE` for releases. `FieldDef` identifies a field's meaning, such as `ID` or `SCRIPT`. Use `DatawayConfig` to map them to storage names:

- `tableMapping`: sets a database table name or an entity key under Nacos `records`.
- `fieldMapping`: sets a database column name or a field name within a Nacos record.

```java title="Custom storage names"
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;

config.tableMapping(EntityType.INFO, "api_definitions")
        .tableMapping(EntityType.RELEASE, "api_releases")
        .fieldMapping(EntityType.INFO, FieldDef.ID, "definition_id")
        .fieldMapping(EntityType.RELEASE, FieldDef.API_ID, "definition_ref")
        .fieldMapping(EntityType.RELEASE, FieldDef.SCRIPT, "original_script");
```

The configuration makes these changes. Both providers use the same mapped names:

| Target | Database: before | Nacos: before | After |
| --- | --- | --- | --- |
| `INFO` table / entity | `interface_info` | `INFO` | `api_definitions` |
| `RELEASE` table / entity | `interface_release` | `RELEASE` | `api_releases` |
| `INFO.ID` field | `api_id` | `ID` | `definition_id` |
| `RELEASE.API_ID` field | `pub_api_id` | `API_ID` | `definition_ref` |
| `RELEASE.SCRIPT` field | `pub_script` | `SCRIPT` | `original_script` |

For example, a release script moves from `interface_release.pub_script` to `api_releases.original_script` in the database mapping, or from `records.RELEASE.<recordID>.SCRIPT` to `records.api_releases.<recordID>.original_script` in the Nacos mapping. Unspecified names keep their defaults.

Configure mappings before creating Dataway, using one mapping configuration per access-layer instance. Mappings determine where Dataway reads and writes; the application must update the actual schema and existing data names accordingly.

## Storage dictionary

These are the default names. Use `tableMapping` for tables / entities and `fieldMapping` for fields.

### Tables and entities

| EntityType | Content | Database table | Nacos entity key |
| --- | --- | --- | --- |
| `INFO` | Editable API definitions | `interface_info` | `records.INFO` |
| `RELEASE` | Release snapshots and history | `interface_release` | `records.RELEASE` |

### Fields

Nacos field names match `FieldDef`. A dash indicates that the entity does not use that field.

| FieldDef / Nacos field | INFO database column | RELEASE database column |
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

### Naming rules

| Item | Rule |
| --- | --- |
| Database table prefix | Single-argument constructors use default names. `tablePrefix` is prepended to defaults; `tableMapping` specifies the complete name without adding a prefix |
| Database table format | `table`, `schema.table` or `catalog.schema.table`, subject to database support |
| Database name characters | Each table-name segment and column starts with a letter or underscore and contains only letters, digits and underscores |
| Database column uniqueness | Names must be unique within each entity, compared case-insensitively |
| Nacos names | Case-sensitive; dots are ordinary characters |
| Nacos name uniqueness | Entity keys must be unique, as must field names within each entity |
| Nacos initial snapshot | Use mapped names; see the [initialization example](providers/nacos.md#name-mappings) |

Empty names, unsupported fields and conflicting mappings fail during initialization.

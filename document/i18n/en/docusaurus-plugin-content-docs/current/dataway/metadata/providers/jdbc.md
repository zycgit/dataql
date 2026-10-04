---
title: "6.3.1 Database provider"
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Dependency and schema

```groovy
implementation 'net.hasor:dataway-meta-jdbc:@project.docsVersion@'
```

The application supplies the JDBC driver and DataSource. This module uses dbVisitor to access the database.

## Table creation scripts {#schema}

Select your database, then copy and run the script to create `interface_info`, `interface_release` and their indexes. Adjust names to match any [table and field mappings](../mapping.md).

<Tabs groupId="metadata-database">
<TabItem value="mysql" label="MySQL / MariaDB" default>

```sql
CREATE TABLE interface_info (
    api_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    api_method VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    api_path VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    api_status VARCHAR(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    api_comment VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    api_type VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    api_script MEDIUMTEXT NOT NULL,
    api_schema MEDIUMTEXT NOT NULL,
    api_sample MEDIUMTEXT NOT NULL,
    api_option MEDIUMTEXT NOT NULL,
    api_create_time VARCHAR(32) NOT NULL,
    api_gmt_time VARCHAR(32) NOT NULL,
    api_revision BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE interface_release (
    pub_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    pub_api_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    pub_method VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    pub_path VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    pub_status VARCHAR(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    pub_comment VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    pub_type VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    pub_script MEDIUMTEXT NOT NULL,
    pub_schema MEDIUMTEXT NOT NULL,
    pub_sample MEDIUMTEXT NOT NULL,
    pub_option MEDIUMTEXT NOT NULL,
    pub_release_time VARCHAR(32) NOT NULL,
    pub_revision BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE UNIQUE INDEX uk_interface_info ON interface_info (api_method, api_path);
CREATE INDEX idx_interface_release_api ON interface_release (pub_api_id);
CREATE INDEX idx_interface_release_path ON interface_release (pub_method, pub_path, pub_status);
```

</TabItem>
<TabItem value="postgresql" label="PostgreSQL">

```sql
CREATE TABLE interface_info (
    api_id VARCHAR(64) COLLATE "C" NOT NULL PRIMARY KEY,
    api_method VARCHAR(12) COLLATE "C" NOT NULL,
    api_path VARCHAR(512) COLLATE "C" NOT NULL,
    api_status VARCHAR(4) COLLATE "C" NOT NULL,
    api_comment VARCHAR(255) COLLATE "C" NOT NULL,
    api_type VARCHAR(24) COLLATE "C" NOT NULL,
    api_script TEXT NOT NULL,
    api_schema TEXT NOT NULL,
    api_sample TEXT NOT NULL,
    api_option TEXT NOT NULL,
    api_create_time VARCHAR(32) NOT NULL,
    api_gmt_time VARCHAR(32) NOT NULL,
    api_revision BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE interface_release (
    pub_id VARCHAR(64) COLLATE "C" NOT NULL PRIMARY KEY,
    pub_api_id VARCHAR(64) COLLATE "C" NOT NULL,
    pub_method VARCHAR(12) COLLATE "C" NOT NULL,
    pub_path VARCHAR(512) COLLATE "C" NOT NULL,
    pub_status VARCHAR(4) COLLATE "C" NOT NULL,
    pub_comment VARCHAR(255) COLLATE "C" NOT NULL,
    pub_type VARCHAR(24) COLLATE "C" NOT NULL,
    pub_script TEXT NOT NULL,
    pub_schema TEXT NOT NULL,
    pub_sample TEXT NOT NULL,
    pub_option TEXT NOT NULL,
    pub_release_time VARCHAR(32) NOT NULL,
    pub_revision BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX uk_interface_info ON interface_info (api_method, api_path);
CREATE INDEX idx_interface_release_api ON interface_release (pub_api_id);
CREATE INDEX idx_interface_release_path ON interface_release (pub_method, pub_path, pub_status);
```

</TabItem>
<TabItem value="oracle" label="Oracle">

Use connections with `NLS_COMP=BINARY` and `NLS_SORT=BINARY` for case-sensitive comparisons. The database character set must support script and description text; Oracle stores empty descriptions as NULL.

```sql
CREATE TABLE interface_info (
    api_id VARCHAR2(64 CHAR) NOT NULL PRIMARY KEY,
    api_method VARCHAR2(12 CHAR) NOT NULL,
    api_path VARCHAR2(512 CHAR) NOT NULL,
    api_status VARCHAR2(4 CHAR) NOT NULL,
    api_comment VARCHAR2(255 CHAR),
    api_type VARCHAR2(24 CHAR) NOT NULL,
    api_script CLOB NOT NULL,
    api_schema CLOB NOT NULL,
    api_sample CLOB NOT NULL,
    api_option CLOB NOT NULL,
    api_create_time VARCHAR2(32 CHAR) NOT NULL,
    api_gmt_time VARCHAR2(32 CHAR) NOT NULL,
    api_revision NUMBER(19) DEFAULT 1 NOT NULL
);

CREATE TABLE interface_release (
    pub_id VARCHAR2(64 CHAR) NOT NULL PRIMARY KEY,
    pub_api_id VARCHAR2(64 CHAR) NOT NULL,
    pub_method VARCHAR2(12 CHAR) NOT NULL,
    pub_path VARCHAR2(512 CHAR) NOT NULL,
    pub_status VARCHAR2(4 CHAR) NOT NULL,
    pub_comment VARCHAR2(255 CHAR),
    pub_type VARCHAR2(24 CHAR) NOT NULL,
    pub_script CLOB NOT NULL,
    pub_schema CLOB NOT NULL,
    pub_sample CLOB NOT NULL,
    pub_option CLOB NOT NULL,
    pub_release_time VARCHAR2(32 CHAR) NOT NULL,
    pub_revision NUMBER(19) DEFAULT 1 NOT NULL
);

CREATE UNIQUE INDEX uk_interface_info ON interface_info (api_method, api_path);
CREATE INDEX idx_interface_release_api ON interface_release (pub_api_id);
CREATE INDEX idx_interface_release_path ON interface_release (pub_method, pub_path, pub_status);
```

</TabItem>
<TabItem value="sqlserver" label="SQL Server">

For SQL Server 2016 or later, using Unicode columns and binary collations.

```sql
CREATE TABLE interface_info (
    api_id NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    api_method NVARCHAR(12) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_path NVARCHAR(512) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_status NVARCHAR(4) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_comment NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_type NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_script NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_schema NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_sample NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_option NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_create_time NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_gmt_time NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_revision BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE interface_release (
    pub_id NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    pub_api_id NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_method NVARCHAR(12) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_path NVARCHAR(512) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_status NVARCHAR(4) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_comment NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_type NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_script NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_schema NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_sample NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_option NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_release_time NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_revision BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE NONCLUSTERED INDEX uk_interface_info ON interface_info (api_method, api_path);
CREATE NONCLUSTERED INDEX idx_interface_release_api ON interface_release (pub_api_id);
CREATE NONCLUSTERED INDEX idx_interface_release_path ON interface_release (pub_method, pub_path, pub_status);
```

</TabItem>
<TabItem value="h2" label="H2">

```sql
CREATE TABLE interface_info (
    api_id VARCHAR(64) NOT NULL PRIMARY KEY,
    api_method VARCHAR(12) NOT NULL,
    api_path VARCHAR(512) NOT NULL,
    api_status VARCHAR(4) NOT NULL,
    api_comment VARCHAR(255) NOT NULL,
    api_type VARCHAR(24) NOT NULL,
    api_script CLOB NOT NULL,
    api_schema CLOB NOT NULL,
    api_sample CLOB NOT NULL,
    api_option CLOB NOT NULL,
    api_create_time VARCHAR(32) NOT NULL,
    api_gmt_time VARCHAR(32) NOT NULL,
    api_revision BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE interface_release (
    pub_id VARCHAR(64) NOT NULL PRIMARY KEY,
    pub_api_id VARCHAR(64) NOT NULL,
    pub_method VARCHAR(12) NOT NULL,
    pub_path VARCHAR(512) NOT NULL,
    pub_status VARCHAR(4) NOT NULL,
    pub_comment VARCHAR(255) NOT NULL,
    pub_type VARCHAR(24) NOT NULL,
    pub_script CLOB NOT NULL,
    pub_schema CLOB NOT NULL,
    pub_sample CLOB NOT NULL,
    pub_option CLOB NOT NULL,
    pub_release_time VARCHAR(32) NOT NULL,
    pub_revision BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX uk_interface_info ON interface_info (api_method, api_path);
CREATE INDEX idx_interface_release_api ON interface_release (pub_api_id);
CREATE INDEX idx_interface_release_path ON interface_release (pub_method, pub_path, pub_status);
```

</TabItem>
</Tabs>

The JAR also includes `mysql.sql`, `postgresql.sql`, `oracle.sql`, `sqlserver.sql` and `h2.sql` under `META-INF/dataway/schema/`.

## Create the store

```java
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;

ApiDataAccessLayer metadata = new JdbcDataAccessLayer(source);
```

Configure the store through `DatawayConfig.dataAccessLayer(metadata)`. Single-argument constructors use `interface_info` and `interface_release`. To add a prefix, use `new JdbcDataAccessLayer(source, "dw_")`; create matching tables named `dw_interface_info` and `dw_interface_release`.

Use the primary or a dedicated database. Default construction uses local transactions; pass JdbcExecutor for [host transactions](../transactions.md).

## Consistency

Each batch runs on one connection. Updates and deletes compare revisions; stale versions and duplicate routes report conflicts. Method, path and identity columns must compare case-sensitively; follow the selected script and its connection requirements.

Use [mappings](../mapping.md) for custom storage names.

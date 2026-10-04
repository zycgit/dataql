---
title: "6.3.1 数据库提供者"
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## 引入和准备

```groovy
implementation 'net.hasor:dataway-meta-jdbc:@project.docsVersion@'
```

应用提供 JDBC 驱动和 DataSource，本模块使用 dbVisitor 访问数据库。

## 建表脚本 {#schema}

选择数据库，复制并执行以下脚本，创建 `interface_info`、`interface_release` 及索引。使用自定义名称时，按[表与字段映射](../mapping.md)同步调整脚本。

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

连接使用 `NLS_COMP=BINARY`、`NLS_SORT=BINARY`，保持大小写敏感比较。数据库字符集需支持脚本和备注中的字符；空备注按 Oracle 规则存为 NULL。

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

适用于 SQL Server 2016 及以上版本，使用 Unicode 字段和二进制排序规则。

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

脚本也随 JAR 发布，位于 `META-INF/dataway/schema/` 目录下的 `mysql.sql`、`postgresql.sql`、`oracle.sql`、`sqlserver.sql`、`h2.sql`。

## 创建访问层

```java
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;

ApiDataAccessLayer metadata = new JdbcDataAccessLayer(source);
```

通过 `DatawayConfig.dataAccessLayer(metadata)` 配置访问层。单参数构造方法使用默认表名 `interface_info`、`interface_release`；需要前缀时使用 `new JdbcDataAccessLayer(source, "dw_")`，对应表名为 `dw_interface_info`、`dw_interface_release`，建表脚本需使用相同名称。

元数据可使用主库或独立库。默认使用本地事务，加入[宿主事务](../transactions.md)时传入 JdbcExecutor。

## 一致性

批次在同一连接中执行，更新与删除使用版本条件；版本冲突或重复路由会报告冲突。方法、路径和标识字段应使用大小写敏感比较，按所选数据库脚本及连接要求配置。

自定义存储名称见[表字段映射](../mapping.md)。

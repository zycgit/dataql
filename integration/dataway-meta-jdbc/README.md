# Dataway JDBC 元数据存储

JDBC 元数据存储由可选模块 `dataway-meta-jdbc` 提供，整个 `dal.jdbc` 包、dbVisitor 及建表/升级脚本均在该模块中。核心、Hasor 集成及 Nacos 扩展不依赖它。Spring/Solon 集成包含可选 JDBC 事务适配代码，但不传递引入 JDBC 存储、dbVisitor 或宿主事务库。

使用 `new JdbcDataAccessLayer(executor, tablePrefix)` 将 `JdbcExecutor` 与访问层组合，再调用 `builder.dataAccessLayer(access)`。`JdbcCallback` 的连接只供 DAL 借用，提交、回滚和释放由执行器负责。查询和整批写入均经过执行器。

- 纯内嵌或无宿主事务组件：引入 `dataway-meta-jdbc`，使用 `new JdbcDataAccessLayer(dataSource, "")`，内部使用 `LocalJdbcExecutor`。
- Spring：执行器随 `dataway-spring` 提供，使用 `net.hasor.dataway.spring.SpringJdbcExecutor(dataSource, transactionManager)`，按 REQUIRED 加入宿主事务。
- Solon：执行器随 `dataway-solon` 提供，使用 `net.hasor.dataway.solon.SolonJdbcExecutor(dataSource)`，接入宿主事务组件。
- Hasor/Guice：应用显式组合自己的执行器和访问层，再传入 Builder；也可通过 SPI 选择 JDBC：优先查找宿主执行器，否则使用本地执行器。

框架通过 `dataway.metadata.type` 选择 SPI 存储，也支持显式传入 `ApiDataAccessLayer` 或已有服务。Builder 的 `dataSource(...)` 只配置脚本运行时，不再决定元数据存储。SQL 脚本的 `fx.transaction` 仍由 sqlproc 管理，与元数据存储事务独立。

## 引用与装配

```groovy
implementation 'net.hasor:dataway-meta-jdbc:4.3.0-SNAPSHOT'
```

```java
import net.hasor.dataway.Dataway;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;

var access = new JdbcDataAccessLayer(metadataDataSource, "");
var dataway = Dataway.builder().dataAccessLayer(access).build();
```

连接可以与脚本数据源不同。使用前执行本模块 `META-INF/dataway/schema/{h2,mysql,postgresql}.sql`，旧表执行 `schema/upgrade/` 下对应脚本。不会自动建表。

`JdbcExecutor`/`JdbcCallback` 和默认的 `LocalJdbcExecutor` 均位于本模块，可自定义事务执行方式。`JdbcDataAccessLayer` 复用 dbVisitor `LambdaTemplate`；不在内部提交、回滚或关闭执行器提供的连接。

配置项、默认值和 SPI 扩展方式见[元信息存储配置](../../document/docs/integration/embedded-configuration.md#元信息存储选择jdk-spi)。

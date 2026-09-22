# Dataway JDBC Solon 事务适配

事务执行器随 `dataway-solon` 提供，以可选依赖方式编译；应用按需引入元信息扩展及宿主事务库。支持配置选择 SPI，也可保留显式装配。

```groovy
implementation 'net.hasor:dataway-solon:4.3.0-SNAPSHOT'
implementation 'net.hasor:dataway-meta-jdbc:4.3.0-SNAPSHOT'
implementation 'org.noear:solon-data:4.1.0'
```

```java
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.solon.SolonJdbcExecutor;
var executor = new SolonJdbcExecutor(source);
var access = new JdbcDataAccessLayer(executor, "");
var builder = Dataway.builder().dataAccessLayer(access);
// 应用启动时安装 new DatawayPlugin(builder)。
```

元数据操作参与宿主事务，失败回滚或标记 rollback-only。无需宿主事务时可以只使用 `dataway-meta-jdbc` 的本地执行器。数据源和客户端生命周期仍由应用负责。

配置项、默认值和 SPI 扩展方式见[元信息存储配置](../../document/docs/integration/embedded-configuration.md#元信息存储选择jdk-spi)。

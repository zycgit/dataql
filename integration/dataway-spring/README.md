# Dataway JDBC Spring 事务适配

事务执行器随 `dataway-spring` 提供，以可选依赖方式编译；应用按需引入元信息扩展及宿主事务库。支持配置选择 SPI，也可保留显式装配。

```groovy
implementation 'net.hasor:dataway-spring:4.3.0-SNAPSHOT'
implementation 'net.hasor:dataway-meta-jdbc:4.3.0-SNAPSHOT'
implementation 'org.springframework:spring-jdbc'
```

```java
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.spring.SpringJdbcExecutor;
import net.hasor.dataway.spi.DatawayConfigurer;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

// 放在应用的 @Configuration 类中，数据源和事务管理器由应用提供。
@Bean
DatawayConfigurer metadata(DataSource source, PlatformTransactionManager manager) {
    var executor = new SpringJdbcExecutor(source, manager);
    var access = new JdbcDataAccessLayer(executor, "");
    return builder -> builder.dataAccessLayer(access);
}
```

元数据操作参与宿主事务，失败回滚或标记 rollback-only。无需宿主事务时可以只使用 `dataway-meta-jdbc` 的本地执行器。数据源和客户端生命周期仍由应用负责。

配置项、默认值和 SPI 扩展方式见[元信息存储配置](../../document/docs/integration/embedded-configuration.md#元信息存储选择jdk-spi)。

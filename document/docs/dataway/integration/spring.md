---
title: "3.1 Spring 整合"
hide_table_of_contents: false
description: "Spring 接入 Dataway：依赖、配置、SQL 数据源与事务。"
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Spring 概述

Spring 提供依赖注入、MVC 和事务管理，Spring Boot 提供启动与自动配置。本节使用 Spring Boot 4.1。

项目地址：[官方网站](https://spring.io/)

## 特性

- 通过自动配置创建 Dataway，并注册到 Spring MVC。
- 复用 MVC 拦截器、登录身份和异常处理。
- 通过 Spring 容器提供元数据访问层及单数据源、多数据源 SQL 连接。

## 配置方法 {#配置与装配}

### 引入依赖

引入框架整合、JDBC 元数据和 SQL 扩展，另行配置连接池与 JDBC 驱动。

<Tabs groupId="build-tool">
<TabItem value="maven" label="Maven" default>

```xml
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-spring</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-meta-jdbc</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataql-sqlproc</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
```

</TabItem>
<TabItem value="gradle" label="Gradle">

```groovy
implementation 'net.hasor:dataway-spring:@project.docsVersion@'
implementation 'net.hasor:dataway-meta-jdbc:@project.docsVersion@'
implementation 'net.hasor:dataql-sqlproc:@project.docsVersion@'
```

</TabItem>
</Tabs>

### 开放入口

在宿主配置文件中开放入口，三个开关默认均为 `false`。

<Tabs groupId="spring-config">
<TabItem value="yaml" label="application.yml" default>

```yaml title="application.yml"
dataway:
  api-enabled: true
  admin-enabled: true
  docs-enabled: true
```

</TabItem>
<TabItem value="properties" label="application.properties">

```properties
dataway.api-enabled=true
dataway.admin-enabled=true
dataway.docs-enabled=true
```

</TabItem>
</Tabs>

### 注册服务

```java title="DatawayConfiguration.java"
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.service.DatawayConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DatawayConfiguration {
    @Bean
    public DatawayConfig datawayConfig(IdentityProvider identityProvider, ConnectionProvider connections) {
        return new DatawayConfig()
                .identityProvider(identityProvider)
                .attachment(ConnectionProvider.class, connections);
    }

    @Bean
    public IdentityProvider identityProvider() {
        return new RequestIdentityProvider(LoginInterceptor.IDENTITY_ATTRIBUTE);
    }
}
```

- `IdentityProvider`：由上面的 `identityProvider()` 方法创建 `RequestIdentityProvider` 并注册为 Bean。
- `ConnectionProvider`：由 [SQL 数据源](#sql-数据源) 中的 `DatawayConfiguration.connectionProvider()` 方法注册为 Bean。

### 元数据存储

Spring 自动配置按类型获取 `ApiDataAccessLayer` Bean。先按[数据库提供者](../metadata/providers/jdbc.md)创建表，将元数据 DataSource 注入下面的 Bean，默认使用独立事务：

```java title="MetadataConfiguration.java"
import javax.sql.DataSource;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MetadataConfiguration {
    @Bean
    public ApiDataAccessLayer metadata(DataSource source) {
        return new JdbcDataAccessLayer(source);
    }
}
```

接入宿主事务时，引入 `spring-jdbc`，为 `source` 配置匹配的 Spring 事务管理器，并替换上面的 `metadata` 方法：

```java title="接入宿主事务"
import net.hasor.dataway.spring.SpringJdbcExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Bean
public ApiDataAccessLayer metadata(DataSource source, PlatformTransactionManager transactionManager) {
    return new JdbcDataAccessLayer(new SpringJdbcExecutor(source, transactionManager));
}
```

借助 Spring 的 `@Transactional` 或 `TransactionTemplate`，将元数据操作与应用业务纳入同一事务。选择依据见[事务整合](../metadata/transactions.md)。

### 访问鉴权

应用在每次请求中校验 JWT，将 `UserIdentity` 写入请求属性 `host.identity`，供 `RequestIdentityProvider` 读取。

```java title="注册拦截器"
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Configuration(proxyBeanMethods = false)
public class WebConfiguration {
    @Bean
    public MappedInterceptor loginInterceptor() {
        // 适用于默认和自定义的 HandlerMapping。
        return new MappedInterceptor(null, new LoginInterceptor());
    }
}
```

JWT 校验和身份写入见示例工程的 [LoginInterceptor.java](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/config/auth/LoginInterceptor.java)。

```java title="请求拦截"
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.hasor.dataway.authorization.UserIdentity;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {
    public static final String IDENTITY_ATTRIBUTE = "host.identity";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("/session/login".equals(path)) {
            return true;
        }

        // 应用已完成 JWT 校验和身份写入，此处读取当前请求的身份。
        Object identity = request.getAttribute(IDENTITY_ATTRIBUTE);
        if (!(identity instanceof UserIdentity user) || !user.authenticated()) {
            response.setStatus(401);
            return false;
        }

        return true;
    }
}
```

## SQL 数据源

### 数据源接入

`ConnectionProvider.findConnection(name, hints)` 为 SQL 脚本和 SQL 片段提供数据库连接。

- `name`：由 `FRAGMENT_SQL_DATA_SOURCE` 指定的数据源名称，未指定时使用主库。
- `hints`：本次执行的 Hint 配置，可用于自定义连接选择。
- 返回值：可用的 JDBC 连接；无法提供时返回 `null`。连接由 SQL 模块在执行后释放。

数据源由 Spring Boot 或 `@Bean` 创建。主库使用 `@Primary` 标记，其他库使用 `@Bean("ds1")`、`@Bean("ds2")` 命名。

```java title="DatawayConfiguration.java"
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.spring.SpringTransactionProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@Bean
public ConnectionProvider connectionProvider(ApplicationContext context) {
    return new SpringTransactionProvider(context);
}
```

脚本使用 `hint FRAGMENT_SQL_DATA_SOURCE = "ds1"` 选择 ds1。

### 使用事务 {#sql-事务}

`TransactionUdfSource` 提供脚本事务函数，前面注册的 `SpringTransactionProvider` 将这些调用交给 Spring 的事务管理。

- `required`：加入当前数据源的已有 Spring 事务，没有时创建事务。
- `requiresNew`：挂起已有事务，创建独立事务。
- `nested`：已有事务中使用保存点，没有时创建事务；需要 JDBC 驱动支持保存点。

以下示例将 ds1 的 `example_people` 表中，用户 1 的余额转出 5，转入用户 2。请求参数为 `{"fromId":1,"toId":2,"amount":5}`。

```javascript title="使用脚本事务转账"
hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
var changeBalance = @@updateSql(id, amount)<%
    UPDATE example_people SET balance = balance + #{amount} WHERE id = #{id}
%>;
if (${amount} <= 0) {
    throw 400, "Amount must be positive";
}
return tran.required(() -> {
    if (changeBalance(${fromId}, 0 - ${amount}) != 1) {
        throw 404, "Source account not found";
    }
    if (changeBalance(${toId}, ${amount}) != 1) {
        throw 404, "Target account not found";
    }
    return true;
});
```

两次更新全部成功后提交，任意一次抛出异常则一起回滚。事务按数据源管理，多数据源操作不保证共同提交。

### 事务整合

通过 `SpringTransactionProvider` 接入 Spring 事务。

应用引入 `org.springframework:spring-jdbc`，每个业务数据源配置对应的事务管理器。已有事务管理器时直接复用，示例的三个数据源配置如下：

```java title="DatabaseConfiguration.java：事务管理器"
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class DatabaseConfiguration {
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource source) {
        return new DataSourceTransactionManager(source);
    }

    @Bean
    public PlatformTransactionManager ds1TransactionManager(@Qualifier("ds1") DataSource source) {
        return new DataSourceTransactionManager(source);
    }

    @Bean
    public PlatformTransactionManager ds2TransactionManager(@Qualifier("ds2") DataSource source) {
        return new DataSourceTransactionManager(source);
    }
}
```

然后在前面的 `DatawayConfiguration` 中注册提供者，并通过 `attachment` 传给 Dataway：

```java title="DatawayConfiguration.java：接入 Spring 事务"
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.spring.SpringTransactionProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DatawayConfiguration {
    @Bean
    public DatawayConfig datawayConfig(IdentityProvider identityProvider, ConnectionProvider connections) {
        return new DatawayConfig()
                .identityProvider(identityProvider)
                // SQL 连接和脚本事务函数都使用下面注册的 SpringTransactionProvider。
                .attachment(ConnectionProvider.class, connections);
    }

    @Bean
    public ConnectionProvider connectionProvider(ApplicationContext context) {
        // 从 Spring 容器获取数据源及其对应的 JDBC 事务管理器。
        return new SpringTransactionProvider(context);
    }
}
```

事务管理器与提供者需使用相同的 DataSource 实例。

配置完成后，借助 Spring 的事务能力统一管理 SQL、脚本事务函数和应用数据库操作。

示例已发布 `/api/transfer`，请求 `{"fromId":1,"toId":2,"amount":5}` 可完成转账；将 `toId` 改为不存在的账号，已扣除的余额也会回滚。

## 配置项说明

### 服务装配

`DatawayAutoConfiguration` 通过 Spring Environment 读取入口配置，使用容器中的配置和服务创建 Dataway。

| 类型 | 配置方式与作用 |
| --- | --- |
| `DatawayAutoConfiguration` | Spring Boot 自动加载；在 Servlet Web 应用中注册 MVC 入口 |
| `DatawayConfig` | 使用容器中的 Bean；未提供时使用默认配置 |
| `ApiDataAccessLayer` | 优先使用 `dataAccessLayer(...)` 显式设置的实例，否则按类型获取 Spring Bean；多个实现时使用 `@Primary` 或显式设置 |
| `Dataway` | 未提供该 Bean 时由适配器创建；应用可注入它获取四个 Handler 和 `AdminService` |
| `ConnectionProvider` | SQL 连接提供接口；将实现注册为 Spring Bean，注入 `DatawayConfig` 配置方法，再通过 `attachment(ConnectionProvider.class, provider)` 接入 SQL 模块 |
| `SpringTransactionProvider` | Spring 事务实现；使用 `new SpringTransactionProvider(context)` 创建并作为 `ConnectionProvider` Bean 装配。普通 SQL 使用 Spring 事务连接，`tran.*` 调用与数据源对应的 Spring JDBC 事务管理器 |
| `SpringJdbcExecutor` | 可选，传给 `JdbcDataAccessLayer` 使元数据操作加入 Spring 事务，见[元数据存储](#元数据存储) |

### 入口配置

以下为整合模块读取的全部 `dataway.*` 配置。

| 配置项 | 类型 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `dataway.api-enabled` | Boolean | `false` | 注册已发布 API 的调用入口 |
| `dataway.api-prefix` | String | `/api` | 业务 API 路由前缀，随 `api-enabled` 生效 |
| `dataway.admin-enabled` | Boolean | `false` | 同时注册管理 API、控制台页面和资源 |
| `dataway.admin-prefix` | String | `/admin/api` | 管理 API 路由前缀，随 `admin-enabled` 生效 |
| `dataway.admin-ui` | String | `/admin` | 控制台页面和资源前缀，随 `admin-enabled` 生效 |
| `dataway.docs-enabled` | Boolean | `false` | 注册 Swagger、OpenAPI 规范文档入口 |
| `dataway.docs-prefix` | String | `/docs` | 规范文档路由前缀，随 `docs-enabled` 生效 |

三个开关独立设置，默认均为 `false`。前缀相对于宿主 context path，以 `/` 开头、不以 `/` 结尾，仅包含路径。完整默认配置如下：

<Tabs groupId="spring-config">
<TabItem value="yaml" label="application.yml" default>

```yaml title="application.yml"
dataway:
  api-enabled: false
  api-prefix: /api
  admin-enabled: false
  admin-prefix: /admin/api
  admin-ui: /admin
  docs-enabled: false
  docs-prefix: /docs
```

</TabItem>
<TabItem value="properties" label="application.properties">

```properties title="application.properties"
dataway.api-enabled=false
dataway.api-prefix=/api
dataway.admin-enabled=false
dataway.admin-prefix=/admin/api
dataway.admin-ui=/admin
dataway.docs-enabled=false
dataway.docs-prefix=/docs
```

</TabItem>
</Tabs>

### 核心配置

常用接入项如下，完整方法、默认值和约束见 [10.1 DatawayConfig](../configuration/core.md)。

| 方法 | 作用 |
| --- | --- |
| `dataAccessLayer(layer)` | 指定元数据访问层；未设置时从容器获取 `ApiDataAccessLayer` Bean |
| `identityProvider(provider)` | 注册身份提供者，获取当前请求的用户身份 |
| `attachment(ConnectionProvider.class, provider)` | 注册 SQL 的连接与事务提供者 |

### 文件上传

Spring 优先复用 `MultipartHttpServletRequest`，否则使用 Servlet multipart，宿主需启用上传配置。 上传缓存通过 [DatawayConfig](../configuration/core.md#upload) 设置，请求大小上限由宿主配置。

### 控制台配置

整合模块根据 `dataway.admin-ui`、`dataway.admin-prefix` 和 `dataway.api-prefix` 自动生成 `initializer.js`。修改后端配置并重启后，控制台自动使用对应地址，无需单独修改前端。业务入口关闭时不提供调用地址。

初始化脚本调用 `DatawayUI(...)`，参数如下：

| 参数 | 默认初始化脚本中的值 | 作用 |
| --- | --- | --- |
| `adminApi` | `api/` | 浏览器访问管理 API 的基础地址，必填 |
| `api` | `../api/` | 浏览器调用已发布 API 的基础地址，省略后不能在列表页调用 |

地址相对控制台解析，保留 context path 和代理添加的公共前缀。代理分别改写各入口地址时，可提供自定义初始化脚本。API 文档的服务地址仍通过 `DatawayConfig.documentServer(...)` 设置。见[控制台部署](../configuration/console.md)。

## 样例工程 {#example}

### JDBC 示例 {#jdbc-example}

:::info[Example]

[Spring Boot + JDBC 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)使用 JDBC 保存元数据，包含双数据源 SQL、用户表鉴权、上传和 Swagger UI。

在仓库根目录执行：

```bash title="启动 JDBC 示例"
mvn -f example/dataway-spring-example/pom.xml clean package
java -jar example/dataway-spring-example/target/dataway-spring-example.jar
```

IDE 中导入示例 POM。访问 `http://127.0.0.1:8080/`，使用 `admin` / `example-password` 登录。

:::

### Nacos 示例 {#nacos-example}

[Spring Boot + Nacos 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-nacos-example)使用 Nacos 保存元数据，H2 提供用户表和双数据源 SQL。

```bash title="启动 Nacos 示例"
mvn -f example/dataway-spring-nacos-example/pom.xml spring-boot:test-run
```

测试入口在独立 Java 进程中启动真实 Nacos，自动选择空闲端口，关闭应用时一并停止，无需 Docker。打开 `http://127.0.0.1:8080/`，使用 `admin` / `example-password` 登录。

```bash title="运行真实 HTTP 测试"
mvn -f example/dataway-spring-nacos-example/pom.xml test
```

测试覆盖登录、接口发布与调用、SQL、表单上传、Swagger 文档和 Nacos 并发写入校验。

IDEA 中执行 `test-compile` 后运行测试目录中的 `TestExampleApplication`。普通 `ExampleApplication` 和应用 JAR 连接已有 Nacos，服务端不会打入应用 JAR。

发布和调用步骤见[快速开始](../intro/quickstart.md)。

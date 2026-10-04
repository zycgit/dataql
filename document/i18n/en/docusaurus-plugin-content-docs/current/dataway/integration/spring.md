---
title: "3.1 Spring integration"
hide_table_of_contents: false
description: "Integrate Dataway with Spring: configuration, SQL data sources and transactions."
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## About Spring

Spring provides injection, MVC and transactions. Spring Boot handles startup and auto-configuration. This guide uses Boot 4.1.

Project website：[website](https://spring.io/)

## Features

- Create Dataway through auto-configuration and register its MVC endpoints.
- Reuse MVC interceptors, login identities and exception handling.
- Resolve metadata stores and single or named SQL data sources from Spring.

## Configuration {#configure-and-assemble}

### Dependencies

Add the framework integration, JDBC metadata and SQL extension modules, plus a connection pool and JDBC driver.

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

### Enable endpoints

Add the following settings to the host configuration. All three switches default to `false`.

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

### Register services

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

- `IdentityProvider`: The `identityProvider()` method above creates a `RequestIdentityProvider` and registers it as a bean.
- `ConnectionProvider`: Registered as a bean by `DatawayConfiguration.connectionProvider()` in [SQL data sources](#sql-data-sources).

### Metadata storage

Spring auto-configuration resolves the `ApiDataAccessLayer` bean by type. Create the tables using the [database provider](../metadata/providers/jdbc.md) instructions, then inject the metadata DataSource into this bean. It uses independent transactions by default:

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

For host transactions, add `spring-jdbc`, configure a Spring transaction manager matching `source`, and replace the `metadata` method above:

```java title="Join host transactions"
import net.hasor.dataway.spring.SpringJdbcExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Bean
public ApiDataAccessLayer metadata(DataSource source, PlatformTransactionManager transactionManager) {
    return new JdbcDataAccessLayer(new SpringJdbcExecutor(source, transactionManager));
}
```

Use Spring’s `@Transactional` or `TransactionTemplate` to include metadata operations and application changes in one transaction. See [Transaction integration](../metadata/transactions.md) for guidance.

### Access authorization

For each request, the application validates the JWT and stores the `UserIdentity` in the `host.identity` request attribute for `RequestIdentityProvider` to read.

```java title="Register the interceptor"
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Configuration(proxyBeanMethods = false)
public class WebConfiguration {
    @Bean
    public MappedInterceptor loginInterceptor() {
        // Applies to both default and custom HandlerMappings.
        return new MappedInterceptor(null, new LoginInterceptor());
    }
}
```

See the example's [LoginInterceptor.java](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/config/auth/LoginInterceptor.java) for JWT validation and identity assignment.

```java title="Request interception"
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

        // Read the identity stored by the application's JWT authentication logic.
        Object identity = request.getAttribute(IDENTITY_ATTRIBUTE);
        if (!(identity instanceof UserIdentity user) || !user.authenticated()) {
            response.setStatus(401);
            return false;
        }

        return true;
    }
}
```

## SQL data sources

### Data source access

`ConnectionProvider.findConnection(name, hints)` supplies database connections for SQL scripts and SQL fragments.

- `name`: The data source selected by `FRAGMENT_SQL_DATA_SOURCE`; an unspecified name selects the primary source.
- `hints`: Hint settings for this execution, available for custom connection selection.
- Return value: A JDBC connection, or `null` when the provider cannot supply one. The SQL module releases the connection after execution.

Create data sources through Spring Boot or `@Bean`. Mark the primary source with `@Primary` and name other sources with `@Bean("ds1")` and `@Bean("ds2")`.

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

Use `hint FRAGMENT_SQL_DATA_SOURCE = "ds1"` in a script to select ds1.

### Using transactions {#sql-transactions}

`TransactionUdfSource` provides script transaction functions. The registered `SpringTransactionProvider` delegates these calls to Spring transaction management.

- `required`: Joins an existing Spring transaction for the current data source, or creates one.
- `requiresNew`: Suspends an existing transaction and creates an independent one.
- `nested`: Uses a savepoint within an existing transaction, or creates a transaction. The JDBC driver must support savepoints.

This example transfers 5 from user 1 to user 2 in ds1's `example_people` table. Request parameters are `{"fromId":1,"toId":2,"amount":5}`.

```javascript title="Transfer within a script transaction"
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

Both updates commit on success or roll back if either throws. Transactions apply per source; cross-source operations do not commit atomically.

### Transaction integration

Use `SpringTransactionProvider` to integrate Spring transactions.

Include `org.springframework:spring-jdbc` and configure a transaction manager for each business data source. Reuse existing managers. The example configures its three sources as follows:

```java title="DatabaseConfiguration.java: transaction managers"
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

In the earlier `DatawayConfiguration`, register the provider and pass it to Dataway through `attachment`:

```java title="DatawayConfiguration.java: connect Spring transactions"
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
                // SQL connections and script transaction functions use the provider registered below.
                .attachment(ConnectionProvider.class, connections);
    }

    @Bean
    public ConnectionProvider connectionProvider(ApplicationContext context) {
        // Resolve data sources and their JDBC transaction managers from Spring.
        return new SpringTransactionProvider(context);
    }
}
```

The transaction manager and provider must use the same DataSource instance.

Spring transaction management now covers SQL, script transaction functions and application database operations.

The example publishes `/api/transfer`. Send `{"fromId":1,"toId":2,"amount":5}` to transfer balances. An unknown `toId` rolls back the transfer.

## Configuration reference

### Service assembly

`DatawayAutoConfiguration` reads entry settings from Spring Environment and creates Dataway using container-managed configuration and services.

| Type | Configuration and purpose |
| --- | --- |
| `DatawayAutoConfiguration` | Loaded by Spring Boot; registers MVC entries in Servlet web applications |
| `DatawayConfig` | Uses the container bean, or default configuration when none is supplied |
| `ApiDataAccessLayer` | Uses the explicit `dataAccessLayer(...)` instance first, otherwise resolves a Spring bean by type; use `@Primary` or explicit configuration for multiple implementations |
| `Dataway` | Created by the adapter when no bean is supplied; inject it to obtain four handlers and `AdminService` |
| `ConnectionProvider` | SQL connection provider interface; register an implementation as a Spring bean, inject it into the `DatawayConfig` configuration method, and supply it to the SQL module through `attachment(ConnectionProvider.class, provider)` |
| `SpringTransactionProvider` | Spring transaction implementation; create it with `new SpringTransactionProvider(context)` and register it as a `ConnectionProvider` bean. Ordinary SQL uses Spring transaction connections, while `tran.*` delegates to the Spring JDBC transaction manager matching the data source |
| `SpringJdbcExecutor` | Optional; pass it to `JdbcDataAccessLayer` to join Spring transactions for metadata operations; see [metadata storage](#metadata-storage) |

### Entry settings

These are all the `dataway.*` properties read by the integration module.

| Property | Type | Default | Purpose |
| --- | --- | --- | --- |
| `dataway.api-enabled` | Boolean | `false` | Register the published API entry |
| `dataway.api-prefix` | String | `/api` | Business API route prefix, used when `api-enabled` is true |
| `dataway.admin-enabled` | Boolean | `false` | Register management APIs, console pages and assets together |
| `dataway.admin-prefix` | String | `/admin/api` | Management API route prefix, used when `admin-enabled` is true |
| `dataway.admin-ui` | String | `/admin` | Console page and asset prefix, used when `admin-enabled` is true |
| `dataway.docs-enabled` | Boolean | `false` | Register Swagger and OpenAPI specification endpoints |
| `dataway.docs-prefix` | String | `/docs` | Specification route prefix, used when `docs-enabled` is true |

The three switches are independent and default to `false`. Prefixes are paths relative to the host context path, starting with `/` and without a trailing `/`. Complete defaults:

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

### Core settings

Common integration points are listed below. See [10.1 DatawayConfig](../configuration/core.md) for all methods, defaults and constraints.

| Method | Purpose |
| --- | --- |
| `dataAccessLayer(layer)` | Supply metadata storage; when unset, obtain the `ApiDataAccessLayer` Bean from the container |
| `identityProvider(provider)` | Register the provider that resolves the current request's user identity |
| `attachment(ConnectionProvider.class, provider)` | Register the SQL connection and transaction provider |

### File uploads

Spring reuses `MultipartHttpServletRequest` when available, otherwise Servlet multipart; enable multipart handling in the host. Configure upload cache storage with [DatawayConfig](../configuration/core.md#upload); the host sets request size limits.

### Console settings

The adapter generates `initializer.js` from `dataway.admin-ui`, `dataway.admin-prefix` and `dataway.api-prefix`. After changing these settings and restarting the application, the console uses the new URLs automatically. No invocation URL is supplied when the business entry is disabled.

The initializer calls `DatawayUI(...)` with these options:

| Option | Value in the bundled initializer | Purpose |
| --- | --- | --- |
| `adminApi` | `api/` | Browser-facing management API base, required |
| `api` | `../api/` | Published API base; omitting it disables list-page invocations |

URLs are relative to the console, preserving the context path and a common proxy prefix. Provide a custom initializer if the proxy rewrites each entry independently. API document servers remain configured through `DatawayConfig.documentServer(...)`. See [console deployment](../configuration/console.md).

## Example project {#example}

### JDBC example {#jdbc-example}

:::info[Example]

The [Spring Boot + JDBC example](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example) stores metadata through JDBC and includes SQL across two data sources, user-table authentication, uploads and Swagger UI.

Run from the repository root:

```bash title="Start the JDBC example"
mvn -f example/dataway-spring-example/pom.xml clean package
java -jar example/dataway-spring-example/target/dataway-spring-example.jar
```

Import the example POM. Open `http://127.0.0.1:8080/` and sign in with `admin` / `example-password`.

:::

### Nacos example {#nacos-example}

The [Spring Boot + Nacos example](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-nacos-example) stores metadata in Nacos. H2 provides the user table and two SQL data sources.

```bash title="Start the Nacos example"
mvn -f example/dataway-spring-nacos-example/pom.xml spring-boot:test-run
```

The test entry point starts real Nacos in a separate Java process, selects free ports and stops it when the application closes. Docker is not required. Open `http://127.0.0.1:8080/` and sign in with `admin` / `example-password`.

```bash title="Run real HTTP tests"
mvn -f example/dataway-spring-nacos-example/pom.xml test
```

Tests cover login, API publication and calls, SQL, forms, uploads, Swagger documents and stale-write rejection in Nacos.

In IDEA, run `test-compile`, then launch `TestExampleApplication` from the test sources. The regular `ExampleApplication` and application JAR connect to an existing Nacos server; server libraries are excluded from the application JAR.

Follow [Quick start](../intro/quickstart.md) to publish and call an API.

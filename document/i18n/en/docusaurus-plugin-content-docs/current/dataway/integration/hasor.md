---
title: "3.3 Hasor integration"
hide_table_of_contents: false
description: "Integrate Dataway with Hasor: configuration, SQL data sources and transactions."
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## About Hasor

Hasor provides injection, MVC and modules. This guide uses Hasor Boot 5.3 and DatawayModule.

Project website：[website](https://www.hasor.net/)

## Features

- Install Dataway as a module using annotations and hconfig.xml.
- Reuse MVC interceptors, identity resolution and exception handlers.
- Supply SQL data sources through AppContext and optionally participate in dbVisitor transactions.

## Configuration {#configure-and-assemble}

### Dependencies

Add the framework integration, JDBC metadata and SQL extension modules, plus a connection pool and JDBC driver.

<Tabs groupId="build-tool">
<TabItem value="maven" label="Maven" default>

```xml
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-hasor</artifactId>
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
implementation 'net.hasor:dataway-hasor:@project.docsVersion@'
implementation 'net.hasor:dataway-meta-jdbc:@project.docsVersion@'
implementation 'net.hasor:dataql-sqlproc:@project.docsVersion@'
```

</TabItem>
</Tabs>

### Enable endpoints

Add the following settings to the host configuration. All three switches default to `false`.

```xml title="hconfig.xml"
<config xmlns="https://www.hasor.net/sechma/main">
    <hasor.loadPackages>com.example</hasor.loadPackages>
    <dataway>
        <api-enabled>true</api-enabled>
        <admin-enabled>true</admin-enabled>
        <docs-enabled>true</docs-enabled>
    </dataway>
</config>
```

### Register services

Place the configuration class in the scanned package, declare the core configuration and install the Dataway module:

```java title="DatawayConfiguration.java"
import net.hasor.config.Bean;
import net.hasor.config.Configuration;
import net.hasor.core.ApiBinder;
import net.hasor.core.Module;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.hasor.DatawayModule;
import net.hasor.dataway.service.DatawayConfig;

@Configuration
public class DatawayConfiguration implements Module {
    @Override
    public void loadModule(ApiBinder binder) throws Throwable {
        binder.installModule(new DatawayModule());
    }

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

`DatawayModule` resolves the unnamed `ApiDataAccessLayer` binding from the container. Create the tables using the [database provider](../metadata/providers/jdbc.md) instructions, then inject the metadata DataSource into this bean. It uses independent transactions by default:

```java title="MetadataConfiguration.java"
import javax.sql.DataSource;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.config.Bean;
import net.hasor.config.Configuration;

@Configuration
public class MetadataConfiguration {
    @Bean
    public ApiDataAccessLayer metadata(DataSource source) {
        return new JdbcDataAccessLayer(source);
    }
}
```

For host transactions, use `HasorJdbcExecutor` with the dbVisitor transaction manager associated with `source`. Replace the `metadata` method above:

```java title="Join host transactions"
import net.hasor.dataway.hasor.HasorJdbcExecutor;

@Bean
public ApiDataAccessLayer metadata(DataSource source) {
    return new JdbcDataAccessLayer(new HasorJdbcExecutor(source));
}
```

Use dbVisitor’s `TransactionTemplate` to include metadata operations and application changes in one transaction. See [Transaction integration](../metadata/transactions.md) for guidance.

### Access authorization

For each request, the application validates the JWT and stores the `UserIdentity` in the `host.identity` request attribute for `RequestIdentityProvider` to read.

```java title="Register the interceptor"
import net.hasor.config.Configuration;
import net.hasor.config.web.WebMvcConfigurer;
import net.hasor.config.web.render.JsonRenderConfigurer;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.web.Invoker;
import net.hasor.web.WebApiBinder;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(WebApiBinder binder) {
        binder.bindInterceptor(new LoginInterceptor());
    }

    @Override
    public void configureJson(JsonRenderConfigurer configurer) {
        // Reuse DataQL's JSON utility for Hasor MVC rendering.
        configurer.renderEngine((invoker, writer) -> {
            writer.write(JsonUtils.writeValueAsString(invoker.get(Invoker.RETURN_DATA_KEY)));
        });
    }
}
```

See the example's [LoginInterceptor.java](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-hasor-example/src/main/java/net/hasor/dataway/hasor/example/config/auth/LoginInterceptor.java) for JWT validation and identity assignment.

```java title="Request interception"
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.web.HandlerInterceptor;
import net.hasor.web.Invoker;

public class LoginInterceptor implements HandlerInterceptor {
    public static final String IDENTITY_ATTRIBUTE = "host.identity";

    @Override
    public boolean preHandle(Invoker invoker) {
        var request = invoker.getHttpRequest();
        var response = invoker.getHttpResponse();
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

Create data sources with `@Bean`: an unnamed Bean for the primary source, and `@Bean("ds1")` and `@Bean("ds2")` for other sources.

```java title="DatawayConfiguration.java"
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.hasor.HasorTransactionProvider;
import net.hasor.core.AppContext;
import net.hasor.config.Bean;

@Bean
public ConnectionProvider connectionProvider(AppContext context) {
    return new HasorTransactionProvider(context);
}
```

Use `hint FRAGMENT_SQL_DATA_SOURCE = "ds1"` in a script to select ds1.

### Using transactions {#sql-transactions}

`TransactionUdfSource` provides script transaction functions. The registered `HasorTransactionProvider` delegates these calls to dbVisitor transaction management.

- `required`: Joins an existing dbVisitor transaction for the current data source, or creates one.
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

Include `net.hasor:dbvisitor-hasor`. The example's `DatabaseConfiguration` implements `Module` and registers dbVisitor services for each source:

```java title="DatabaseConfiguration.java: database and transaction services"
import net.hasor.core.ApiBinder;
import net.hasor.core.Module;
import net.hasor.dbvisitor.hasor.session.SessionConfigurer;

@Override
public void loadModule(ApiBinder binder) throws Throwable {
    binder.installModule(new SessionConfigurer());
    binder.installModule(new SessionConfigurer("ds1"));
    binder.installModule(new SessionConfigurer("ds2"));
}
```

`SessionConfigurer` reuses the named `DataSource` to supply `JdbcTemplate`, `TransactionTemplate` and related services. `HasorTransactionProvider` resolves these services by data source name.

dbVisitor transaction management now covers SQL, script transaction functions and application database operations.

The example publishes `/api/transfer`. Send `{"fromId":1,"toId":2,"amount":5}` to transfer balances. An unknown `toId` rolls back the transfer.

## Configuration reference

### Service assembly

`DatawayModule` reads entry settings from Hasor Settings, including the `dataway` node in `hconfig.xml`.

| Type | Configuration and purpose |
| --- | --- |
| `DatawayModule` | Install through `binder.installModule(...)`; creates the shared core and registers MVC entries |
| `DatawayConfig` | The no-argument module resolves configuration from the container; `new DatawayModule(config)` supplies it explicitly |
| `ApiDataAccessLayer` | Uses the explicit `dataAccessLayer(...)` instance first, otherwise resolves the container's unnamed `ApiDataAccessLayer` binding |
| `Dataway` | Created and registered as a singleton by the module; pass an existing instance to `new DatawayModule(dataway)` to reuse it; provides four handlers and `AdminService` |
| `ConnectionProvider` | Register with `attachment(ConnectionProvider.class, provider)` to supply JDBC connections for SQL scripts and fragments |
| `HasorTransactionProvider` | A transaction-aware `ConnectionProvider`, created with `new HasorTransactionProvider(context)`; resolves `DataSource` and `TransactionTemplate` by the same name to integrate ordinary SQL and `tran.*` with dbVisitor transactions |
| `HasorJdbcExecutor` | Optional; pass it to `JdbcDataAccessLayer` to join dbVisitor transactions for metadata operations; see [metadata storage](#metadata-storage) |

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

```xml title="hconfig.xml"
<config xmlns="https://www.hasor.net/sechma/main">
    <dataway>
        <api-enabled>false</api-enabled>
        <api-prefix>/api</api-prefix>
        <admin-enabled>false</admin-enabled>
        <admin-prefix>/admin/api</admin-prefix>
        <admin-ui>/admin</admin-ui>
        <docs-enabled>false</docs-enabled>
        <docs-prefix>/docs</docs-prefix>
    </dataway>
</config>
```

### Core settings

Common integration points are listed below. See [10.1 DatawayConfig](../configuration/core.md) for all methods, defaults and constraints.

| Method | Purpose |
| --- | --- |
| `dataAccessLayer(layer)` | Supply metadata storage; when unset, obtain the `ApiDataAccessLayer` Bean from the container |
| `identityProvider(provider)` | Register the provider that resolves the current request's user identity |
| `attachment(ConnectionProvider.class, provider)` | Register the SQL connection and transaction provider |

### File uploads

Hasor uses `hasor-web` FileUpload and the host upload limits. Configure upload cache storage with [DatawayConfig](../configuration/core.md#upload); the host sets request size limits.

### Console settings

The adapter generates `initializer.js` from `dataway.admin-ui`, `dataway.admin-prefix` and `dataway.api-prefix`. After changing these settings and restarting the application, the console uses the new URLs automatically. No invocation URL is supplied when the business entry is disabled.

The initializer calls `DatawayUI(...)` with these options:

| Option | Value in the bundled initializer | Purpose |
| --- | --- | --- |
| `adminApi` | `api/` | Browser-facing management API base, required |
| `api` | `../api/` | Published API base; omitting it disables list-page invocations |

URLs are relative to the console, preserving the context path and a common proxy prefix. Provide a custom initializer if the proxy rewrites each entry independently. API document servers remain configured through `DatawayConfig.documentServer(...)`. See [console deployment](../configuration/console.md).

## Example project {#example}

:::info[Example]

The [Hasor Boot + JDBC example](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-hasor-example) stores metadata through JDBC and includes SQL across two data sources, user-table authentication, uploads and Swagger UI.

Run from the repository root:

```bash title="Start the Hasor example"
mvn -f example/dataway-hasor-example/pom.xml clean package
java -jar example/dataway-hasor-example/target/dataway-hasor-example.jar
```

Import the example POM. Open `http://127.0.0.1:8080/` and sign in with `admin` / `example-password`.

:::

Follow [Quick start](../intro/quickstart.md) to publish and call an API.

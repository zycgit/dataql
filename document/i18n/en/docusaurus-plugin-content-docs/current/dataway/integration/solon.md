---
title: "3.2 Solon integration"
hide_table_of_contents: false
description: "Integrate Dataway with Solon: configuration, SQL data sources and transactions."
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## About Solon

Solon provides injection, Web, transactions and plugins. This guide uses Solon 4.1 and DatawayPlugin.

Project website：[website](https://solon.noear.org/)

## Features

- Install Dataway as a plugin and reuse the host web server.
- Resolve configuration, metadata stores and SQL data sources through AppContext.
- Reuse RouterInterceptor, login identities and Solon transactions.

## Configuration {#configure-and-assemble}

### Dependencies

Add the framework integration, JDBC metadata and SQL extension modules, plus a connection pool and JDBC driver.

<Tabs groupId="build-tool">
<TabItem value="maven" label="Maven" default>

```xml
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-solon</artifactId>
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
implementation 'net.hasor:dataway-solon:@project.docsVersion@'
implementation 'net.hasor:dataway-meta-jdbc:@project.docsVersion@'
implementation 'net.hasor:dataql-sqlproc:@project.docsVersion@'
```

</TabItem>
</Tabs>

### Enable endpoints

Add the following settings to the host configuration. All three switches default to `false`.

<Tabs groupId="solon-config">
<TabItem value="properties" label="app.properties" default>

```properties title="app.properties"
dataway.api-enabled=true
dataway.admin-enabled=true
dataway.docs-enabled=true
```

</TabItem>
<TabItem value="yaml" label="app.yml">

```yaml
dataway:
  api-enabled: true
  admin-enabled: true
  docs-enabled: true
```

</TabItem>
</Tabs>

### Register services

Declare the core configuration and install the Dataway plugin:

```java title="DatawayConfiguration.java"
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.solon.DatawayPlugin;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;

@Configuration
public class DatawayConfiguration {
    @Inject
    private AppContext context;
    @Inject
    private DatawayConfig config;

    @Init
    public void initialize() {
        new DatawayPlugin(this.config).start(this.context);
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

`DatawayPlugin` requires exactly one `ApiDataAccessLayer` bean in the container. Create the tables using the [database provider](../metadata/providers/jdbc.md) instructions, then inject the metadata DataSource into this bean. It uses independent transactions by default:

```java title="MetadataConfiguration.java"
import javax.sql.DataSource;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

@Configuration
public class MetadataConfiguration {
    @Bean
    public ApiDataAccessLayer metadata(DataSource source) {
        return new JdbcDataAccessLayer(source);
    }
}
```

For host transactions, enable `solon-data` and use `SolonJdbcExecutor` for Solon transactions and connection proxies. Replace the `metadata` method above:

```java title="Join host transactions"
import net.hasor.dataway.solon.SolonJdbcExecutor;

@Bean
public ApiDataAccessLayer metadata(DataSource source) {
    return new JdbcDataAccessLayer(new SolonJdbcExecutor(source));
}
```

Use Solon’s transaction management to include metadata operations and application changes in one transaction. See [Transaction integration](../metadata/transactions.md) for guidance.

### Access authorization

For each request, the application validates the JWT and stores the `UserIdentity` in the `host.identity` request attribute for `RequestIdentityProvider` to read.

```java title="Register the interceptor"
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;

@Configuration
public class WebConfiguration {
    @Inject
    private AppContext context;

    @Init
    public void initialize() {
        this.context.app().chains().addRouterInterceptor(new LoginInterceptor(), 0);
    }
}
```

See the example's [LoginInterceptor.java](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-solon-example/src/main/java/net/hasor/dataway/solon/example/config/auth/LoginInterceptor.java) for JWT validation and identity assignment.

```java title="Request interception"
import net.hasor.dataway.authorization.UserIdentity;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.solon.core.route.RouterInterceptor;
import org.noear.solon.core.route.RouterInterceptorChain;

public class LoginInterceptor implements RouterInterceptor {
    public static final String IDENTITY_ATTRIBUTE = "host.identity";

    @Override
    public void doIntercept(Context context, Handler handler, RouterInterceptorChain chain) throws Throwable {
        if ("/session/login".equals(context.path())) {
            chain.doIntercept(context, handler);
            return;
        }

        // Read the identity stored by the application's JWT authentication logic.
        Object identity = context.attr(IDENTITY_ATTRIBUTE);
        if (!(identity instanceof UserIdentity user) || !user.authenticated()) {
            context.status(401);
            context.setHandled(true);
            return;
        }

        chain.doIntercept(context, handler);
    }
}
```

## SQL data sources

### Data source access

`ConnectionProvider.findConnection(name, hints)` supplies database connections for SQL scripts and SQL fragments.

- `name`: The data source selected by `FRAGMENT_SQL_DATA_SOURCE`; an unspecified name selects the primary source.
- `hints`: Hint settings for this execution, available for custom connection selection.
- Return value: A JDBC connection, or `null` when the provider cannot supply one. The SQL module releases the connection after execution.

Create data sources with `@Bean`. Register the primary source by `DataSource` type and name other sources with annotations such as `@Bean(name = "ds1", typed = false)`.

```java title="DatawayConfiguration.java"
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.solon.SolonTransactionProvider;
import org.noear.solon.core.AppContext;
import org.noear.solon.annotation.Bean;

@Bean
public ConnectionProvider connectionProvider(AppContext context) {
    return new SolonTransactionProvider(context);
}
```

Use `hint FRAGMENT_SQL_DATA_SOURCE = "ds1"` in a script to select ds1.

### Using transactions {#sql-transactions}

`TransactionUdfSource` provides script transaction functions. The registered `SolonTransactionProvider` delegates these calls to Solon transaction management.

- `required`: Joins an existing Solon transaction for the current data source, or creates one.
- `requiresNew`: Suspends an existing transaction and creates an independent one.
- `nested`: Uses Solon's nested transaction policy, creating a transaction when none exists.

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

Both updates commit on success or roll back if either throws. Atomic commit across data sources is not guaranteed.

### Transaction integration

Include `org.noear:solon-data`. `SolonTransactionProvider` uses Solon's connection and transaction management; no additional transaction manager bean is needed. Existing application `@Transaction` and `TranUtils.execute(...)` configuration remains applicable.

Solon transaction management now covers SQL, script transaction functions and application database operations.

The example publishes `/api/transfer`. Send `{"fromId":1,"toId":2,"amount":5}` to transfer balances. An unknown `toId` rolls back the transfer.

## Configuration reference

### Service assembly

`DatawayPlugin` reads entry settings from Solon Props. Pass the application's `DatawayConfig` when installing the plugin.

| Type | Configuration and purpose |
| --- | --- |
| `DatawayPlugin` | Install `new DatawayPlugin(config)`; creates Dataway and registers entries after container service initialization |
| `DatawayConfig` | Pass the configuration bean to the plugin; the no-argument `DatawayPlugin()` creates default configuration |
| `ApiDataAccessLayer` | Uses the explicit `dataAccessLayer(...)` instance first; otherwise requires exactly one implementation in the container |
| `Dataway` | Created and registered by the plugin; pass an existing instance to `new DatawayPlugin(dataway)` to reuse it; provides four handlers and `AdminService` |
| `ConnectionProvider` / `SolonTransactionProvider` | Register with `attachment(ConnectionProvider.class, provider)`; enable `solon-data` to use Solon transactions for ordinary SQL and `tran.*` |
| `SolonJdbcExecutor` | Optional; pass it to `JdbcDataAccessLayer` to join Solon transactions for metadata operations; see [metadata storage](#metadata-storage) |

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

<Tabs groupId="solon-config">
<TabItem value="yaml" label="app.yml" default>

```yaml title="app.yml"
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
<TabItem value="properties" label="app.properties">

```properties title="app.properties"
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

Solon reuses `Context.paramMap` and `Context.fileMap` for form and upload data. Configure upload cache storage with [DatawayConfig](../configuration/core.md#upload); the host sets request size limits.

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

The [Solon + JDBC example](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-solon-example) stores metadata through JDBC and includes SQL across two data sources, user-table authentication, uploads and Swagger UI.

Run from the repository root:

```bash title="Start the Solon example"
mvn -f example/dataway-solon-example/pom.xml clean package
java -jar example/dataway-solon-example/target/dataway-solon-example.jar
```

Import the example POM. Open `http://127.0.0.1:8080/` and sign in with `admin` / `example-password`.

:::

Follow [Quick start](../intro/quickstart.md) to publish and call an API.

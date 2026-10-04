---
title: "3.2 Solon 整合"
hide_table_of_contents: false
description: "Solon 接入 Dataway：依赖、配置、SQL 数据源与事务。"
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Solon 概述

Solon 提供依赖注入、Web、事务和插件扩展。本节使用 Solon 4.1，通过 DatawayPlugin 接入。

项目地址：[官方网站](https://solon.noear.org/)

## 特性

- 通过插件安装 Dataway，沿用宿主 Web 服务器。
- 通过 AppContext 获取配置、元数据访问层和 SQL 数据源。
- 复用 RouterInterceptor、登录身份及 Solon 事务。

## 配置方法 {#配置与装配}

### 引入依赖

引入框架整合、JDBC 元数据和 SQL 扩展，另行配置连接池与 JDBC 驱动。

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

### 开放入口

在宿主配置文件中开放入口，三个开关默认均为 `false`。

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

### 注册服务

声明核心配置，并安装 Dataway 插件：

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

- `IdentityProvider`：由上面的 `identityProvider()` 方法创建 `RequestIdentityProvider` 并注册为 Bean。
- `ConnectionProvider`：由 [SQL 数据源](#sql-数据源) 中的 `DatawayConfiguration.connectionProvider()` 方法注册为 Bean。

### 元数据存储

`DatawayPlugin` 从容器获取唯一的 `ApiDataAccessLayer` Bean。先按[数据库提供者](../metadata/providers/jdbc.md)创建表，将元数据 DataSource 注入下面的 Bean，默认使用独立事务：

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

接入宿主事务时，启用 `solon-data`，通过 `SolonJdbcExecutor` 使用 Solon 事务和连接代理，并替换上面的 `metadata` 方法：

```java title="接入宿主事务"
import net.hasor.dataway.solon.SolonJdbcExecutor;

@Bean
public ApiDataAccessLayer metadata(DataSource source) {
    return new JdbcDataAccessLayer(new SolonJdbcExecutor(source));
}
```

借助 Solon 的事务管理能力，将元数据操作与应用业务纳入同一事务。选择依据见[事务整合](../metadata/transactions.md)。

### 访问鉴权

应用在每次请求中校验 JWT，将 `UserIdentity` 写入请求属性 `host.identity`，供 `RequestIdentityProvider` 读取。

```java title="注册拦截器"
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

JWT 校验和身份写入见示例工程的 [LoginInterceptor.java](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-solon-example/src/main/java/net/hasor/dataway/solon/example/config/auth/LoginInterceptor.java)。

```java title="请求拦截"
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

        // 应用已完成 JWT 校验和身份写入，此处读取当前请求的身份。
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

## SQL 数据源

### 数据源接入

`ConnectionProvider.findConnection(name, hints)` 为 SQL 脚本和 SQL 片段提供数据库连接。

- `name`：由 `FRAGMENT_SQL_DATA_SOURCE` 指定的数据源名称，未指定时使用主库。
- `hints`：本次执行的 Hint 配置，可用于自定义连接选择。
- 返回值：可用的 JDBC 连接；无法提供时返回 `null`。连接由 SQL 模块在执行后释放。

通过 `@Bean` 创建数据源。主库按 `DataSource` 类型注册，其他库使用 `@Bean(name = "ds1", typed = false)` 等名称注册。

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

脚本使用 `hint FRAGMENT_SQL_DATA_SOURCE = "ds1"` 选择 ds1。

### 使用事务 {#sql-事务}

`TransactionUdfSource` 提供脚本事务函数，前面注册的 `SolonTransactionProvider` 将这些调用交给 Solon 的事务管理。

- `required`：加入当前数据源的已有 Solon 事务，没有时创建事务。
- `requiresNew`：挂起已有事务，创建独立事务。
- `nested`：使用 Solon 的嵌套事务策略，没有事务时创建事务。

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

两次更新全部成功后提交，任意一次抛出异常则一起回滚。跨数据源操作不保证原子提交。

### 事务整合

应用引入 `org.noear:solon-data`。前面的 `SolonTransactionProvider` 已同时接入 Solon 的连接与事务管理，无需另建事务管理器 Bean。应用原有的 `@Transaction`、`TranUtils.execute(...)` 配置可以继续使用。

配置完成后，借助 Solon 的事务能力统一管理 SQL、脚本事务函数和应用数据库操作。

示例已发布 `/api/transfer`，请求 `{"fromId":1,"toId":2,"amount":5}` 可完成转账；将 `toId` 改为不存在的账号，已扣除的余额也会回滚。

## 配置项说明

### 服务装配

`DatawayPlugin` 通过 Solon Props 读取入口配置，应用安装插件时传入 `DatawayConfig`。

| 类型 | 配置方式与作用 |
| --- | --- |
| `DatawayPlugin` | 安装 `new DatawayPlugin(config)`，在容器完成服务初始化后创建 Dataway 并注册入口 |
| `DatawayConfig` | 将应用的配置 Bean 传入插件；无参 `DatawayPlugin()` 使用新建的默认配置 |
| `ApiDataAccessLayer` | 优先使用 `dataAccessLayer(...)` 显式设置的实例，否则要求容器中恰好有一个实现 |
| `Dataway` | 由插件创建并注册到容器；也可传入 `new DatawayPlugin(dataway)` 复用已创建实例，从中获取四个 Handler 和 `AdminService` |
| `ConnectionProvider` / `SolonTransactionProvider` | 通过 `attachment(ConnectionProvider.class, provider)` 接入；启用 `solon-data` 后，普通 SQL 和 `tran.*` 使用 Solon 事务 |
| `SolonJdbcExecutor` | 可选，传给 `JdbcDataAccessLayer` 使元数据操作加入 Solon 事务，见[元数据存储](#元数据存储) |

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

### 核心配置

常用接入项如下，完整方法、默认值和约束见 [10.1 DatawayConfig](../configuration/core.md)。

| 方法 | 作用 |
| --- | --- |
| `dataAccessLayer(layer)` | 指定元数据访问层；未设置时从容器获取 `ApiDataAccessLayer` Bean |
| `identityProvider(provider)` | 注册身份提供者，获取当前请求的用户身份 |
| `attachment(ConnectionProvider.class, provider)` | 注册 SQL 的连接与事务提供者 |

### 文件上传

Solon 复用 `Context.paramMap`、`Context.fileMap` 获取表单和上传文件。 上传缓存通过 [DatawayConfig](../configuration/core.md#upload) 设置，请求大小上限由宿主配置。

### 控制台配置

整合模块根据 `dataway.admin-ui`、`dataway.admin-prefix` 和 `dataway.api-prefix` 自动生成 `initializer.js`。修改后端配置并重启后，控制台自动使用对应地址，无需单独修改前端。业务入口关闭时不提供调用地址。

初始化脚本调用 `DatawayUI(...)`，参数如下：

| 参数 | 默认初始化脚本中的值 | 作用 |
| --- | --- | --- |
| `adminApi` | `api/` | 浏览器访问管理 API 的基础地址，必填 |
| `api` | `../api/` | 浏览器调用已发布 API 的基础地址，省略后不能在列表页调用 |

地址相对控制台解析，保留 context path 和代理添加的公共前缀。代理分别改写各入口地址时，可提供自定义初始化脚本。API 文档的服务地址仍通过 `DatawayConfig.documentServer(...)` 设置。见[控制台部署](../configuration/console.md)。

## 样例工程 {#example}

:::info[Example]

[Solon + JDBC 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-solon-example)使用 JDBC 保存元数据，包含双数据源 SQL、用户表鉴权、上传和 Swagger UI。

在仓库根目录执行：

```bash title="启动 Solon 示例"
mvn -f example/dataway-solon-example/pom.xml clean package
java -jar example/dataway-solon-example/target/dataway-solon-example.jar
```

IDE 中导入示例 POM。访问 `http://127.0.0.1:8080/`，使用 `admin` / `example-password` 登录。

:::

发布和调用步骤见[快速开始](../intro/quickstart.md)。

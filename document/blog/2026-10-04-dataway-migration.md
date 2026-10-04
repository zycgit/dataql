---
slug: dataway-migration
title: "新版 Dataway：架构、接口与数据迁移指南"
description: "对照 old-hasor 的 Dataway 实现，说明新版的模块、配置、核心接口、SPI、脚本和元数据变化，并提供迁移步骤与离线辅助工具。"
authors: [zyc]
tags: [DataQL, Dataway]
topics: [dataway]
language: zh-cn
---

新版 Dataway 保留了“编写脚本、调试、保存、发布、调用”的使用方式，将运行核心、框架整合、元数据存储和控制台资源拆成独立模块。迁移时，接口的原始脚本和发布历史可以继续使用，应用的装配代码、身份接入和扩展接口需要按新接口调整。

本文对照 `old-hasor/hasor-dataql` 中的 `dataql-dataway`、`dataql-fx` 与当前 `@project.docsVersion@` 源码，说明变化及处理步骤。文中的数据库操作应先在备份副本上执行；目标版本仍为开发版本，迁移前应固定实际使用的依赖版本。

<!-- truncate -->

## 1. 迁移范围

先清点应用是否使用了以下内容：

- 控制台创建的 API、请求参数样例、响应模板、发布历史。
- `hasor.dataway.*` 配置、数据源及元数据提供者。
- 登录 SPI、权限 SPI、执行前后拦截、结果序列化扩展。
- Java 程序中的 `DatawayService` 调用、自定义 UDF、SQL 片段和 Hint。
- 自定义控制台、反向代理、Swagger 地址及直接调用管理接口的程序。

只使用控制台和 SQL 脚本的应用，主要调整配置、身份和存储；实现了 SPI 或直接引用内部类型的应用，还需迁移 Java 扩展。旧 JAR 与新 JAR 中存在同名包和接口，部署时应替换依赖，不能把两套运行时混装在一个应用中。

### 主要变化

| 方面 | old-hasor Dataway | 新版 Dataway |
| --- | --- | --- |
| 运行环境 | 依赖 Hasor 容器、Web 过滤器和 SPI 调度 | 框架无关核心，Spring、Solon、Hasor 分别提供 MVC 适配 |
| 初始化 | Hasor 模块读取配置并创建服务 | `DatawayConfig` 保存配置，`Dataway` 装配服务 |
| HTTP 入口 | 业务 API 过滤器、管理页面与管理接口 | API、管理 API、页面资源、文档四个 Handler |
| 元数据 | 提供者位于 `dataql-dataway` 中 | `dataway-meta-jdbc`、`dataway-meta-nacos` 独立引入 |
| 登录 | 内置登录及登录 SPI | 应用负责登录，`IdentityProvider` 提供请求身份 |
| Java 调用 | `DatawayService` 混合调用和部分管理能力 | `ApiService` 调用发布接口，`AdminService` 管理接口 |
| 结果输出 | Structure 配置、执行结果 SPI、序列化 SPI | `ResultHandler` 和 `ResultInfo` |
| 并发修改 | CRUD 接口没有统一版本参数 | `REVISION` 与批量原子写入，冲突需重新读取 |
| 路由 | 定义按 path 唯一 | 定义按 `(method, path)` 唯一 |

用户操作仍保留草稿、发布版和历史记录。修改草稿后，外部调用继续使用已启用的发布版，重新发布后才使用新内容。

## 2. 替换依赖与启动配置

### 模块选择

| 原有模块或能力 | 新模块 |
| --- | --- |
| `dataql-dataway` | `dataway-embedded`；Web 应用通常直接引入框架整合模块 |
| Spring 中通过 Hasor 接入 | `dataway-spring` |
| Hasor 模块 | `dataway-hasor` |
| Solon 接入 | `dataway-solon` |
| 内置控制台 | `dataway-embedded-web`，资源位于 `META-INF/dataway-ui/` |
| 数据库元数据提供者 | `dataway-meta-jdbc` |
| Nacos 元数据提供者 | `dataway-meta-nacos` |
| `dataql-fx` 中的 SQL 能力 | `dataql-sqlproc` |
| 常规函数与 Web 函数 | 分别由 `dataql-engine`、`dataway-embedded` 提供 |

例如，Spring 应用使用 JDBC 元数据和 SQL 脚本时引入：

```xml title="pom.xml"
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

连接池和数据库驱动由应用提供。新版不再通过 `dalType` 查找存储提供者，应用创建一个 `ApiDataAccessLayer` Bean 即可。框架版本及完整接入方式见 [Spring](/docs/dataway/integration/spring)、[Solon](/docs/dataway/integration/solon)、[Hasor](/docs/dataway/integration/hasor)。

### 配置名称对照

| 原配置 | 新配置或接入方式 |
| --- | --- |
| `hasor.dataway.enable` | `dataway.api-enabled`；新版没有总开关 |
| `hasor.dataway.enableAdmin` | `dataway.admin-enabled`，同时注册管理 API 和页面资源 |
| `hasor.dataway.enableSwaggerApi` | `dataway.docs-enabled`，独立控制文档入口 |
| `hasor.dataway.baseApiUrl` | `dataway.api-prefix`，默认 `/api` |
| `hasor.dataway.baseAdminUrl` | 拆为 `dataway.admin-prefix`（默认 `/admin/api`）和 `dataway.admin-ui`（默认 `/admin`） |
| 文档跟随 API/管理入口 | `dataway.docs-prefix`，默认 `/docs` |
| `dataAccessLayer.dalType`、`provider` | 创建 JDBC、Nacos 或自定义的 `ApiDataAccessLayer` |
| `settings.dal_db_table_prefix` | `JdbcDataAccessLayer` 的可选前缀参数，或 `DatawayConfig.tableMapping(...)` |
| `authorization.*`、内置管理员账号 | 应用登录系统、`IdentityProvider`、`AuthorizationCheck` |
| `globalConfig.resultStructure` | `DatawayConfig.defaultResultHandler("structure")` 或 `"raw"` |
| `globalConfig.responseFormat` | `StructureResultHandler` 构造配置，或单个 API 的 `responseFormat` |
| `globalConfig.wrapAllParameters`、`wrapParameterName` | `DatawayConfig` 同名配置及单个 API 选项 |
| `enableCrossDomain` | 宿主框架或网关的 CORS 配置 |
| `showGitButton` | 删除，控制台已移除 Git 按钮 |
| Nacos 服务发现配置 | 由应用的服务注册与发现组件承担 |

三个入口开关默认均为 `false`。原实现中开启管理会顺带开放 Swagger，新版需要显式开启文档入口。

```yaml title="application.yml"
dataway:
  api-enabled: true
  api-prefix: /api
  admin-enabled: true
  admin-prefix: /admin/api
  admin-ui: /admin
  docs-enabled: true
  docs-prefix: /docs
```

Solon 和 Hasor 使用相同配置键，分别写入宿主配置文件及 `hconfig.xml`。保留对外业务 API 地址时，将 `api-prefix` 设置成原值；数据库里的 API 路径仍是去掉入口前缀后的路径。

### 核心配置

下面展示已经配置数据源和 Spring 事务管理器的应用如何接线。元数据表需要预先准备，升级已有表的方法见后文。

```java title="DatawayConfiguration.java"
import javax.sql.DataSource;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.spring.SpringTransactionProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DatawayConfiguration {
    @Bean
    public DatawayConfig datawayConfig(IdentityProvider identity, ConnectionProvider connections) {
        return new DatawayConfig()
                .identityProvider(identity)
                .attachment(ConnectionProvider.class, connections);
    }

    @Bean
    public ApiDataAccessLayer metadata(DataSource source) {
        return new JdbcDataAccessLayer(source);
    }

    @Bean
    public IdentityProvider identityProvider() {
        return new RequestIdentityProvider("login.identity");
    }

    @Bean
    public ConnectionProvider connectionProvider(ApplicationContext context) {
        return new SpringTransactionProvider(context);
    }
}
```

`RequestIdentityProvider` 读取请求属性；登录拦截器应在校验 Cookie 或令牌后，将 `UserIdentity` 放入 `login.identity`。普通 SQL 和事务函数通过 `SpringTransactionProvider` 接入 Spring 事务。元数据的 `JdbcDataAccessLayer(source)` 此时仍使用独立事务，两者的配置用途不同。

## 3. Java 管理与调用接口

### 接口替换

| 原接口或类型 | 新入口与差异 |
| --- | --- |
| `DatawayService.invokeApi(path, params)` | `ApiService.invokeByPath(method, path, params)`，方法参与路由匹配 |
| `invokeApiWithoutThrow(...)` | 不再提供该包装方法，调用方按自身异常策略处理 `Exception` |
| 按接口标识调用 | `ApiService.invokeById(apiID, params)`，传 API ID，不传发布记录 ID |
| `DatawayService.getApiById(...)` | `AdminService.getApiById(...)` 返回状态；脚本等定义通过 `getDraftByApi(...)` 获取 |
| `disableApi(apiID)`、`deleteApi(apiID)` | 对应 `AdminService` 方法增加 `version` 参数 |
| `DatawayApi` | 按用途使用 `ApiDefinition`、`ApiState`、`ApiRelease` |
| 管理 Controller 的调用与转换 | 应用代码使用 `AdminService`，无需构造 HTTP 请求 |
| `EntityDef`、`QueryCondition` | `EntityType`、`Map<FieldDef, String>` 查询条件 |

旧版带 method 的 `invokeApi` 重载已经标记弃用，实际仍按 path 调用。迁移到 `invokeByPath` 后必须填写发布 API 的请求方法。

原 `invokeApiWithoutThrow` 会把异常转为运行时异常，并非忽略失败。新版调用时也应区分前置异常、脚本失败响应和正常数据。

### 调用已发布 API

```java title="PublishedApiClient.java"
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.script.ApiService;

public class PublishedApiClient {
    private final ApiService service;

    public PublishedApiClient(Dataway dataway) {
        this.service = dataway.getApiService();
    }

    public ResultInfo findPerson(long id) throws Exception {
        return this.service.invokeByPath("POST", "/person", Map.of("id", id));
    }

    public ResultInfo invokeSavedApi(String apiID, Map<String, ?> parameters) throws Exception {
        return this.service.invokeById(apiID, parameters);
    }
}
```

两个方法都只调用已发布且启用的 API；没有可用发布版时抛出状态为 404 的 `DatawayException`。路径是 `/person`，不是包含入口前缀的 `/api/person`。

返回值是 `ResultInfo`，包含状态、Header、数据和编码方式。`getData()` 返回结果处理器生成的数据，选择 Structure 时包含外层响应结构。返回流或二进制资源时，由 Java 调用方完成读取和关闭。

程序调用不要求传 `UserIdentity`，也不执行 HTTP 身份校验。它仍经过 `ApiInterceptor`，其中 `source()` 为 `ApiCallSource.PROGRAMMATIC`，`identity()` 为匿名身份；应用可在拦截器中按来源限制调用。真实 HTTP 请求的来源为 `HTTP`，控制台 Perform、Smoke 调试为 `DEBUG`，列表页调用已发布 API 也属于 `HTTP`。

### 管理草稿与发布版

```java title="读取、保存和发布"
AdminService admin = dataway.getAdminService();
ApiState current = admin.getApiById(apiID);
ApiDefinition draft = admin.getDraftByApi(apiID);
draft.setDescription("Updated description");
ApiState saved = admin.save(draft, current.getRevision());
ApiState published = admin.publish(apiID, saved.getRevision());
```

这里使用 `net.hasor.dataway.service.admin.AdminService` 及 `net.hasor.dataway.model` 下的三个模型。`ApiDefinition` 使用无参构造和 setter；新建接口以 `version=0` 保存。

版本在成功写入后递增，保存之后发布要使用保存返回的版本。发生 `DataConflictException` 时重新读取并合并修改，不要直接覆盖另一位开发者的修改。新建 API 的路由冲突也会报告冲突。

- `ApiState` 包含 API ID、revision、published、enabled、hasDraft。
- `getDraftByApi` 返回草稿；`getReleaseByApi` 返回当前或最近发布版，停用后仍可读取。
- `getHistoryByApi`、`getHistoryById` 读取发布历史。
- `list()` 返回不带脚本的定义列表。

直接调用 `AdminService` 不经过 HTTP 权限检查和 `AdminInterceptor`。应用对外提供管理服务时，应自行验证权限。内置管理 API 由 `DatawayAdminHandler` 完成这两项工作。

## 4. 身份与权限迁移

删除内置登录配置和 `LoginPerformChainSpi`、`LoginTokenChainSpi` 的注册，复用应用现有的登录入口、会话或 JWT 校验。登录拦截器取得可信用户后，为本次请求设置身份：

```java title="登录拦截器中的身份绑定"
request.setAttribute("login.identity",
        UserIdentity.consoleAdmin(verifiedUserId, Map.of("tenant", tenantId)));
```

`request` 是本次 HTTP 请求，`verifiedUserId`、`tenantId` 来自已经验证的登录状态。每次请求都要完成身份解析；不能把客户端提交的用户名或角色直接当作已认证身份。

| 工厂方法 | 默认允许的操作 |
| --- | --- |
| `UserIdentity.anonymous(attributes)` | 无权限 |
| `UserIdentity.authenticated(id, attributes)` | 已发布 API 调用、文档访问 |
| `UserIdentity.consoleReadOnly(id, attributes)` | API 与文档访问，以及列表、详情、历史查看 |
| `UserIdentity.consoleAdmin(id, attributes)` | 全部操作，包括保存、调试、发布、停用、删除 |

`DefaultAuthorizationCheck` 调用 `identity.checkOperation(operation)`。通常实现或配置 `IdentityProvider` 即可使用预设权限；自定义权限规则通过 `DatawayConfig.authorizationCheck(...)` 接入。

`AuthorizationCheck` 只接收身份和 `Operation`。按某个 API、租户、路径限制访问时，在 `ApiInterceptor` 或 `AdminInterceptor` 中读取 `definition()`、`parameters()` 等目标信息。管理集合操作可能没有具体 definition，代码需要处理 `null`。

页面资源不经过这两组操作拦截器，也没有 `Operation.RESOURCE`。需要阻止未登录用户加载控制台页面时，在宿主 MVC 或网关保护页面路径。管理接口本身仍逐操作校验权限。详细配置见[身份鉴权](/docs/dataway/authorization)。

## 5. SPI 对应关系

| old-hasor 扩展 | 新版接入点 | 迁移重点 |
| --- | --- | --- |
| `AuthorizationChainSpi` | `AuthorizationCheck`，按目标限制时配合两组拦截器 | 将登录识别和操作许可分开 |
| `LoginPerformChainSpi`、`LoginTokenChainSpi` | 应用登录系统 + `IdentityProvider` | 不再使用 Dataway 内置登录页或会话 |
| `PreExecuteChainSpi` | `ApiInterceptor` | 直接返回可短路，调用 `chain.proceed(context)` 继续执行 |
| `ResultProcessChainSpi` | `ApiInterceptor` 的返回值处理、try/catch，或 `ResultHandler` | 区分业务执行拦截与最终响应格式 |
| `SerializationChainSpi.SerializationInfo` | `ResultHandler`、`ResultInfo`、`ResultInfoUtils` | 显式设置状态、媒体类型、Header 与正文 |
| `CompilerSpiListener` | 无直接对应监听器 | 通用配置使用 `configureQuery`；替换编译与跨请求缓存需另行设计 |
| `LookupDataSourceListener`、`LookupConnectionListener` | `ConnectionProvider` | 将数据源名称、连接获取和事务归属明确交给宿主 |
| `FxSqlCheckChainSpi` | SQL 模块的 `SqlExecutionInterceptor` | 注册到 `ExecuteContext`，用继续执行或抛错表达检查结果 |
| 自定义元数据提供者 | `ApiDataAccessLayer` | 增加批次原子性、版本比较、路由唯一性，不能仅改方法名 |

这些对象通过 `DatawayConfig` 或 SQL 执行器配置注册，不再沿用 Hasor 的 `autoLoadSpi` 和 `SpiTrigger` 注册方式。

### 执行拦截与缓存

`ApiInterceptorContext` 提供 definition、releaseId、parameters、operation、identity、source。发布调用有 releaseId，草稿调试的 releaseId 为 `null`。HTTP request、response 没有放入拦截上下文。

下面为 API 调用记录耗时：

```java title="ApiTimingInterceptor.java"
import net.hasor.dataway.service.script.ApiInterceptor;
import net.hasor.dataway.service.script.ApiInterceptorChain;
import net.hasor.dataway.service.script.ApiInterceptorContext;

public class ApiTimingInterceptor implements ApiInterceptor {
    private final System.Logger logger = System.getLogger(ApiTimingInterceptor.class.getName());

    @Override
    public Object invoke(ApiInterceptorContext context, ApiInterceptorChain chain) throws Exception {
        long started = System.nanoTime();
        try {
            return chain.proceed(context);
        } finally {
            this.logger.log(System.Logger.Level.INFO,
                    context.source() + " " + context.definition().getPath()
                            + " " + (System.nanoTime() - started) + " ns");
        }
    }
}
```

在创建 Dataway 前调用 `config.apiInterceptor(new ApiTimingInterceptor())`。管理操作使用 `config.adminInterceptor(...)`，其继续执行方法为无参的 `chain.proceed()`，可用于审计或为管理操作开启宿主事务。

缓存命中时可直接返回 `ResultInfo`，或返回普通对象。当前实现会将非 `QueryResult` 返回值直接转换为响应，不再进入所选 `ResultHandler`；需要保留特定响应结构时，应缓存或构造完整结果。流和上传文件依赖当前请求生命周期，不适合直接跨请求缓存。缓存键应覆盖 API、发布记录、参数，以及影响数据权限的用户或租户。

新版先在 `DatawayEngine.newQuery` 中编译，再进入执行拦截器。执行短路可以跳过脚本运行，不能替代原 `CompilerSpiListener` 的编译缓存。详细用法见 [API 拦截器](/docs/dataway/configuration/api-interceptors)与[管理拦截器](/docs/dataway/configuration/admin-interceptors)。

## 6. 脚本、函数与 SQL

### 保存原始脚本

DataQL 脚本继续使用原有语法；SQL 类型 API 只保存原始 SQL。新版由 `DatawayEngine` 根据脚本类型构造片段 AST，再交给 DataQL 编译，不能把原来的 DataQL 包装脚本当作 SQL 再次包装。

SQL 类型 API 的参数名来自保存的 Parameters 样例。实际请求提供参数值，因此迁移时需要保留样例的键。例如 SQL 使用 `#{id}`，样例中应包含 `id`。

### 函数导入与自定义 UDF

原 `dataql-fx` 不再作为统一函数模块。脚本中的类名导入需要逐项检查；新版未提供所有旧函数名的兼容映射。

```javascript title="Web 函数迁移后的调用"
import 'net.hasor.dataway.function.WebUdfSource' as web;
var trace = web.header('X-Trace-Id');
run web.setHeader('X-Trace-Id', trace);
run web.setCookie('theme', 'dark', {'path': '/', 'maxAge': 3600});
return {'traceId': trace};
```

原 `getHeader`、`getHeaderArray` 改为 `header`、`headerArray`，`getCookie`、`getCookieArray` 改为 `cookie`、`cookieArray`。`tempCookie`、`storeCookie` 改用带属性对象的 `setCookie`。Session、Hasor 上下文等宿主专属能力由应用函数提供。其他函数对照[内置函数库](/docs/dataql/funx)确认导入名、参数和返回值。

Java 自定义函数改用 `net.hasor.dataql.domain.Udf`，唯一调用方法是 `call(Hints, UdfParams)`：

```java title="注册一个 UDF"
config.function("greet", (hints, params) -> {
    Object[] arguments = params.allParams();
    return "Hello " + arguments[0];
});
```

```javascript
return greet('Dataway');
```

函数库、应用对象导入和片段的注册分别使用 `library`、`importSource`、`fragment`，见[引擎扩展](/docs/dataway/engine)。默认 `DatawayFinder` 通过反射创建对象，不会自动把任意类名解析为 Spring、Solon 或 Hasor Bean；需要容器实例时使用 `importSource("appService", () -> service)` 或自定义 Finder。

原程序直接持有 `DataQL` 对象执行查询时，改用 `HostConfiguration → QueryManager → QueryBuilder → Query` 这套入口，见[独立使用引擎](/docs/dataway/dataql-engine/execute)。已经使用 Dataway 的应用可通过 `configureHost`、`configureQuery` 统一注册扩展。迁移原始脚本后重新编译，不复用旧运行时生成的 QIL 缓存。

### 参数与 Hint

请求正文覆盖同名 URL 查询参数，URL 查询参数覆盖 `CustomizeScope` 的 `$` 默认值，再按 `wrapAllParameters` 包装。`@`、`#` 参数由应用的 `CustomizeScope` 提供。旧代码若试图用 `$` 中的默认值强行覆盖用户请求，需要调整。

原 `ApiInfo.getPrepareHint()` 没有作为新版拦截上下文继续暴露。Nacos 导入保存的 `PREPARE_HINT` 只是数据，不会自动执行。将仍需使用的 Hint 迁到脚本的 `hint` 语句或经确认的引擎配置，不要仅因导入字段存在就认为执行行为一致。

SQL 数据源、分页、规则、类型转换等 Hint 应按[SQL 执行器](/docs/dataql/sql)逐项核对。`FRAGMENT_SQL_DATA_SOURCE` 在两版中都存在，可以保留名称，但其中的数据源名必须能被新的 `ConnectionProvider` 解析；直接依赖旧函数类名或旧片段别名的脚本需单独更新。

### 两种事务分别配置

- API 脚本访问业务数据库：通过 `ConnectionProvider` 提供连接；需要事务函数时使用支持 `TransactionalProvider` 的提供者。三套适配分别提供 `SpringTransactionProvider`、`SolonTransactionProvider`、`HasorTransactionProvider`，连接普通 SQL、事务函数与宿主事务。
- 保存、发布等元数据写入：`JdbcDataAccessLayer(DataSource)` 自带批次独立事务；需要与同库审计等业务操作共同回滚时，传入对应 `SpringJdbcExecutor`、`SolonJdbcExecutor`、`HasorJdbcExecutor`。

两条配置互不替代。元数据选用 Nacos 时，它的 CAS 发布也不会自动加入 JDBC 事务。多数据源不会自动成为一个分布式事务，详见[元数据事务整合](/docs/dataway/metadata/transactions)及各框架接入页。

## 7. 响应格式与 API 选项

默认处理器是 Structure，内置名称包括 `structure`、`raw`、`csv`、`text`、`verifyCode`。旧 `SerializationChainSpi` 的自定义输出可以改为结果处理器：

```java title="ApplicationResultHandler.java"
import java.util.LinkedHashMap;
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.result.ResultHandler;
import net.hasor.dataway.service.ResultInfoUtils;

public class ApplicationResultHandler implements ResultHandler {
    @Override
    public ResultInfo handle(ResultContext context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", context.isSuccess());
        body.put("code", context.getCode());
        body.put("message", context.getMessage());
        body.put("data", context.getValue());
        int status = context.isSuccess() ? 200 : 500;
        return ResultInfoUtils.json(status, body);
    }
}
```

```java title="注册并设为默认值"
config.resultHandler("application", new ApplicationResultHandler());
config.defaultResultHandler("application");
```

单个 API 可在选项中设置 `{"resultHandler":"application"}`。`prepareOptions` 用于合并处理器默认值与 API 选项并校验，不是一次性的应用启动回调；实现时应避免副作用。每个处理器可以通过自己的构造方法接收默认配置。

只需更换 Structure 模板时：

```java title="配置默认响应模板"
config.resultHandler("structure", new StructureResultHandler(Map.of(
        "responseFormat", "{\"ok\":\"@resultStatus\",\"data\":\"@resultData\"}")));
```

这里使用 `net.hasor.dataway.result.structure.StructureResultHandler`。单个 API 的同名选项优先于处理器的默认配置。迁移时特别核对：

- 没有显式 `resultHandler` 时，布尔型 `resultStructure` 仍被识别；建议转换为 `structure` 或 `raw`，显式处理器优先。
- 布尔选项必须是真正的 JSON 布尔值，`"false"` 字符串不等于 `false`；显式 `null` 不表示使用默认值。
- `responseFormat` 是 JSON 对象字符串，模板只替换顶层占位符。JSON 序列化统一省略 `null`。
- Structure 的脚本失败响应默认仍为 HTTP 200，正文 `success=false`、`code` 表示执行失败；鉴权、路由、正文解析和编译等前置错误交由宿主处理。
- 结果处理器本身抛错时，当前实现退回 Structure 失败响应。需要自定义状态时由处理器明确返回 `ResultInfo`。
- `byte[]`、`InputStream`、`BinaryModel` 和上传文件支持二进制返回；文件在请求结束后清理，需长期保存时在结束前复制。详见[结果响应](/docs/dataway/capabilities/development/response)。

草稿和每条发布历史分别保存 API 选项，迁移时需逐一核对。

## 8. JDBC 元数据迁移

### 先确认原表内容

| 位置 | old-hasor 的含义 | 新版处理 |
| --- | --- | --- |
| `interface_info.api_script` | 原始 DataQL 或 SQL | 可以继续读取 |
| `interface_release.pub_script` | 可执行脚本，SQL API 可能已包装成 DataQL | 不能直接作为原始 SQL |
| `interface_release.pub_script_ori` | 发布时的原始脚本 | 映射为新版逻辑 `SCRIPT`，或复制到默认列 |
| `api_schema`、`pub_schema` | 请求与响应结构文档 | 保留 JSON，复核当前文档生成结果 |
| `api_sample`、`pub_sample` | 参数与 Header 样例 | 保留，尤其是 SQL 参数名 |
| `api_option`、`pub_option` | 每份定义或发布版的选项 | 按上一节校验与转换 |
| `api_revision`、`pub_revision` | 原表没有统一修订号 | 新增非空版本列，已有记录以 1 初始化 |

`api_status` 的 0、1、2、3 继续表示草稿、已发布、有修改、停用，历史记录保留原 ID 和发布时间。检查旧数据是否存在逻辑删除的 `-1`、孤立发布记录或同一个 API 多条启用记录，先确认处理规则再导入。新版 Java 管理接口使用 `ApiState` 表达状态。

### 方案 A：保留原始脚本列

这是改动较小的接管方式。保持 `api_script`，将发布记录的逻辑 `SCRIPT` 映射到 `pub_script_ori`：

```java title="复用原始脚本列"
config.fieldMapping(EntityType.RELEASE, FieldDef.SCRIPT, "pub_script_ori");
```

`EntityType`、`FieldDef` 位于 `net.hasor.dataway.dal`。自定义过表名时，再通过 `tableMapping(...)` 指定完整表名。

新写入不再维护 `pub_script`，因此必须解除这列的旧 NOT NULL 约束或提供合理默认值。下面以未修改过表名、索引名的 MySQL 表为例，在备份副本停写后执行一次：

```sql title="MySQL 表结构升级：保留 pub_script_ori"
ALTER TABLE interface_info
    ADD COLUMN api_revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE interface_release
    ADD COLUMN pub_revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE interface_release MODIFY pub_script MEDIUMTEXT NULL;

ALTER TABLE interface_info
    MODIFY api_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    MODIFY api_method VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    MODIFY api_path VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE interface_release
    MODIFY pub_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    MODIFY pub_api_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    MODIFY pub_method VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    MODIFY pub_path VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

ALTER TABLE interface_info DROP INDEX uk_interface_info;
CREATE UNIQUE INDEX uk_interface_info ON interface_info (api_method, api_path);
ALTER TABLE interface_release DROP INDEX idx_interface_release_path;
CREATE INDEX idx_interface_release_path
    ON interface_release (pub_method, pub_path, pub_status);
```

数据库需支持完整索引长度；已有同名修订列时不重复添加。表或索引名称不同、使用其他数据库时，应按实际 DDL 调整。索引和比较规则要满足 `(method, path)` 唯一及大小写敏感查询。新建目标库可直接使用[数据库提供者附带的建表脚本](/docs/dataway/metadata/providers/jdbc)。

先核对原始脚本是否完整：

```sql title="核对发布记录"
SELECT pub_id, pub_api_id, pub_type
FROM interface_release
WHERE pub_script_ori IS NULL OR TRIM(pub_script_ori) = '';
```

空字符串需要逐条确认，不应自动用包装脚本替代。还应对照草稿、发布记录的 method/path 和状态，确认调用时能找到正确的启用记录。

### 方案 B：统一为默认脚本列

需要长期统一到默认表结构时，可以在停写副本上复制原文：

```sql
UPDATE interface_release
SET pub_script = pub_script_ori
WHERE pub_script_ori IS NOT NULL;
```

此后不再配置 SCRIPT 字段映射。补齐修订列和索引后，还需解除 `pub_script_ori` 的 NOT NULL 约束，或在完成备份、核对和回退安排后删除它；新版不会为这个额外列写值。不能保留其强制非空约束却省略它的新写入值。

不要对已有表执行完整 `CREATE TABLE` 脚本，也不要通过重新发布全部草稿来替代复制历史记录。全新表导入时，应保留两张表的 ID、状态、原始脚本和发布时间，并补版本值。

### 最早期 JSON 字段

old-hasor 的 JDBC 读取代码兼容过 `requestSchema`、`responseSchema`、`headerData` 等早期键名。新版主要使用 `requestBody`、`responseBody`、`requestHeader`、`responseHeader`。

这类存量 JSON 需按用途迁到当前键名并验证。仅保留未知 JSON 字段，不表示当前控制台或文档服务会使用它；特别是 SQL API，必须确认 `sample.requestBody` 能解析成含参数名的对象。

## 9. Nacos 元数据迁移

旧提供者使用 `INDEX_DIRECTORY_n`、`i_`、`r_` 等多条配置保存数据。新版将草稿与发布历史放在同一个 `NacosSnapshot` 中，以一次 CAS 保证一个写入批次的原子性。

### 导入原 Nacos 布局

1. 停止所有旧写入端，导出并备份原 namespace/group 的相关配置。
2. 在独立的目标 dataId/group 中创建空快照：`NacosSnapshot.empty().serialize()`。
3. 使用指向原 namespace 的 `ConfigService` 创建目标 DAL；需要名称映射时先初始化映射。
4. 调用一次 `importLegacy`，核对记录和脚本，再启动业务入口。

```java title="导入旧 group"
import net.hasor.dataway.dal.nacos.NacosDataAccessLayer;
import net.hasor.dataway.dal.nacos.NacosSnapshot;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;

// configService 由应用创建，目标配置已用空快照初始化。
NacosDataAccessLayer metadata = new NacosDataAccessLayer(
        configService, "dataway-v2", "DATAWAY_V2", 3000);
Dataway dataway = new DatawayConfig().dataAccessLayer(metadata).createDataway();
metadata.importLegacy("HASOR_DATAWAY");
```

`HASOR_DATAWAY` 表示示例中的源 group，要替换成实际配置；目标 group 为 `DATAWAY_V2`。`ConfigService` 决定 namespace，导入器不会自动跨 namespace 搜索。不要把迁移调用永久放到每次启动流程中，也不要覆盖已存在的目标快照。

导入器会保留 ID、状态、发布时间，优先用 `SCRIPT_ORI` 作为 `SCRIPT`，合并旧 schema/sample 分区，并将记录 revision 初始化为 1。没有原文字段时保留原 `SCRIPT`，因此仍需核对 SQL 是否为原文。缺失记录、目录不完整、路由冲突或非空目标会导致导入失败；源配置不修改。

`OPTION` 和 `PREPARE_HINT` 会保留，但选项类型、函数名和 Hint 行为仍需按前文迁移。旧 Nacos 提供者曾限制历史存储，导入只能保留源中实际存在的记录，不能恢复已清理的历史。

所有历史都计入单条 Nacos 配置大小。上线前检查实际服务端限制；CAS 冲突后重新读取，网络超时需先确认服务端是否提交，不能盲目重放。详见[Nacos 提供者](/docs/dataway/metadata/providers/nacos)。

## 10. 控制台、HTTP 与文档

新版控制台应与后端一起更新，不能只替换旧页面中的 API 前缀。管理请求新增版本校验，入口也已拆分。

- 页面默认在 `/admin/`，管理 API 在 `/admin/api`，已发布 API 在 `/api`。
- 文档通过 `/docs/openapi.json`、`/docs/swagger2.json` 提供，分别支持 OpenAPI 3.0 和 Swagger 2.0。原 `/api/docs/swagger2.json` 等链接需调整，默认文档权限由 `Operation.DOCUMENT` 控制。
- `initializer.js` 用 `window.DatawayUI({adminApi, api})` 初始化前端地址；独立部署时填写浏览器实际可访问的代理路径。
- 控制台资源可单独打包或替换，登录继续使用应用的 Cookie/认证方案，详见[控制台部署](/docs/dataway/configuration/console)。

新版 `WebRequest` 由适配层读取正文，支持 JSON、URL 编码表单和 multipart 上传。非空正文应提供正确的 Content-Type；现有拦截器已经读取正文时，确认宿主提供可复用的请求或已解析的表单，避免后续再读空流。文件上传缓存与宿主上传大小限制应分别配置。

原程序若直接请求旧管理 Controller，逐项对照当前路由、方法和参数格式。Java 应用优先改用 `AdminService`；浏览器用户使用匹配版本的控制台。

## 11. 按顺序完成迁移

1. 固定目标版本，清点依赖、API、SPI、脚本导入、Hint 和对外地址。
2. 备份元数据，准备独立副本。先搭起新应用的框架配置、身份提供者、SQL 数据源和事务。
3. 升级 JDBC 副本或导入 Nacos 新快照，保留草稿和历史，核对原始脚本及版本。
4. 转换选项、函数和扩展代码，检查发布 API 的请求方法、参数样例与响应协议。
5. 使用真实 HTTP 和 Java 调用验证，完成后停止旧写入，迁移最终数据并切换流量。
6. 保留旧应用和元数据备份。回退时恢复匹配旧程序的数据副本；新写入不会同步维护旧执行脚本和旧 Nacos 布局。

至少覆盖以下验收场景：

| 场景 | 核对内容 |
| --- | --- |
| 草稿、已发布、有修改、停用 | 只有启用发布版能被调用，草稿内容不会提前生效 |
| 保存、发布、历史、并发修改 | 版本递增、旧版本冲突、历史读取和删除行为 |
| HTTP 与 Java 调用 | method/path、API ID、三种调用来源、拦截器及返回类型 |
| 登录与权限 | 匿名、API 用户、只读用户、管理用户，各自允许的操作 |
| SQL 与参数 | 命名数据源、分页、绑定参数、参数包装、表单与 Header |
| 事务 | 普通 SQL、事务函数、宿主事务；元数据独立或共同回滚 |
| 结果与文件 | 成功、脚本失败、前置异常、模板、null、下载和请求结束清理 |
| 页面与文档 | 代理后的地址、登录 Cookie、Swagger UI 与两个规范接口 |

三套可运行应用见[Spring 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)、[Solon 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-solon-example)、[Hasor 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-hasor-example)。它们提供相同的 SQL、表单、上传、结果处理和文档场景，可作为目标行为的参照。

数据库升级 SQL 须针对实际数据库版本和表结构预演。源码核对入口包括 `Dataway`、`DatawayEngine`、`DatawayQuery`、`ApiServiceImpl`、`AdminServiceImpl`、`JdbcDataAccessLayer` 和 `LegacyNacosReader`。迁移应用时，应以固定依赖版本中的这些契约为准。

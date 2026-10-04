---
title: "10.1 DatawayConfig"
---

`DatawayConfig` 保存核心配置，通过 `createDataway()` 创建服务。入口开关和地址在宿主配置文件中设置，见 [Spring](../integration/spring.md#入口配置)、[Solon](../integration/solon.md#入口配置)、[Hasor](../integration/hasor.md#入口配置)。

```java title="创建 Dataway"
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;

DatawayConfig config = new DatawayConfig()
        .dataAccessLayer(metadata)
        .identityProvider(identityProvider)
        .documentTitle("Order API");
Dataway dataway = config.createDataway();
```

使用框架整合时提供 DatawayConfig 和 DAL，由适配器创建 Dataway。

## 配置项

### 元数据存储

| 方法 | 默认值 | 作用与约束 |
| --- | --- | --- |
| `dataAccessLayer(layer)` | 空 | 指定 `ApiDataAccessLayer`，优先于容器查找；创建核心前必须提供 |
| `tableMapping(entity, name)` | 空 | 按 `EntityType` 设置数据库表名或 Nacos 实体名，未配置时使用提供者默认名称，见[表字段映射](../metadata/mapping.md) |
| `fieldMapping(entity, field, name)` | 空 | 按实体和 `FieldDef` 设置存储字段名，未指定的字段保留提供者默认名称 |

### 身份与拦截器

| 方法 | 默认值 | 作用与约束 |
| --- | --- | --- |
| `identityProvider(provider)` | 空 | 注册 `IdentityProvider` 获取当前身份；为空时使用 `WebRequest::getIdentity`，默认请求为无权限的匿名身份 |
| `authorizationCheck(check)` | 空 | 注册 `AuthorizationCheck`；为空时使用 `DefaultAuthorizationCheck`，通过 `UserIdentity.checkOperation` 校验权限 |
| `adminInterceptor(interceptor)` | 空 | 注册 `AdminInterceptor` 拦截管理操作，多次调用按注册顺序追加 |
| `apiInterceptor(interceptor)` | 空 | 注册 `ApiInterceptor` 拦截脚本执行，多次调用按注册顺序追加 |

### 结果与参数

| 方法 | 默认值 | 作用与约束 |
| --- | --- | --- |
| `defaultResultHandler(name)` | `structure` | 选择默认结果处理器：`structure`、`raw`、`csv`、`text`、`verifyCode` 或注册名称 |
| `resultHandler(name, handler)` | 五种内置处理器 | 按名称注册 `ResultHandler`；名称以英文字母开头，可含英文字母、数字、`_`、`-`、`.`。可替换内置 `structure`、`raw`、`csv`、`text`、`verifyCode`，`default` 为保留名称 |
| `wrapAllParameters(enabled)` | `false` | 将全部调用参数包装到 `wrapParameterName` 下，对 DataQL 和 SQL 均生效 |
| `wrapParameterName(name)` | `root` | 参数包装名称；以字母或下划线开头，仅含字母、数字、下划线 |

### 上传缓存

| 方法 | 默认值 | 作用与约束 |
| --- | --- | --- |
| `uploadTempDirectory(path)` | 空 | 设置上传缓存落盘目录，接收 `Path`；为空时使用系统临时目录，请求结束后清理临时文件 |
| `uploadMemoryThreshold(bytes)` | `51200` 字节 | 单文件内存缓存阈值，超出后落盘；须非负，`0` 表示非空文件直接落盘 |

### 文档信息

| 方法 | 默认值 | 作用与约束 |
| --- | --- | --- |
| `documentTitle(title)` | `Dataway API` | Swagger、OpenAPI 文档标题，须非空白 |
| `documentVersion(version)` | `1.0` | 文档中的应用 API 版本，须非空白 |
| `documentServer(server)` | `/api` | 文档中的业务 API 基础地址；使用根相对路径或 HTTP(S) URL，不含查询参数、片段或凭据 |

### 引擎与扩展

| 方法 | 默认值 | 作用与约束 |
| --- | --- | --- |
| `finder(finder)` | `DatawayFinder` | 配置 `Finder`，负责对象、资源和片段查找；传入 `null` 恢复默认实现 |
| `resourceLoader(loader)` | `ClassPathResourceLoader.INSTANCE` | 设置 `DatawayFinder` 的资源加载器；传入 `null` 恢复默认值 |
| `classLoader(loader)` | `DatawayFinder` 所在的 ClassLoader | 设置 `DatawayFinder` 的类加载器；传入 `null` 恢复默认值 |
| `customizeScope(scope)` | 空 | 提供 `$` 默认参数及 `@`、`#` 环境；调用参数覆盖 `$` 默认值。为空时使用返回空 Map 的实现 |
| `configureHost(customizer)` | 空 | 接收 `Consumer<HostConfiguration>`，在核心初始化时按注册顺序执行 |
| `configureQuery(customizer)` | 空 | 接收 `Consumer<QueryBuilder>`，在每次创建查询时按注册顺序执行 |
| `function(name, udf)` | 空 | 注册查询可直接调用的 `Udf` |
| `library(namespace, functions)` | 空 | 注册 `Map<String, Udf>` 函数集，通过 `import` 导入命名空间 |
| `importSource(name, provider)` | 空 | 注册 `Supplier<?>`，为 `import` 提供对象 |
| `fragment(name, provider)` | 空 | 注册 `FragmentProcess` 的供应器，提供命名片段执行能力 |
| `attachment(type, object)` | 空 | 按类型注册引擎资源；SQL 连接和事务提供者以 `ConnectionProvider.class` 注册 |

配置在初始化时生效。外部资源由宿主管理，不同映射使用不同 DAL 实例。

自定义 `Finder` 的加载器由该实现管理，`resourceLoader`、`classLoader` 仅用于 `DatawayFinder` 及其子类。函数、加载器和作用域示例见[引擎扩展](../engine/index.md)。

扩展实现分别见[自定义函数](../engine/functions.md)、[函数库](../engine/libraries.md)、[应用对象导入](../engine/imports.md)、[片段执行器](../engine/fragments.md)、[查找器](../engine/finder.md)、[自定义作用域](../engine/scope.md)和[引擎与查询配置](../engine/customizers.md)。

## 响应与入参配置 {#response}

`defaultResultHandler`、`wrapAllParameters`、`wrapParameterName` 设置应用默认值，单个 API 的选项可覆盖这些值。结果处理器的默认配置通过构造方法传入，例如 `new StructureResultHandler(Map.of("responseFormat", template))`，再通过 `resultHandler("structure", handler)` 注册。

界面设置、参数包装示例和完整响应模板见 [API 选项](../capabilities/development/options.md)。

创建 `DatawayConfig` 时默认注册 Structure、Raw Value、CSV、Text 和 VerifyCode。同名注册替换对应处理器，引擎使用这份配置中的处理器集合。

接口通过 `resultHandler` 选项选择已注册的处理器，完整示例见[自定义结果处理器](../capabilities/result-handlers/custom.md)。

## 文件上传 {#upload}

```java title="上传缓存配置"
import java.nio.file.Path;

config.uploadTempDirectory(Path.of("/var/tmp/dataway"))
        .uploadMemoryThreshold(64 * 1024);
```

文件默认缓存 50 KiB，超出后写入临时目录。阈值须非负，0 表示直接落盘。请求结束后清理缓存和流，持久化文件需在此前保存。

应用函数通过 WebFile 读取文件名、类型、大小和文件流。请求大小上限由宿主配置；前置过滤器读取正文后需保留可重复读取的缓存。

## 文档输出 {#api-documents}

```java title="API 文档配置"
config.documentTitle("Order API")
        .documentVersion("1.0")
        .documentServer("/gateway/api");
```

`documentVersion` 为应用 API 版本。`documentServer` 为对外基础地址，支持根相对路径或 HTTP(S) URL。

代理或 context path 改变时，显式设置业务 API 的对外地址。`documentServer` 用于生成文档中的服务地址，入口注册仍使用宿主配置。

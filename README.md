# Dataway

[中文](README.md) · [English](README.en.md)

内嵌于 Java 应用的数据 API 开发框架，集数据查询、转换、聚合与接口发布于一体。

[Website](https://www.dataql.net) · [Documentation](https://www.dataql.net/docs/dataway/intro/overview) · [Blog](https://www.dataql.net/blog) · [AI 文档索引](https://www.dataql.net/llms.txt)

## 📖 简介 | Introduction

Dataway 提供从脚本开发到 API 发布的完整能力。开发者使用 SQL 或 DataQL 编写数据处理逻辑，在控制台完成编辑、调试和发布，应用通过 HTTP 或 Java 调用已发布的接口。

框架内置 DataQL 语言与执行引擎，支持查询数据库、调用 Java 服务、转换数据结构和聚合多个来源的结果。适用于报表看板、列表与详情查询、表单提交及服务聚合，减少重复的数据访问和接口代码。

![Dataway 连接应用数据、业务服务与前端接口](document/static/img/dataway/application-overview.png)

## ✨ 核心特性 | Features

### ⚙️ 框架特点 (Framework Characteristics)

- 内嵌应用：以 JAR 接入现有 Java 项目，支持 Spring、Solon、Hasor。
- 统一执行：SQL 与 DataQL 共用 API 管理、参数处理、拦截和结果输出流程。
- 复用宿主：接入应用的数据源、业务服务、登录身份和事务管理。
- 按需组合：引擎可独立使用，控制台可独立部署，元数据存储和结果处理器均可替换。

### 🔋 基础能力 (Capabilities)

![Dataway 控制台：编辑脚本、设置参数、预览结果和发布接口](document/static/img/dataway/quickstart-published.png)

- API 管理：
  - [可视化操作](https://www.dataql.net/docs/dataway/capabilities/management)：编辑、调试、测试、发布和停用接口，查看发布历史。
  - [程序化管理](https://www.dataql.net/docs/dataway/capabilities/programmatic)：通过 Java 接口或管理 HTTP API 维护定义和版本。
  - 草稿与发布版分离，修改在再次发布后生效；版本检查用于检测并发更新冲突。
- 数据查询与转换：
  - [DataQL 语言](https://www.dataql.net/docs/dataql/overview)：使用表达式、函数和转换模板筛选字段、计算值、构造嵌套结果。
  - [SQL 执行器](https://www.dataql.net/docs/dataway/dataql-engine/sql)：支持查询、更新、动态 SQL、分页、类型处理和事务。
  - [数据源接入](https://www.dataql.net/docs/dataway/capabilities/datasources)：按名称访问多个数据源，组合数据库与应用服务的结果。
- 请求与响应：
  - [请求参数](https://www.dataql.net/docs/dataway/capabilities/development/request)：接收 URL 参数、JSON、表单和上传文件，通过 Web 函数读写 Header 与 Cookie。
  - [结果处理器](https://www.dataql.net/docs/dataway/capabilities/result-handlers)：输出结构化结果、原始值、CSV、文本和验证码图片，支持二进制响应与自定义处理器。
  - 控制台按响应类型预览 JSON、文本、表格和图像，并提供文件下载。
- 接口调用与文档：
  - 通过 HTTP 访问已发布 API，或使用 [ApiService](https://www.dataql.net/docs/dataway/capabilities/development/java) 从 Java 按路径、标识直接调用。
  - [文档生成](https://www.dataql.net/docs/dataway/capabilities/document)：生成 OpenAPI 3.x 和 Swagger 2.0 文档，配合 Swagger UI 展示和调用接口。
- 身份与扩展：
  - [身份鉴权](https://www.dataql.net/docs/dataway/authorization)：接入应用登录体系，按身份控制 API 访问和管理操作。
  - [管理拦截器](https://www.dataql.net/docs/dataway/configuration/admin-interceptors)与 [API 拦截器](https://www.dataql.net/docs/dataway/configuration/api-interceptors)：接入审计、校验、缓存等应用逻辑。
  - [引擎扩展](https://www.dataql.net/docs/dataway/engine)：注册 Java 函数、应用对象、代码片段和自定义作用域。
- 元数据存储：
  - [JDBC](https://www.dataql.net/docs/dataway/metadata/providers/jdbc) 或 [Nacos](https://www.dataql.net/docs/dataway/metadata/providers/nacos) 保存接口定义、草稿和发布记录。
  - 支持表与字段映射、宿主事务整合和自定义存储提供者。

## 💡 为何选择 Dataway？ | Why Dataway

- 减少重复开发：通过 SQL 和脚本实现常见数据接口，在控制台完成参数调试与结果预览。
- 按业务组织结果：一次 API 调用可查询多个数据源、调用应用服务，并按前端需要转换和聚合数据。
- 接入现有工程：保留应用的登录、数据源和事务配置，通过框架适配器接入现有 Web 容器。
- 按需扩展能力：使用 UDF、代码片段、拦截器和结果处理器扩展处理流程，Java 引擎也可独立运行。

## 🚀 使用介绍

### 1. 引入依赖

以 Spring Boot 项目为例，引入框架整合、JDBC 元数据存储和 SQL 执行扩展：

```xml
<!-- Spring 框架整合，包含核心与控制台资源 -->
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-spring</artifactId>
    <version>5.0.0</version>
</dependency>
<!-- 保存接口定义和发布记录 -->
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-meta-jdbc</artifactId>
    <version>5.0.0</version>
</dependency>
<!-- SQL 执行能力 -->
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataql-sqlproc</artifactId>
    <version>5.0.0</version>
</dependency>
```

通过 `DatawayConfig` 配置身份识别、数据库连接和脚本扩展，通过 `ApiDataAccessLayer` 接入元数据存储。完整配置见[快速开始](https://www.dataql.net/docs/dataway/intro/quickstart)；[Spring 示例](example/dataway-spring-example)提供数据初始化、登录、控制台和 Swagger UI。

### 2. 实战代码

#### SQL 查询接口

创建 `POST /person-query`，选择 DataQL 脚本类型。以下脚本使用 `ds1` 数据源，将请求参数 `id` 绑定到 SQL：

```javascript
hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
var query = @@selectSql(id)<%
    SELECT id AS "id", name AS "name", balance AS "balance"
    FROM example_people
    WHERE id = #{id}
%>;
return query(${id});
```

保存并发布后，使用 Spring 示例的登录接口获取 Cookie，再调用 API：

```bash
curl -c cookies.txt -X POST http://127.0.0.1:8080/session/login \
  -d 'username=admin&password=example-password'

curl -b cookies.txt http://127.0.0.1:8080/api/person-query \
  -H 'Content-Type: application/json' \
  -d '{"id":1}'
```

默认使用 Structure 处理器包装响应，`value` 中包含查询结果：

```json
{"id": 1, "name": "Alice", "balance": 100}
```

#### 数据结构转换

使用 DataQL 转换模板选取字段、计算新值。`people` 也可以替换为 SQL 查询或应用函数的返回结果：

```javascript
var people = [
    {"name": "Alice", "age": 25},
    {"name": "Bob", "age": 30}
];

return people => [{
    "name",
    "nextAge": age + 1
}];
```

脚本返回：

```json
[
    {"name": "Alice", "nextAge": 26},
    {"name": "Bob", "nextAge": 31}
]
```

#### Java 调用接口

从框架容器获取 `Dataway` 实例后，使用 `ApiService` 调用已发布且启用的 API。路径使用定义中的路径，不包含 HTTP 入口的 `/api` 前缀：

```java
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.script.ApiService;

ApiService api = dataway.getApiService();
Map<String, Object> parameters = Map.of("id", 1);
ResultInfo result = api.invokeByPath("POST", "/person-query", parameters);

System.out.println(result.getData());
```

Java 调用共用 API 拦截器、参数处理和结果处理器，也可通过 `invokeById` 按 API 标识调用。

## 📚 文档与资源 | Resources

- [官方网站](https://www.dataql.net)、[快速开始](https://www.dataql.net/docs/dataway/intro/quickstart)、[文档指南](https://www.dataql.net/docs/dataway/intro/overview)。
- 框架整合：[Spring](https://www.dataql.net/docs/dataway/integration/spring)、[Solon](https://www.dataql.net/docs/dataway/integration/solon)、[Hasor](https://www.dataql.net/docs/dataway/integration/hasor)。
- 示例工程：[Spring + JDBC](example/dataway-spring-example)、[Solon + JDBC](example/dataway-solon-example)、[Hasor + JDBC](example/dataway-hasor-example)、[Spring + Nacos](example/dataway-spring-nacos-example)。
- 语言与引擎：[DataQL 语言手册](https://www.dataql.net/docs/dataql/overview)、[独立使用引擎](https://www.dataql.net/docs/dataway/dataql-engine)、[数据处理样例](example/dataql-blog-example)。
- [博客文章](https://www.dataql.net/blog)、[AI 文档索引](https://www.dataql.net/llms.txt)。
- [开发指南](community/README.md)：仓库结构、环境准备、编译测试与文档维护。

## 📄 许可证 | License

Dataway 使用 [Apache License 2.0](LICENSE.txt) 许可协议。

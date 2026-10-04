---
title: "2. 快速开始"
description: "配置 Spring Boot 与 SQL 扩展，在控制台创建、调试和发布 API，再通过浏览器、HTTP 或 Swagger UI 调用。"
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

本节创建 `POST /person-query`：接收人员 `id`，执行 SQL 查询，返回数据库中的人员信息。完整配置见 [Spring Boot + JDBC 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-example)。示例包含 H2 数据库、登录页面、Dataway 控制台和 Swagger UI。

使用 Nacos 存储元数据时，参见独立的 [Spring Boot + Nacos 示例](../integration/spring.md#nacos-example)，接口发布和调用步骤相同。

## 配置工程

### 引入 Dataway

在 Spring Boot MVC 项目中引入框架整合、JDBC 元数据存储和 SQL 扩展：

<Tabs groupId="build-tool">
<TabItem value="maven" label="Maven" default>

```xml title="pom.xml"
<!-- Spring 框架整合 -->
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-spring</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
<!-- 保存接口定义和发布记录 -->
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataway-meta-jdbc</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
<!-- SQL 执行能力 -->
<dependency>
    <groupId>net.hasor</groupId>
    <artifactId>dataql-sqlproc</artifactId>
    <version>@project.docsVersion@</version>
</dependency>
```

</TabItem>
<TabItem value="gradle" label="Gradle">

```groovy title="build.gradle"
// Spring 框架整合
implementation 'net.hasor:dataway-spring:@project.docsVersion@'
// 保存接口定义和发布记录
implementation 'net.hasor:dataway-meta-jdbc:@project.docsVersion@'
// SQL 执行能力
implementation 'net.hasor:dataql-sqlproc:@project.docsVersion@'
```

</TabItem>
</Tabs>

### 核心配置

[DatawayConfiguration](https://gitee.com/zycgit/dataql/blob/dev/example/dataway-spring-example/src/main/java/net/hasor/dataway/spring/example/config/DatawayConfiguration.java) 中的三个核心 Bean 如下：

```java title="DatawayConfiguration.java"
// 配置身份识别、SQL 连接和脚本事务。
@Bean
public DatawayConfig datawayConfig(IdentityProvider identityProvider,
        ConnectionProvider connections) {
    return new DatawayConfig()
            .identityProvider(identityProvider)
            .attachment(ConnectionProvider.class, connections);
}

// 提供当前用户身份，供权限校验使用。
@Bean
public IdentityProvider identityProvider() {
    return new RequestIdentityProvider(LoginInterceptor.IDENTITY_ATTRIBUTE);
}

// 为 SQL 执行和事务函数接入 Spring 的连接与事务管理。
@Bean
public ConnectionProvider connectionProvider(ApplicationContext context) {
    return new SpringTransactionProvider(context);
}
```

```yaml title="application.yml"
dataway:
  # 启用 API 访问
  api-enabled: true
  # 启用管理控制台
  admin-enabled: true
  # 启用接口文档
  docs-enabled: true
```

## 发布接口

![在控制台编辑 SQL 查询并发布 person-query 接口](/img/dataway/quickstart-published.png)

### 创建接口

启动应用后打开 `http://127.0.0.1:8080/`，使用 `admin` / `example-password` 登录，进入 **管理控制台**。点击 **New**，填写：

- 请求方法选择 `POST`。
- 接口路径填写 `/person-query`。
- 脚本类型选择 `DataQL`。

在左侧编辑器输入脚本：

```javascript title="接口脚本"
hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
var query = @@selectSql(id)<%
    SELECT id AS "id", name AS "name", balance AS "balance"
    FROM example_people
    WHERE id = #{id}
%>;
return query(${id});
```

脚本选择 `ds1` 数据源，将请求中的 `id` 绑定到 SQL 的 `#{id}`。

### 调试接口

在右侧 **Parameters** 中输入：

```json title="调试参数"
{
  "id": 1
}
```

点击 **Execute Query** 调试当前脚本，右下方 **Result** 显示 Alice 的信息。将 `id` 改为 `2` 再次执行，可以查看 Bob 的信息。

### 保存并发布

依次点击 **Save → Smoke Test → Publish**，完成保存、测试和发布。状态变为 `Published` 后，接口即可调用。

## 接口调用

![在 Dataway UI 接口列表中调用 person-query](/img/dataway/quickstart-list.png)

已发布接口的调用地址为 `http://127.0.0.1:8080/api/person-query`，其中 `/api` 是业务入口前缀。

### 浏览器中调用

在 Dataway UI 顶部点击 **Interface**，进入接口列表：

1. 选择已发布的 **POST /person-query**。
2. 在右侧 **Parameters** 中填写 `{"id": 1}`。
3. 点击 **Execute Query**，在下方 **Result** 查看 HTTP 状态和 Alice 的信息。

### HTTP 调用

<Tabs groupId="http-client">
<TabItem value="curl" label="curl" default>

```bash title="curl 调用"
# 登录示例应用并保存 Cookie
curl -c cookies.txt -X POST http://127.0.0.1:8080/session/login \
  -d 'username=admin&password=example-password'

# 调用已发布的接口
curl -b cookies.txt http://127.0.0.1:8080/api/person-query \
  -H 'Content-Type: application/json' \
  -d '{"id":1}'
```

</TabItem>
<TabItem value="javascript" label="JavaScript">

```javascript title="JavaScript 调用"
// 在已登录的示例页面中运行，沿用登录 Cookie。
const response = await fetch('/api/person-query', {
    method: 'POST',
    credentials: 'same-origin',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({id: 1})
});
if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
}
const result = await response.json();
console.log(result);
```

</TabItem>
</Tabs>

默认返回结构化结果：

```json title="响应结果"
{
  "success": true,
  "message": "OK",
  "code": 0,
  "lifeCycleTime": 2,
  "executionTime": 1,
  "value": {
    "id": 1,
    "name": "Alice",
    "balance": 100
  }
}
```

耗时单位为毫秒，每次执行可能不同。将 `id` 改为 `2` 再次调用，返回 Bob 的信息。

### SwaggerUI

保持登录状态，打开示例的 `http://127.0.0.1:8080/swagger/index.html`。Swagger UI 沿用登录 Cookie。

1. 展开 **POST /person-query**，点击 **Try it out**。
2. 在 **Request body** 中填写 `{"id": 1}`。
3. 点击 **Execute**，在 **Server response** 中查看状态码和响应正文。

![通过 Swagger UI 调用 person-query 并查看 SQL 查询结果](/img/dataway/quickstart-invoke.png)

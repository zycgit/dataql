---
slug: /dataway/principles
title: "4. 工作原理"
description: "Dataway 的运行过程、请求处理和 API 发布机制。"
---

Dataway 根据已发布的接口定义执行脚本，将业务数据整理为 HTTP 响应。

## 架构 {#architecture}

![Dataway 技术架构：请求环境、已发布 API、UDF、片段执行器与响应](/img/dataway/architecture.svg)

应用通过 HTTP 发起请求，Context 承载本次调用的参数、身份、Header 和 Cookie。Dataway 按请求方法和路径读取已发布的 API，交给 DataQL 执行，脚本在执行过程中通过两类扩展获取数据：

- UDF 调用应用提供的函数，访问 HTTP、RPC 或本地业务服务。
- `FragmentProcess` 执行 SQL 或自定义脚本片段。SQL 使用应用配置的数据源和事务。
- DataQL 汇集调用结果，完成筛选、计算和结构转换。Dataway 按响应配置输出 JSON 或二进制数据。

## 请求处理 {#request-flow}

宿主 MVC 完成路由和登录校验，适配器提供 `WebRequest`、`WebResponse`。`WebHandler` 通过 `IdentityProvider` 获取当前身份。

![请求处理：管理请求经过 Controller 和 AdminService，业务请求经过发布记录查找、编译和执行；控制台调试复用执行引擎](/img/dataway/request-flow.svg)

### API 调用

DataQL 脚本直接编译。SQL 由引擎构建片段调用语法树，交给 DataQL 编译、`dataql-sqlproc` 执行。SQL 参数名取自请求样例，参数值在调用时传入。单个 API 的选项覆盖 [DatawayConfig](../configuration/core.md) 中的参数包装和响应模板。查询 Hint 为 Web 函数提供本次请求与响应，执行结束后清理绑定。具体调用流程如下：

1. API Handler 检查 `INVOKE` 权限，解析查询参数和正文。正文按 Content-Type 及其 charset 读取，默认 UTF-8。
2. Handler 按 `(method, path)` 查找启用的发布快照，读取脚本、请求样例和接口选项。
3. `DatawayEngine` 编译脚本，创建 `DatawayQuery`。
4. `DatawayQuery` 按“请求正文 → URL 查询参数 → [CustomizeScope](../engine/scope.md) 默认参数”的优先级合并参数，根据 [API 选项](../capabilities/development/options.md#parameter-wrapping)完成包装并执行 DataQL。
5. Query 处理结果，`WebHandler` 写入响应、清理上传缓存并关闭返回的数据流。

JSON 格式或顶层类型错误抛出状态码为 400 的 `DatawayException`；不支持的媒体类型、字符集或非空正文缺少 Content-Type 时，异常状态码为 415。异常响应由宿主 Web 框架处理。

### 管理与调试

管理请求通过权限检查后，由 Controller 转换数据、`AdminService` 操作 DAL。Perform、Smoke 以 `DEBUG` 操作复用查询引擎，分别执行编辑内容和已保存的草稿。应用也可通过 `dataway.getAdminService()` 进行[程序化管理](../capabilities/programmatic.md)，由调用方负责鉴权。

## 版本管理 {#lifecycle}

同一个 API 保留一份可编辑草稿和多份发布快照。当前启用的快照用于业务调用和文档生成，先前的快照保留为历史版本。

![接口状态与版本：保存更新草稿，发布创建当前快照，前次发布进入历史，历史内容可恢复到草稿](/img/dataway/api-versions.svg)

- **草稿：** 保存时更新，供编辑和调试。保存草稿期间，对外调用继续使用当前发布版本。
- **发布：** 每次发布从草稿创建一份独立快照，切换对外调用的版本。
- **历史：** 保留各次发布的内容。恢复操作将选定内容复制到草稿，再次发布生成新快照。

图中当前发布版本为 v2，历史中保留 v1。再次发布生成 v3，v2 进入历史；恢复 v1 后发布，同样生成新的发布版本。v1、v2、v3 表示发布顺序。停用后，草稿与发布历史继续保留，API 调用和文档展示停止。重新发布后，接口使用新快照提供服务。

<a id="concurrent-updates"></a>

:::info[并发更新]

- `revision` 用于并发校验，保存、发布和停用都会递增；发布序号按发布次数递增，两者独立变化。
- 创建时传 `version = 0`，后续写操作传当前 `revision`；冲突报 409，读取最新版本后重试。
- 发布通过原子批次更新状态、停用前次快照并创建新快照。JDBC 使用事务，Nacos 使用快照 CAS，详见[元数据存储](../metadata/index.md)。

:::

## 拦截器 {#interceptors}

拦截器在管理操作或脚本执行前后加入应用逻辑。

![两类拦截器：AdminInterceptor 围绕 Controller 调用，ApiInterceptor 围绕 DataQL 执行，结果沿调用链返回](/img/dataway/interceptors.svg)

- `AdminInterceptor` 在管理权限校验后执行，获取当前操作和用户身份，用于操作审计、管理动作限制及结果处理。
- `ApiInterceptor` 在参数准备后执行，获取接口定义、调用参数、当前操作和用户身份，用于参数校验、执行计时及事务控制。

Perform、Smoke 调试先经过管理拦截链，执行查询时进入 API 拦截链。

### 调用原理

Dataway 按配置顺序连接拦截器。每个拦截器接收本次上下文和下一个执行入口：

1. 调用进入拦截器，执行 `proceed` 之前的逻辑。
2. 管理链通过 `chain.proceed()` 继续调用，API 链通过 `chain.proceed(context)` 传递上下文。链末执行 Controller 或 DataQL。
3. 执行结果沿调用链反向返回，各拦截器完成 `proceed` 之后的处理。

依次注册 A、B 时，执行顺序为：A 前置 → B 前置 → 目标调用 → B 后置 → A 后置。拦截器直接返回会终止后续调用，并将返回值作为处理结果。

拦截器可在 `proceed` 周围通过 `try/catch/finally` 完成回滚与资源清理。管理链向宿主传递异常；API 链外的 `DatawayQuery` 按 [API 选项](../capabilities/development/options.md)将执行异常转换为失败结果。

---
title: "5.1.2 程序化管理"
description: "通过 AdminService 或管理 HTTP 接口维护 API 草稿、版本和发布记录。"
---

## 接口定义

一个 API 包含标识、HTTP 方法、路径、脚本类型、原始脚本、描述、参数样例、文档模型和执行选项。

- `id` 标识 API；请求方法和路径组合唯一，首次保存后固定。
- `sample` 保存请求与响应样例，`schema` 保存文档模型。
- `options` 保存该接口的参数包装及响应设置。
- 草稿用于编辑，发布记录保存每次上线时的完整定义。

控制台操作见 [可视化操作](management.md)。

## 创建草稿

从框架容器获取 `Dataway`，通过 `dataway.getAdminService()` 使用 AdminService。以下代码创建草稿，仅在首次初始化该接口时执行：

```java title="创建接口草稿"
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.service.admin.AdminService;

AdminService admin = dataway.getAdminService();
ApiDefinition definition = new ApiDefinition();
definition.setId("hello");
definition.setMethod("POST");
definition.setPath("/hello");
definition.setType(ApiScriptType.DATA_QL);
definition.setScript("return {\"message\": ${message}};");
definition.setDescription("问候接口");
definition.setSample("{\"requestBody\":{\"message\":\"Hello Dataway\"}}");
definition.setSchema("{}");
definition.setOptions("{}");

ApiState saved = admin.save(definition, 0);
```

Java 调用方自行指定 API 标识。SQL 类型使用 `ApiScriptType.SQL`，script 保存原始 SQL，sample 声明参数名和样例值。模型和选项的含义见[文档生成](document.md)、[API 选项](development/options.md)。

直接调用 AdminService 时，身份校验和管理拦截由调用方安排。管理 HTTP 请求统一经过 Dataway 的鉴权和管理拦截链。

## 查询定义、状态与历史

| 方法 | 返回内容 |
| --- | --- |
| `list()` | 定义列表，省略脚本内容 |
| `getDraftByApi(apiID)` | 可编辑定义，包含原始脚本 |
| `getApiById(apiID)` | ApiState：apiID、revision、published、enabled、hasDraft |
| `getVersionById(apiID)` | 当前并发更新版本 |
| `getHistoryByApi(apiID)` | 按发布时间从早到晚排列的发布记录 |
| `getHistoryById(historyID)` | 指定发布快照 |
| `getReleaseByApi(apiID)` | 当前或最近一次发布快照，停用后仍可读取，未发布时为 null |
| `getReleaseById(releaseID)` | 指定发布快照，与历史 ID 指向同一记录 |

`published` 表示存在发布记录，`enabled` 表示当前可以调用，`hasDraft` 表示尚未发布或草稿与最近发布内容存在差异。ApiRelease 包含发布标识、序号、时间和该次发布的 ApiDefinition。

## 并发更新

新接口以版本 `0` 保存。修改已有接口时，先读取版本，再读取和修改草稿：

```java title="更新草稿"
String apiID = "hello";
long version = admin.getVersionById(apiID);
ApiDefinition draft = admin.getDraftByApi(apiID);
draft.setDescription("更新后的问候接口");
ApiState saved = admin.save(draft, version);
```

保存、发布、停用均更新 `revision`，后续操作使用返回的新版本。过期版本会产生状态为 409 的 DatawayException，调用方重新读取并合并修改。

## 发布与停用 {#publish-disable}

验证草稿后，使用保存操作返回的版本发布：

```java title="发布已保存草稿"
ApiState published = admin.publish(saved.getApiID(), saved.getRevision());
```

停用时调用 `admin.disableApi(apiID, version)`，返回更新后的 ApiState；草稿和发布历史保留。再次调用 `publish` 恢复服务。

恢复历史时，通过 `getHistoryById(historyID).getDefinition()` 获取定义，使用 API 当前版本保存，再发布。草稿与发布记录的关系见[版本管理](../principles/index.md#lifecycle)。

## 删除接口

使用当前版本调用 `deleteApi`：

```java title="删除接口及历史"
admin.deleteApi(apiID, admin.getVersionById(apiID));
```

删除同时移除接口定义和全部发布历史。需要保留定义时使用[停用](#publish-disable)。

## 管理 HTTP 接口

外部管理程序可复用控制台入口，默认前缀为 `/admin/api`：

| 方法 | 相对路径 | 用途 |
| --- | --- | --- |
| GET | `/api-list` | 查询接口列表 |
| GET | `/get-handlers` | 查询可选的结果处理器名称 |
| GET | `/api-info?id=...`、`/api-detail?id=...` | 读取调用信息、编辑详情 |
| GET | `/api-history?id=...` | 查询发布历史 |
| GET | `/get-history?id=...&historyId=...` | 读取指定历史内容 |
| POST | `/save-api` | 保存编辑内容 |
| POST | `/perform`、`/smoke` | 调试编辑内容、测试已保存草稿 |
| POST | `/publish`、`/disable`、`/delete` | 发布、停用、删除 |

请求携带宿主登录凭据和相应操作权限，更新请求携带 `version`。管理读写响应使用 `success`、`result`；Perform、Smoke 返回脚本执行结果。

## Java 调用已发布 API

使用 `ApiService` 按路径或 API 标识调用已发布且启用的接口，详见 [Java 调用](development/java.md)。

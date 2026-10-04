---
slug: /dataway/authorization
title: "7. 身份鉴权"
hide_table_of_contents: true
description: "接入应用身份，控制 API、管理操作和文档访问。"
---

身份鉴权控制 API 调用、管理操作和文档访问。应用负责登录与凭据校验，Dataway 通过 `IdentityProvider` 获取用户身份，通过 `AuthorizationCheck` 检查操作权限。

## 工作过程

1. 应用校验 Cookie、JWT 等凭据，由 `IdentityProvider` 返回 `UserIdentity`。
2. Dataway 根据请求确定 `Operation`，调用 `AuthorizationCheck.check(identity, operation)`。
3. 检查通过后执行操作；拒绝时抛出 `DatawayException(401, "Unauthorized")`，由宿主框架处理响应。

默认检查器使用身份预设的权限。控制台页面和静态资源由宿主登录拦截器控制访问。

## 使用指引

- [身份接入](identity.md)：选择预设身份，接入应用用户。
- [权限检查](permissions.md)：查看操作权限，扩展检查规则。

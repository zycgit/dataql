---
id: loginchainspi
sidebar_position: 4
title: d.LoginChainSpi
description: DataQL Dataway SPI,LoginPerformChainSpi,LoginTokenChainSpi
---
# LoginChainSpi

LoginChainSpi 是由两个接口组成，分别为：
- `LoginPerformChainSpi`
- `LoginTokenChainSpi`

## LoginPerformChainSpi

AuthorizationChainSpi 是 Dataway 4.1.9 加入的新特性。在此之前针对界面的权限校验，通常需要通过 InvokerFilter 接口来辅助完成。
有了 AuthorizationChainSpi 之后就可以更加简单方便的对界面操作进行权限控制了。

```js
// 配置所有接口，都是只读权限
final Set<String> codeSet = AuthorizationType.Group_ReadOnly.toCodeSet();
apiBinder.bindSpiListener(AuthorizationChainSpi.class, (checkType, apiInfo, defaultCheck) -> {
    return checkType.testAuthorization(codeSet);
});
```

## LoginTokenChainSpi

LoginTokenChainSpi 是 Dataway 4.2.2 加入的新特性。在此之前针对界面的权限校验，通常需要通过 InvokerFilter 接口来辅助完成。
有了 LoginTokenChainSpi 之后就可以更加简单方便的对界面操作进行权限控制了。

```js
// 配置所有接口，都是只读权限
final Set<String> codeSet = AuthorizationType.Group_ReadOnly.toCodeSet();
apiBinder.bindSpiListener(AuthorizationChainSpi.class, (checkType, apiInfo, defaultCheck) -> {
    return checkType.testAuthorization(codeSet);
});
```

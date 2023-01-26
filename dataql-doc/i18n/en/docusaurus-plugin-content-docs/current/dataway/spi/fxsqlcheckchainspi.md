---
id: fxsqlcheckchainspi
sidebar_position: 3
title: c.FxSqlCheckChainSpi
description: DataQL Dataway SPI,FxSqlCheckChainSpi
---
# FxSqlCheckChainSpi

FxSqlCheckChainSpi 是 4.2.0 加入的新特性，应用可以通过这个扩展点对于执行 @@sql 时对 SQL 进行检查和改写。

```js
apiBinder.bindSpiListener(FxSqlCheckChainSpi.class, infoObject -> {
    System.out.println(String.format("[%s] %s", infoObject.getSourceName(), infoObject.getQueryString().trim()));
    return FxSqlCheckChainSpi.NEXT; // 如果存在后续 那么继续执行检查，否则使用 EXIT 常量控制退出后续的检查。
});
```

---
id: lookuplistener
sidebar_position: 5
title: e.LookupListener
description: DataQL Dataway SPI,LookupConnectionListener,LookupDataSourceListener
---
# LookupListener

LookupListener 是由两个接口组成，负责查找数据源，分别为：
- `LookupConnectionListener`
- `LookupDataSourceListener`

## LookupConnectionListener

LookupConnectionListener 是 4.2.0 加入的新特性，应用可以自己管理数据源。每次 DataQL 在需要数据库连接的时候，都会经过该接口来获取对应的数据库连接。
有了 LookupConnectionListener 之后就应用就可以更加方便的和已有数据库框架进行紧密集成。

当 LookupConnectionListener 与 LookupDataSourceListener 同时出现的时。LookupConnectionListener 有享有优先。

```js
apiBinder.bindSpiListener(LookupConnectionListener.class, (lookupName) -> {
    Connection conn = ...
    return conn;
});
```

## LookupDataSourceListener

LookupDataSourceListener 是 4.1.10 加入的新特性，应用可以自己管理数据源。每次 DataQL 在需要数据库连接的时候，都会经过该接口来获取对应的数据源。
有了 LookupDataSourceListener 之后就应用就可以在不停机的情况下动态的改变某个数据源的连接。

```js
Map<String,DataSource> dataSourcePool = ...

apiBinder.bindSpiListener(LookupDataSourceListener.class, (lookupName) -> {
    return dataSourcePool.get(lookupName);
});
```

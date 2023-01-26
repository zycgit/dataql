---
id: dialect
sidebar_position: 3
title: c.配置和方言
description: 在 DataQL 中执行一条SQL，并且使用分页查询。
---

# 配置和方言

## 配置数据源

下例是初始化 Hasor 数据源的模块代码，如果你是基于 Spring 生态，那么请参考与 **[Spring 整合](../../integration/with-springboot.md)** 内容。

:::tip
如果 Hasor 环境中已经初始化了数据源那么无需二次初始化。
:::

```js
public class ExampleModule implements Module {
    public void loadModule(ApiBinder apiBinder) throws Throwable {
        // .创建数据源
        DataSource dataSource = null;
        // .初始化Hasor Jdbc 模块，并配置数据源
        apiBinder.installModule(new JdbcModule(Level.Full, this.dataSource));
    }
}
```

## SQL方言

提示：在 `4.2.1` 版本之后，SQL 执行器会根据使用的数据库连接自动推断对应的方言。通常情况下无需特意通过 [hint FRAGMENT_SQL_PAGE_DIALECT](../hints/hint_sql.md#FRAGMENT_SQL_PAGE_DIALECT) 来设置方言。

方言是可选项，但如果使用分页查询那么就会用到方言。
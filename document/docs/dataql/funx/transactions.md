---
id: transactions
title: 7.10 事务函数
description: "使用 DataQL 事务函数控制 SQL 提交、回滚和传播行为。"
---

:::info 依赖模块

本库由外部模块 `net.hasor:dataql-sqlproc` 提供，仅引入 DataQL 引擎不能使用。应用需提供数据库驱动、可用的数据源，并接入支持事务的连接提供者；只配置普通 SQL 连接还不能调用本库。

独立使用见 [SQL 执行器与事务配置](../../dataway/dataql-engine/sql.md#接入事务)，Dataway 应用见[数据源接入](../../dataway/capabilities/datasources.md#接入事务)。本文示例假定已配置 `ds1`，且 `example_people` 表包含 `id`、`balance` 字段。

:::

## 脚本事务

导入 `TransactionUdfSource`，将需要共同提交的操作放进回调。所有事务函数都只接收一个脚本参数 `callback`，写法为 `() -> { ... }`：

| 项目 | 约定 |
| --- | --- |
| 回调输入 | 不传入参数，也不传入连接或事务对象；可读取外层变量和 `${...}` 请求参数 |
| 回调返回值 | 用 `return` 返回需要的值，可以是数值、字符串、对象、列表或 `null`；事务函数返回同一个结果 |
| 正常结束 | 新建的事务提交；加入已有事务时由外层控制提交。返回 `false` 或 `null` 不表示回滚 |
| 异常结束 | 异常继续向调用方传播；回滚范围由下面的传播行为决定 |

下面把 `ds1` 中一名用户的余额转给另一名用户：

```javascript title="转账事务"
hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
var changeBalance = @@updateSql(id, amount)<%
    UPDATE example_people SET balance = balance + #{amount} WHERE id = #{id}
%>;
if (${amount} <= 0) {
    throw 400, "Amount must be positive";
}
return tran.required(() -> {
    if (changeBalance(${fromId}, 0 - ${amount}) != 1) {
        throw 404, "Source account not found";
    }
    if (changeBalance(${toId}, ${amount}) != 1) {
        throw 404, "Target account not found";
    }
    return true;
});
```

例如两人的初始余额都为 `100`，传入 `{"fromId":1,"toId":2,"amount":5}` 后余额变为 `95`、`105`，脚本返回 `true`。`changeBalance(id, amount)` 返回受影响的行数；目标账号不存在时抛出异常，已扣除的余额回滚。基础示例见 [SQL 事务](../sql/transactions.md)。

## 函数参考

以下函数均接收一个无参回调，返回回调结果；异常向调用方传播。

| 调用 | 用途 |
| --- | --- |
| `tran.required(callback)` | 加入已有事务，没有则创建 |
| `tran.requiresNew(callback)` | 使用独立事务 |
| `tran.nested(callback)` | 已有事务中使用保存点 |
| `tran.supports(callback)` | 有事务则加入 |
| `tran.notSupported(callback)` | 挂起已有事务，在事务外执行 |
| `tran.mandatory(callback)` | 要求已存在事务 |
| `tran.never(callback)` | 要求当前没有事务 |

`tran.tranMandatory(callback)` 是 `mandatory` 的兼容入口。

- 各种调用的提交、回滚和嵌套效果见 [SQL 事务：传播行为](../sql/transactions.md#propagation)。
- `isolation` Hint 的取值、配置示例和生效范围见[隔离级别](../sql/transactions.md#isolation)。
- 数据源、线程和分页调用的事务范围见[执行范围](../sql/transactions.md#scope)。

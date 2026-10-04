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

## 传播行为

以下调用沿用上例的 `tran` 和 `changeBalance`，分别展示七种传播行为。回调给编号为 `1` 的用户增加 `5`，正常执行时返回受影响的行数，账号存在时为 `1`。

| DataQL 调用 | 当前没有事务 | 当前已有事务 |
| --- | --- | --- |
| `tran.required(() -> { return changeBalance(1, 5); })` | 创建事务 | 加入已有事务 |
| `tran.requiresNew(() -> { return changeBalance(1, 5); })` | 创建事务 | 挂起外层，创建独立事务，结束后恢复外层 |
| `tran.nested(() -> { return changeBalance(1, 5); })` | 创建事务 | 创建保存点；失败时回滚到保存点，成功后仍由外层决定最终提交 |
| `tran.supports(() -> { return changeBalance(1, 5); })` | 在事务外执行 | 加入已有事务 |
| `tran.notSupported(() -> { return changeBalance(1, 5); })` | 在事务外执行 | 挂起外层，在事务外执行，结束后恢复外层 |
| `tran.mandatory(() -> { return changeBalance(1, 5); })` | 报错，不执行回调 | 加入已有事务 |
| `tran.never(() -> { return changeBalance(1, 5); })` | 在事务外执行 | 报错，不执行回调 |

兼容入口 `tran.tranMandatory(() -> { return changeBalance(1, 5); })` 与 `mandatory` 的参数、结果和行为完全相同，不是额外的传播方式。

下面演示 `requiresNew` 的独立提交。每次运行前将两人的余额恢复为 `100`：

```javascript title="内层提交，外层回滚"
hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
var changeBalance = @@updateSql(id, amount)<%
    UPDATE example_people SET balance = balance + #{amount} WHERE id = #{id}
%>;
return tran.required(() -> {
    run tran.requiresNew(() -> {
        return changeBalance(2, 5);
    });
    run changeBalance(1, -5);
    throw 500, "Cancel outer transaction";
});
```

脚本以异常结束，用户 `1` 的余额仍为 `100`，用户 `2` 为 `105`。若将 `requiresNew` 改为 `nested`，内层成功也会随外层回滚，两人余额都为 `100`。若改为 `notSupported`，在连接开启自动提交的前提下，用户 `2` 的修改同样保留。

## 执行范围

`FRAGMENT_SQL_DATA_SOURCE` 选择事务使用的数据源，回调中的 SQL 应使用相同名称。一个事务只协调同线程、同数据源名称的 SQL，不提供跨库原子提交。

`FRAGMENT_SQL_TRANSACTION_ISOLATION`（短名称 `isolation`）设置隔离级别：`DEFAULT`、`READ_UNCOMMITTED`、`READ_COMMITTED`、`REPEATABLE_READ`、`SERIALIZABLE`。默认为 `DEFAULT`，沿用事务提供者和数据库设置；该 Hint 本身不会开启事务，加入已有事务时遵循提供者规则。详见 [SQL Hint](../hints/hint_sql.md#FRAGMENT_SQL_TRANSACTION_ISOLATION)。

`nested` 需要事务提供者和 JDBC 驱动支持保存点。内层失败后，外层若要继续执行，调用方需处理该异常；异常一直传出外层回调时，外层也会回滚。`requiresNew` 的提交独立于外层，但其异常若继续传出外层回调，外层仍会失败。

在事务外执行时，SQL 提交行为由连接设置决定，通常为自动提交。此时 `supports`、`notSupported`、`never` 不会因后续回调失败而撤销已提交的修改。

当前本地事务实现中，内层 `required` 的异常若被外层捕获并吞掉，不会自动将外层标为只能回滚。需要整体回滚时，让异常传播出外层回调。

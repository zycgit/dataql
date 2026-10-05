---
id: transactions
title: 6.8 事务
---

事务让一组 SQL 共同提交或回滚。DataQL 通过 `TransactionUdfSource` 提供事务函数，使用前需完成[事务接入](../../dataway/dataql-engine/sql.md#接入事务)。本页介绍脚本用法、传播行为和隔离级别；函数的导入与参数约定见[事务函数库](../funx/transactions.md)。

## 提交和回滚 {#commit-rollback}

在业务数据库中准备两条账户记录：

```sql
CREATE TABLE accounts (id INT PRIMARY KEY, balance INT);
INSERT INTO accounts VALUES (1, 100), (2, 100);
```

将两个更新放到 `required` 回调中：

```javascript
import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
var change = @@updateSql(id, amount)<%
    UPDATE accounts SET balance = balance + #{amount} WHERE id = #{id}
%>;
return tran.required(() -> {
    if (change(1, -10) != 1) {
        throw 404, 'Source account not found';
    }
    if (change(2, 10) != 1) {
        throw 404, 'Target account not found';
    }
    return 'done';
});
```

正常执行后余额为 `90`、`110`，脚本返回 `'done'`。第二条更新失败或目标账户不存在时，异常传出回调，第一条更新一并回滚。SQL 影响行数为 `0` 不属于数据库异常，因此示例主动判断并抛错。

事务函数接收一个无参回调，返回回调的结果。返回 `false`、`null`、错误文本或错误对象都属于正常返回，不触发回滚；需要回滚时应抛出异常。

## 传播行为 {#propagation}

传播行为决定回调遇到已有事务时如何执行。以下“已有事务”指当前事务提供者可识别的、当前线程和同名数据源上的事务。

| 函数 | 没有事务时 | 已有事务时 | 典型用途 |
| --- | --- | --- | --- |
| `required` | 新建事务 | 加入已有事务 | 多步更新共同提交 |
| `requiresNew` | 新建事务 | 挂起外层并新建独立事务 | 独立保存审计记录 |
| `nested` | 新建事务 | 建立保存点 | 允许局部步骤回滚 |
| `supports` | 不开启事务 | 加入已有事务 | 随调用环境执行查询 |
| `notSupported` | 不开启事务 | 挂起外层，在事务外执行 | 暂时离开当前事务 |
| `mandatory` | 抛错，不执行回调 | 加入已有事务 | 要求调用方先开启事务 |
| `never` | 不开启事务 | 抛错，不执行回调 | 禁止在事务中执行 |

这些函数的调用形式相同：`tran.required(() -> { ... })`。传播行为由函数名选择，不通过 SQL Hint 设置。

### required：共享提交边界

以下代码沿用上例的 `tran` 和 `change`：

```javascript
return tran.required(() -> {
    run change(1, -10);
    run tran.required(() -> {
        return change(2, 10);
    });
    throw 500, 'Cancel transfer';
});
```

内层正常返回时尚未提交。外层抛出异常后，两次修改都回滚，两人的余额保持 `100`。

本地 `TransactionProvider` 中，内层 `required` 的异常若被外层 Java 调用方捕获并吞掉，不会自动将外层标记为只能回滚。需要整体失败时，让异常传出最外层事务回调。接入宿主事务后，回滚标记由宿主事务管理器决定。

### requiresNew：独立提交

```javascript
return tran.required(() -> {
    run tran.requiresNew(() -> {
        return change(2, 10);
    });
    run change(1, -10);
    throw 500, 'Cancel outer transaction';
});
```

内层先提交，外层随后回滚。初始余额均为 `100` 时，最终为 `100`、`110`。独立事务使用独立连接，连接池需留出额外连接；内外层修改相同记录时还可能产生锁等待。

内层事务的提交独立于外层，但内层异常若继续向外传播，仍会让外层失败。

### nested：保存点

将上例的 `requiresNew` 改为 `nested` 后，内层通过保存点执行。内层成功不会独立提交，外层回滚会撤销全部修改，余额仍为 `100`、`100`。

内层失败时回滚到保存点。若应用捕获该异常并继续外层事务，保存点之前的修改可继续提交；异常继续传出外层时，整个外层也回滚。此方式要求事务提供者和 JDBC 驱动支持保存点。

### supports、notSupported：事务外执行

```javascript
var balance = @@selectSql(id)<% SELECT balance FROM accounts WHERE id = #{id} %>;
return tran.supports(() -> {
    return balance(1);
});
```

`supports` 有事务时参与，没有时直接查询。将函数名改为 `notSupported`，则即使外层已有事务也会暂时挂起它，回调结束后恢复。

事务外写入的提交方式由连接决定，通常为自动提交。后续脚本失败不会撤销已经提交的写入，因此 `notSupported` 不适合需要随外层回滚的操作。

### mandatory、never：限制调用环境

```javascript
return tran.required(() -> {
    return tran.mandatory(() -> {
        return change(1, 10);
    });
});
```

`mandatory` 在上例中加入外层事务，移除外层 `required` 后会在执行更新前报错。`never` 的要求相反：可单独调用，放入已有事务中则报错。

## 隔离级别 {#isolation}

隔离级别约束并发事务能读取到哪些数据。使用 `FRAGMENT_SQL_TRANSACTION_ISOLATION`，或短名称 `isolation` 设置：

```javascript
hint isolation = 'READ_COMMITTED';
import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
var balance = @@selectSql(id)<% SELECT balance FROM accounts WHERE id = #{id} %>;
return tran.required(() -> {
    var first = balance(1);
    var second = balance(1);
    return {'first':first, 'second':second};
});
```

Hint 本身不会开启事务。上例通过 `required` 开启事务并应用隔离设置；如果两次查询之间有其他事务提交了修改，`READ_COMMITTED` 允许两次结果不同。

| 值 | 含义 | 选择依据 |
| --- | --- | --- |
| `DEFAULT` | 沿用连接或事务提供者的默认设置 | 没有特定要求时使用；默认值 |
| `READ_UNCOMMITTED` | 允许读取其他事务未提交的数据 | 明确接受脏读的场景 |
| `READ_COMMITTED` | 只读取已提交数据 | 避免脏读，同一行的重复读取可能变化 |
| `REPEATABLE_READ` | 防止已读取行的不可重复读 | 同一事务内需要稳定读取 |
| `SERIALIZABLE` | 保证等价于串行执行的结果 | 需要最强隔离，接受等待或并发冲突重试 |

例如另一事务把余额从 `100` 改为 `110`：

- `READ_UNCOMMITTED` 可能在对方提交前读到 `110`，即使对方随后回滚。
- `READ_COMMITTED` 在提交前不能读取对方的修改，提交后的再次查询可能读到 `110`。
- `REPEATABLE_READ` 保持已读取行的可重复读；范围查询、锁定读和自身写入的行为还取决于数据库。
- `SERIALIZABLE` 排除不可串行化的执行结果，数据库可通过锁或冲突检测实现。

不同数据库支持的级别和实现方式不同。设置不支持的级别时可能由驱动报错；DataQL 不模拟数据库的隔离机制。建议在最外层事务统一设置级别，避免内层反复修改已有连接。加入宿主事务时，级别校验和沿用规则由宿主事务提供者决定。

## 数据源与执行范围 {#scope}

```javascript
hint FRAGMENT_SQL_DATA_SOURCE = 'ds1';
```

将此 Hint 放在事务和 SQL 片段之前，二者使用同名数据源。事务作用于当前线程，不能把切换到 `ds2` 的 SQL 自动纳入 `ds1` 的事务，也不提供跨库原子提交。

分页查询采用延迟执行，需要在事务回调内部调用 `page.data()`，才能让实际查询加入事务。只在回调内创建分页对象，再在回调外取数据，查询已离开该事务。

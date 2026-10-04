---
id: transactions
title: 6.8 事务
---

通过 `TransactionUdfSource` 将多条 SQL 放入同一事务，共同提交或回滚。运行前需完成[事务接入配置](../../dataway/dataql-engine/sql.md#接入事务)。

## 使用事务函数

```sql
CREATE TABLE accounts (id INT PRIMARY KEY, balance INT);
INSERT INTO accounts VALUES (1, 100), (2, 100);
```

```javascript
import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
var change = @@updateSql(id, amount)<% UPDATE accounts SET balance = balance + #{amount} WHERE id = #{id} %>;
return tran.required(() -> {
    run change(1, -10);
    run change(2, 10);
    return 'done';
});
```

两次更新共同提交，余额变为 90、110。回调抛出异常时回滚，异常继续向调用方传播。

## 传播与隔离

`required` 加入已有事务，没有时创建。其他传播方式见[事务函数库](../funx/transactions.md#传播行为)。

```javascript
hint isolation = 'READ_COMMITTED';
```

隔离级别还支持 `DEFAULT`、`READ_UNCOMMITTED`、`REPEATABLE_READ`、`SERIALIZABLE`，在创建事务时使用。数据源名称由 `FRAGMENT_SQL_DATA_SOURCE` 决定；一个事务只协调同线程、同名称的连接。

回滚边界与完整函数参考见[事务函数库](../funx/transactions.md)。

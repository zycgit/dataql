---
id: transactions
title: 6.8 Transactions
---

Use `TransactionUdfSource` to commit or roll back several SQL statements together. Complete the [transaction setup](../../dataway/dataql-engine/sql.md#transactions) before running the script.

## Execute a transaction

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

The balances become 90 and 110. An exception leaving the callback rolls the transaction back and propagates to the caller.

## Propagation and isolation

`required` joins an existing transaction or creates one. See [Transaction functions](../funx/transactions.md#propagation) for other propagation modes.

The `isolation` hint supports DEFAULT, READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ and SERIALIZABLE when creating a transaction. `FRAGMENT_SQL_DATA_SOURCE` selects its source. Transactions do not span threads or different source names.

See [Transaction functions](../funx/transactions.md) for rollback boundaries and the full API.

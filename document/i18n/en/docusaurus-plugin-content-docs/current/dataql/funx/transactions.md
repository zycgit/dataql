---
id: transactions
title: 7.10 Transaction functions
description: "Control SQL commits, rollbacks and transaction propagation from DataQL."
---

:::info Module dependency

This library requires the external `net.hasor:dataql-sqlproc` module; the DataQL engine alone does not provide it. The application must supply a database driver, an available data source, and a transaction-capable connection provider. Ordinary SQL connection setup alone is insufficient for these functions.

For standalone use, see [SQL executor and transaction setup](../../dataway/dataql-engine/sql.md#transactions). For Dataway applications, see [data source integration](../../dataway/capabilities/datasources.md#with-transaction-integration). The examples assume a configured `ds1` and an `example_people` table with `id` and `balance` columns.

:::

## Script transactions

Import `TransactionUdfSource` and place related operations inside a callback. Every transaction function accepts one script argument, `callback`, written as `() -> { ... }`:

| Item | Contract |
| --- | --- |
| Callback input | No arguments, connection, or transaction object are passed in. The callback can read outer variables and `${...}` request parameters |
| Callback result | Use `return` for a number, string, object, list, or `null`. The transaction function returns that same result |
| Normal completion | A newly created transaction commits; an existing transaction is committed by its outer scope. Returning `false` or `null` does not request rollback |
| Failure | The exception propagates to the caller. The propagation behavior below determines the rollback scope |

This script transfers a balance between two users on `ds1`:

```javascript title="Transfer a balance"
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

With initial balances of `100` each, input `{"fromId":1,"toId":2,"amount":5}` produces balances of `95` and `105` and returns `true`. `changeBalance(id, amount)` returns the affected row count. If the target account is missing, the script throws and the debit rolls back. See [SQL transactions](../sql/transactions.md) for a basic example.

## Function reference

Each function accepts a callback with no arguments, returns its result and propagates exceptions.

| Call | Purpose |
| --- | --- |
| `tran.required(callback)` | Join a transaction or create one |
| `tran.requiresNew(callback)` | Use an independent transaction |
| `tran.nested(callback)` | Use a savepoint inside an existing transaction |
| `tran.supports(callback)` | Join a transaction when present |
| `tran.notSupported(callback)` | Suspend a transaction and execute outside it |
| `tran.mandatory(callback)` | Require an existing transaction |
| `tran.never(callback)` | Require no current transaction |

`tran.tranMandatory(callback)` is an alias for `mandatory`.

- See [transaction propagation](../sql/transactions.md#propagation) for commit, rollback and nesting behavior.
- See [isolation levels](../sql/transactions.md#isolation) for the `isolation` hint and its scope.
- See [execution scope](../sql/transactions.md#scope) for data sources, threads and deferred pagination.

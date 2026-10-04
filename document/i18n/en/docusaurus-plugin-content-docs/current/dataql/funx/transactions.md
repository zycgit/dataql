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

## Propagation {#propagation}

These calls reuse `tran` and `changeBalance` from the example above and demonstrate the seven propagation behaviors individually. Each callback adds `5` to user `1` and returns the affected row count: `1` when that account exists.

| DataQL call | No current transaction | Existing transaction |
| --- | --- | --- |
| `tran.required(() -> { return changeBalance(1, 5); })` | Create a transaction | Join the existing transaction |
| `tran.requiresNew(() -> { return changeBalance(1, 5); })` | Create a transaction | Suspend the outer transaction, create an independent one, then restore the outer transaction |
| `tran.nested(() -> { return changeBalance(1, 5); })` | Create a transaction | Create a savepoint; roll back to it on failure. Success still depends on the outer commit |
| `tran.supports(() -> { return changeBalance(1, 5); })` | Run outside a transaction | Join the existing transaction |
| `tran.notSupported(() -> { return changeBalance(1, 5); })` | Run outside a transaction | Suspend the outer transaction, run outside it, then restore it |
| `tran.mandatory(() -> { return changeBalance(1, 5); })` | Fail without invoking the callback | Join the existing transaction |
| `tran.never(() -> { return changeBalance(1, 5); })` | Run outside a transaction | Fail without invoking the callback |

The compatibility alias `tran.tranMandatory(() -> { return changeBalance(1, 5); })` has the same argument, result, and behavior as `mandatory`. It is not an additional propagation mode.

The following script demonstrates the independent commit made by `requiresNew`. Reset both balances to `100` before each run:

```javascript title="Inner commit, outer rollback"
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

The script fails, leaving user `1` at `100` and user `2` at `105`. Replacing `requiresNew` with `nested` makes the successful inner work roll back with the outer transaction, leaving both balances at `100`. Replacing it with `notSupported` also preserves the change to user `2` when the connection uses auto-commit.

## Execution scope

`FRAGMENT_SQL_DATA_SOURCE` selects the transaction's data source; SQL inside the callback should use the same name. A transaction coordinates SQL on the same thread and data source name and does not provide atomic commits across databases.

`FRAGMENT_SQL_TRANSACTION_ISOLATION` (alias `isolation`) accepts `DEFAULT`, `READ_UNCOMMITTED`, `READ_COMMITTED`, `REPEATABLE_READ`, and `SERIALIZABLE`. The default, `DEFAULT`, follows the transaction provider and database settings. This Hint alone does not start a transaction; joining an existing transaction follows provider rules. See [SQL Hints](../hints/hint_sql.md#FRAGMENT_SQL_TRANSACTION_ISOLATION).

`nested` requires savepoint support from the transaction provider and JDBC driver. To continue after an inner failure, the caller must handle that exception; if it escapes the outer callback, the outer transaction also rolls back. A `requiresNew` commit is independent of the outer transaction, but an exception escaping both callbacks still makes the outer transaction fail.

Outside a transaction, SQL commits follow connection settings, usually auto-commit. In that case, `supports`, `notSupported`, and `never` cannot undo already committed changes if the callback later fails.

In the current local implementation, swallowing an inner `required` failure does not mark the outer transaction rollback-only. Let the exception leave the outer callback when all work must roll back.

---
id: transactions
title: 6.8 Transactions
---

A transaction commits or rolls back a group of SQL operations together. DataQL exposes transaction functions through `TransactionUdfSource`; complete [transaction setup](../../dataway/dataql-engine/sql.md#transactions) before using them. This page covers script usage, propagation, and isolation. Import and callback conventions are listed in the [transaction function library](../funx/transactions.md).

## Commit and rollback {#commit-rollback}

Prepare two accounts in the business database:

```sql
CREATE TABLE accounts (id INT PRIMARY KEY, balance INT);
INSERT INTO accounts VALUES (1, 100), (2, 100);
```

Place both updates inside a `required` callback:

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

On success, balances become `90` and `110`, and the script returns `'done'`. If the second update fails or its account does not exist, the exception leaves the callback and the first update rolls back too. An affected-row count of `0` is not a database error, so the example checks it explicitly.

A transaction function accepts a callback with no parameters and returns its result. Returning `false`, `null`, error text, or an error object counts as a normal return and does not trigger rollback. Throw an exception to roll back.

## Propagation {#propagation}

Propagation determines how a callback behaves when a transaction already exists. Here, an existing transaction means one recognized by the current transaction provider, on the current thread and under the same datasource name.

| Function | Without a transaction | With an existing transaction | Typical use |
| --- | --- | --- | --- |
| `required` | Start one | Join it | Commit multiple updates together |
| `requiresNew` | Start one | Suspend it and start an independent transaction | Save an independent audit record |
| `nested` | Start one | Create a savepoint | Roll back an optional step |
| `supports` | Run without starting one | Join it | Follow the caller's query context |
| `notSupported` | Run without starting one | Suspend it and run outside it | Temporarily leave the current transaction |
| `mandatory` | Fail before invoking the callback | Join it | Require a caller-owned transaction |
| `never` | Run without starting one | Fail before invoking the callback | Prohibit transactional execution |

All functions use the same calling form: `tran.required(() -> { ... })`. Choose propagation through the function name, not a SQL Hint.

### required: share the commit boundary

The following examples reuse `tran` and `change` above:

```javascript
return tran.required(() -> {
    run change(1, -10);
    run tran.required(() -> {
        return change(2, 10);
    });
    throw 500, 'Cancel transfer';
});
```

The inner return does not commit. The outer failure rolls back both updates; balances remain `100` and `100`.

With the local `TransactionProvider`, an inner `required` failure caught and swallowed by an outer Java caller does not automatically mark the outer transaction rollback-only. Let failures leave the outermost callback when the entire operation must fail. With host transaction integration, rollback-only behavior belongs to the host manager.

### requiresNew: commit independently

```javascript
return tran.required(() -> {
    run tran.requiresNew(() -> {
        return change(2, 10);
    });
    run change(1, -10);
    throw 500, 'Cancel outer transaction';
});
```

The inner transaction commits first; the outer one rolls back. Starting from `100` each, final balances are `100` and `110`. An independent transaction uses another connection, so allow pool capacity for it. Inner and outer updates to the same records can also cause lock waits.

The inner commit is independent, but an inner exception that propagates outward can still fail the outer callback.

### nested: use a savepoint

Replacing `requiresNew` above with `nested` creates a savepoint. Inner success does not commit independently; the outer rollback undoes both changes, leaving `100` and `100`.

An inner failure rolls back to its savepoint. If the application catches that failure and continues the outer transaction, changes before the savepoint can still commit. If the exception leaves the outer callback, the whole outer transaction rolls back. Savepoints require support from the transaction provider and JDBC driver.

### supports and notSupported: execution outside a transaction

```javascript
var balance = @@selectSql(id)<% SELECT balance FROM accounts WHERE id = #{id} %>;
return tran.supports(() -> {
    return balance(1);
});
```

`supports` joins a transaction if present and otherwise runs the query directly. Replacing it with `notSupported` temporarily suspends an existing outer transaction and restores it afterward.

Outside a transaction, connection settings determine commits, usually through autocommit. Later script failures cannot undo committed writes. Use another mode for updates that must roll back with the outer operation.

### mandatory and never: restrict the calling context

```javascript
return tran.required(() -> {
    return tran.mandatory(() -> {
        return change(1, 10);
    });
});
```

`mandatory` joins the outer transaction. Removing `required` makes it fail before the update. `never` imposes the opposite requirement: it can run alone but fails when a transaction exists.

## Isolation {#isolation}

Isolation limits which changes concurrent transactions can observe. Set `FRAGMENT_SQL_TRANSACTION_ISOLATION`, or its short name `isolation`:

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

The Hint does not start a transaction. `required` starts it and applies the setting. If another transaction commits an update between the reads, `READ_COMMITTED` permits different results.

| Value | Meaning | When to use it |
| --- | --- | --- |
| `DEFAULT` | Keep connection or provider defaults | Default when no specific requirement exists |
| `READ_UNCOMMITTED` | Permit reads of uncommitted changes | Only when dirty reads are acceptable |
| `READ_COMMITTED` | Read committed data only | Prevent dirty reads; repeated reads may differ |
| `REPEATABLE_READ` | Prevent nonrepeatable reads of previously read rows | Require stable reads within the transaction |
| `SERIALIZABLE` | Require results equivalent to serial execution | Strongest isolation, accepting waits or conflict retries |

Suppose another transaction changes a balance from `100` to `110`:

- `READ_UNCOMMITTED` can expose `110` before commit, even if that transaction later rolls back.
- `READ_COMMITTED` cannot expose that uncommitted update but can return `110` on a later read after commit.
- `REPEATABLE_READ` preserves repeatable reads of existing rows. Range queries, locking reads, and the transaction's own writes depend on the database.
- `SERIALIZABLE` prevents nonserializable outcomes; the database can use locking or conflict detection.

Supported levels and their implementation vary by database. A driver may reject an unsupported level; DataQL does not emulate database isolation. Set isolation consistently at the outer transaction boundary instead of repeatedly changing an existing connection. When joining a host transaction, validation and inheritance of isolation follow the host provider's rules.

## Datasource and execution scope {#scope}

```javascript
hint FRAGMENT_SQL_DATA_SOURCE = 'ds1';
```

Place the Hint before the transaction and SQL fragments so both use the same datasource name. Transactions belong to the current thread. SQL switched to `ds2` does not automatically join a `ds1` transaction; this API does not provide atomic cross-database commits.

Pagination executes lazily. Call `page.data()` inside the transaction callback to include the actual query. Creating the page object inside the callback and fetching its data afterward executes the query outside that transaction.

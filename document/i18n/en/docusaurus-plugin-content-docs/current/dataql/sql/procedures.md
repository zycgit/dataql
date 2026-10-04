---
id: procedures
title: 6.9 Procedures and multiple results
---

`callSql` and `callXml` use JDBC CallableStatement. The driver must support stored procedures. Parameter mode selects input, output or both.

## Procedure outputs

Create this procedure in MySQL:

```sql
CREATE PROCEDURE add_one(IN input_value INT, OUT output_value INT)
SET output_value = input_value + 1;
```

```javascript
hint bindOut = 'answer';
var calculate = @@callSql(value)<%
    {call add_one(#{value, jdbcType=INTEGER},
                  #{answer, mode=OUT, jdbcType=INTEGER})}
%>;
return calculate(41);
```

`answer` names the output and `bindOut` selects it. The expected result is `{"answer":42}`. Use INOUT with an initial value for input/output parameters. Procedure syntax and JDBC output types depend on the database.

## Multiple results

Results are named `#result-set-N` or `#update-count-N`, sharing a sequence beginning at 1. A result set followed by an update count becomes `#result-set-1` and `#update-count-2`.

```javascript
hint bindOut = '#result-set-1,#update-count-2';
```

`bindOut` selects a named result object and may include output parameters. Each result set still follows unpacking settings. Multi-statement support depends on driver configuration and differs from batch fragment calls.

## Driver requirements

Execution checks `supportsStoredProcedures()`. The tested H2 driver returns false and rejects `callSql`; ordinary database functions can be called with `selectSql`. The MySQL procedure example requires verification on MySQL, not a substituted H2 function call.

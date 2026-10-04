---
id: procedures
title: 6.9 存储过程与多结果
---

`callSql`、`callXml` 使用 JDBC `CallableStatement`，需要驱动支持存储过程。参数通过 `mode` 区分输入、输出和输入输出。

## 输入和输出

以下示例使用 MySQL 存储过程；先在数据库中创建它：

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

输出名 `answer` 对应参数表达式，`bindOut` 选择输出字段；预期返回 `{"answer":42}`。输入输出参数使用 `mode=INOUT` 并传入初值。具体过程语法和输出类型由数据库决定。

## 多个结果

一次执行可产生多个结果集或更新计数，名称分别为 `#result-set-N`、`#update-count-N`，N 按 JDBC 结果出现顺序从 1 递增。例如首个结果集后跟随更新计数，对应 `#result-set-1`、`#update-count-2`。

```javascript
hint bindOut = '#result-set-1,#update-count-2';
```

`bindOut` 返回按名称组织的对象，可同时选择输出参数。单个结果集仍遵循[结果拆包](results.md)设置。支持多语句还取决于驱动配置，不等同于批量片段调用。

## 驱动限制

执行器先检查 `supportsStoredProcedures()`。当前测试所用 H2 驱动返回 false，`callSql` 会拒绝执行；查询内调用数据库函数可使用 `selectSql`。此处 MySQL 示例需在对应数据库验证，不能用 H2 成功调用普通函数作为存储过程验证。

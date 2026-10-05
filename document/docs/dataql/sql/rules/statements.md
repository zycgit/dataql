---
title: 6.3.1 语句生成规则
---

以下示例展示 SQL 片段及生成的 SQL，绑定参数按 `?` 出现顺序列出；换行和空格已规整。完整 DataQL 调用方式见[动态规则](../rules.md)。

## AND、IFAND {#and}

`@{and, 条件}` 根据绑定值选择是否输出条件，并补充 `WHERE` 或 `AND`；`@{ifand, OGNL表达式, 条件}` 由表达式决定是否输出。

```sql
SELECT * FROM people WHERE enabled = 1
@{and, name = #{name}}
@{ifand, minAge != null and minAge >= 0, age >= #{minAge}}
```

传入 `name = "Alice"`、`minAge = 18`：

```sql
SELECT * FROM people WHERE enabled = 1 AND name = ? AND age >= ?
-- 参数：["Alice", 18]
```

- `and` 的绑定参数全部为 null，或内容没有绑定参数时，省略该片段。空字符串、0、false 都是非 null 值。
- 一个片段有多个参数时，只要其中一个非 null，就保留整个片段及其他 null 参数。例如 `@{and, age BETWEEN #{low} AND #{high}}` 在 `low = 18, high = null` 时仍输出两个占位符。
- 内容直接含有 `${...}` 文本替换时，`and` 不会因为绑定参数全部为 null 而省略片段。文本替换规则见[SQL 参数](../parameters.md)。
- `ifand` 的条件为 true 时，即使没有参数或参数为 null，也输出内容。判断非空字符串应显式写出 `name != null and name != ''`。

规则检查前文是否包含 `where`，以及末尾是否为 `where`、`and`、`or` 等，再决定补充连接符。内容直接写 `name = #{name}`，不要重复写前导 `AND`。复杂子查询建议显式写 `WHERE 1 = 1`，由规则追加条件。

固定条件使用 `@{ifand, true, enabled = 1}`，或直接写 SQL；`@{and, enabled = 1}` 没有绑定值，会被省略。

## OR、IFOR {#or}

`or`、`ifor` 的空值处理与 `and`、`ifand` 相同，连接符改为 `OR`。

```sql
SELECT * FROM people WHERE name = #{name}
@{or, id = #{id}}
```

传入 `name = "Alice"`、`id = 2`：

```sql
SELECT * FROM people WHERE name = ? OR id = ?
-- 参数：["Alice", 2]
```

传入 `id = null` 时只保留姓名条件。需要显式判断时写 `@{ifor, id != null, id = #{id}}`。混合 AND、OR 时，SQL 运算优先级不变，需要分组的条件应自行添加括号，例子见[规则嵌套](nesting.md)。

## SET、IFSET {#set}

`@{set, 赋值}` 添加 `SET` 或赋值之间的逗号，保留 null 值。`@{ifset, 条件, 赋值}` 只在条件为 true 时输出。

```sql
UPDATE people
@{set, name = #{name}}
@{ifset, changeAge, age = #{age}}
WHERE id = #{id}
```

传入 `name = "Alice"`、`changeAge = true`、`age = null`、`id = 1`：

```sql
UPDATE people SET name = ?, age = ? WHERE id = ?
-- 参数：["Alice", null, 1]
```

`changeAge = false` 时不更新 age。`set` 不会跳过 null 赋值，适合将列清空；只更新非空值时使用 `ifset`。

逗号由规则补在赋值之前，不要在规则后手动加逗号。全部赋值都被条件排除时，不会自动生成有效的 UPDATE；应保留必需赋值或在脚本中跳过此次更新。

## IF、TEXT、IFTEXT {#if}

`@{if, 条件, 内容}` 在条件为 true 时解析内容中的参数和嵌套规则，不自动添加 SQL 连接符。

```sql
SELECT * FROM people WHERE 1 = 1
@{if, name != null, AND name = #{name}}
@{iftext, newest, ORDER BY id DESC}
```

传入 `name = "Alice"`、`newest = true`：

```sql
SELECT * FROM people WHERE 1 = 1 AND name = ? ORDER BY id DESC
-- 参数：["Alice"]
```

`text` 无条件原样输出，`iftext` 判断条件后原样输出：

```sql
SELECT * FROM people @{text, ORDER BY id ASC}
```

生成 `SELECT * FROM people ORDER BY id ASC`，无参数。`text`、`iftext` 中的 `#{...}`、`${...}` 和嵌套规则不会再次解析；需要绑定值时使用 `if`。这些规则适合脚本中的固定 SQL 关键字，不负责把请求内容转换为 SQL。

所有 `if*` 规则的条件为空时视为成立；非空条件必须求值为布尔 true。数字 1 或字符串 `"true"` 不能替代布尔值。

## IN、IFIN {#in}

`@{in, SQL片段}` 将片段内的一个绑定参数展开成括号包围的占位符列表；`ifin` 先检查条件。规则不添加 AND、OR。

```sql
SELECT * FROM people WHERE enabled = 1
@{in, AND id IN #{ids}}
```

传入 `ids = [1, 2, 3]`：

```sql
SELECT * FROM people WHERE enabled = 1 AND id IN (?, ?, ?)
-- 参数：[1, 2, 3]
```

条件写法为 `@{ifin, ids != null and ids.size() > 0, AND id IN #{ids}}`。集合、数组均可展开；非空标量生成一个占位符。已有的参数 JDBC 选项会应用到各元素，例如 `#{ids, jdbcType=INTEGER}`。

- null 或空集合会省略整个规则，已有的固定条件继续执行。
- 集合中的 null 保留为参数，数据库按 SQL NULL 规则比较。
- 内容必须只产生一个绑定参数；没有参数时不输出，多个参数时抛出异常。
- 不要再为 `#{ids}` 添加括号，规则本身会生成括号。

如果空列表表示“不返回任何记录”，应先在 DataQL 中返回空列表，或通过分支输出 `AND 1 = 0`；不要把省略条件当作空结果。

## CASE、WHEN、ELSE {#case}

`case` 选择首个匹配分支。它在语句生成时执行，只把选中的内容发送到数据库。

条件模式将 `case` 的表达式留空：

```sql
SELECT * FROM people WHERE 1 = 1
@{case, ,
    @{when, name != null, AND name = #{name}}
    @{when, minAge != null, AND age >= #{minAge}}
    @{else, AND enabled = 1}
}
```

`name = "Alice", minAge = 18` 时仅输出 `AND name = ?`，参数为 `["Alice"]`；两者都为 null 时输出 `AND enabled = 1`，没有绑定参数。

值模式比较 `case` 的表达式与 `when` 的值：

```sql
SELECT * FROM people
@{case, sort,
    @{when, 'name', ORDER BY name, id}
    @{when, 'age', ORDER BY age, id}
    @{else, ORDER BY id}
}
```

`sort = "age"` 时生成 `SELECT * FROM people ORDER BY age, id`。分支值是 OGNL 表达式，固定字符串需加引号。值模式先比较对象相等，再比较字符串表示；例如数字 1 与字符串 `"1"` 可以匹配。

`else` 放在最后；没有匹配且没有 `else` 时输出为空。`when`、`else` 只能写在 `case` 中。`case` 只处理直接子级的 `when`、`else`，SQL 正文应写在分支内部。分支内容支持继续嵌套规则。

## MACRO、IFMACRO {#macro}

`macro` 引用应用注册的公共 SQL 片段。假设已将 `activePeople` 注册为 `AND enabled = 1`：

```sql
SELECT * FROM people WHERE age >= #{minAge}
@{macro, activePeople}
```

传入 `minAge = 18`，生成 `SELECT * FROM people WHERE age >= ? AND enabled = 1`，参数为 `[18]`。条件引用写作 `@{ifmacro, activeOnly, activePeople}`；条件为 false 时不查找该片段。

名称直接写注册名，不使用引号或 `#{...}`。引用内容会解析参数和其他规则，并共享当前片段参数。片段未注册时执行报错。完整注册和 XML 引用方式见[SQL 片段](../../../dataway/engine/sql-macros.md)。

## MD5 {#md5}

`md5` 将一个绑定参数转成文本，计算摘要，再作为 VARCHAR 参数绑定。

```sql
INSERT INTO payload_log(digest) VALUES (@{md5, #{content}})
```

传入 `content = "abc"`：

```sql
INSERT INTO payload_log(digest) VALUES (?)
-- 参数：["900150983cd24fb0d6963f7d28e17f72"]
```

null 按空字符串计算，得到 `d41d8cd98f00b204e9800998ecf8427e`。规则必须只产生一个绑定参数，裸写 `content` 不会绑定参数。此规则适合兼容已有摘要字段；MD5 不适合新设计的密码存储。

## UUID32、UUID36 {#uuid}

两个规则均生成新的 VARCHAR 参数：`uuid32` 长度为 32，不含连字符；`uuid36` 长度为 36，包含连字符。

```sql
INSERT INTO event_log(id, correlation_id, message)
VALUES (@{uuid32}, @{uuid36}, #{message})
```

生成 `VALUES (?, ?, ?)`，参数依次为两个新 UUID 和 message。每次执行重新生成，不接收业务参数；生成值不会自动写回 DataQL 变量。

## ARG {#arg}

`#{...}` 是参数配置的常用写法；`arg` 提供对应规则入口，第二段留空，第三段为参数表达式和选项：

```sql
SELECT * FROM people WHERE id = @{arg, , id, jdbcType=INTEGER}
```

传入 `id = 1`，生成 `WHERE id = ?`，绑定一个 JDBC INTEGER 参数。它等价于 `#{id, jdbcType=INTEGER}`。null 仍生成占位符，指定 JDBC 类型有助于驱动绑定 null。`mode`、`name`、`scale`、`typeName`、`typeHandler` 详见[参数选项](../parameter-options.md)。

## PAIRS {#pairs}

`@{pairs, #{集合}, 模板}` 遍历对象、列表或数组。每次循环使用固定局部变量：

- `k`：对象字段名，或列表/数组下标的字符串表示。
- `v`：当前值。
- `i`：从 0 开始的整数序号。

这些变量仅在当前模板中覆盖同名参数，模板仍可读取外部参数。各轮按对象迭代顺序或列表顺序连接，分隔符是空白，**不会自动加逗号**。

```sql
@{pairs, #{names},
    @{iftext, i > 0, UNION ALL}
    SELECT CAST(#{v} AS VARCHAR) AS name
}
```

`CAST` 为 UNION 查询中的绑定参数明确 VARCHAR 类型。

传入 `names = ["Alice", "Bob"]`：

```sql
SELECT CAST(? AS VARCHAR) AS name UNION ALL SELECT CAST(? AS VARCHAR) AS name
-- 参数：["Alice", "Bob"]
```

需要逗号时，在模板中使用 `@{iftext, i > 0, ,}` 输出分隔符，或使用提供 `separator` 的 [foreach 标签](../mybaits.md)。null 和空集合不输出内容；非对象、非集合、非数组的标量会报错。模板中的值使用 `#{v}` 绑定；动态列名必须由应用白名单控制。

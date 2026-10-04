---
id: index
slug: /dataql/funx
title: 7. 内置函数库
---

:::info 依赖模块
7.1～7.8 由 `dataql-engine` 提供。7.9 Web 函数依赖 `dataway-embedded`，在 Dataway 请求上下文中使用；7.10 事务函数依赖 `dataql-sqlproc`，需先配置 SQL 数据源和事务提供者。引入对应模块并完成配置后，脚本才能使用这些扩展函数。
:::

内置函数库是 DataQL 提供的可导入函数集合，用于字符串处理、集合运算、日期计算和格式转换。库通过 `import` 导入，函数通过 `库名.函数名(...)` 调用。

```js
import 'net.hasor.dataql.host.function.basic.StringUdfSource' as string;
return string.toUpperCase('DataQL');
```

结果为 `"DATAQL"`。导入语法见[导入与复用](../syntax/imports.md)，函数作为参数的用法见[函数](../syntax/function.md)。

文中 `function(value[, option])` 表示 `option` 可省略，实际调用使用 `function(value)` 或 `function(value, option)`。示例展示脚本返回值；HTTP 响应的外层结构由结果处理器决定。

## 通用函数

以下函数库随 DataQL 引擎提供，每篇列出资源名、函数参数、返回值和示例。

- [字符串函数](string.md)：查找、截取、连接和大小写转换。
- [集合函数](collect.md)：过滤、排序、分组及对象与列表转换。
- [数值函数](number.md)：限制整数的取值范围。
- [时间与日期函数](datetime.md)：获取时间、读取日期字段及格式化。
- [JSON 函数](json.md)：JSON 文本与数据对象之间的转换。
- [类型转换函数](convert.md)：数值、布尔、文本与二进制转换。
- [编码与摘要函数](codec.md)：Base64、URL 编码、摘要和 HMAC。
- [状态函数](state.md)：计数器和 UUID。

## 扩展模块提供的函数

- [Web 函数](web.md)：由 Dataway 提供，用于读写 Header、Cookie 及处理上传文件，需要本次 Web 请求上下文。
- [事务函数](transactions.md)：由 SQL 执行器提供，用于控制事务边界和传播方式，需要配置事务提供者。

应用可以增加函数库，注册后沿用相同的导入和调用语法，实现方式见[函数库扩展](../../dataway/engine/libraries.md)。

---
slug: /dataway/engine
title: "9. 引擎扩展"
hide_table_of_contents: true
---

Dataway 通过 `DatawayConfig` 接入应用函数、外部片段和宿主资源，使用 `HostConfiguration → QueryManager → QueryBuilder → Query` 构建执行环境。以下各节分别介绍扩展实现、注册配置和脚本用法。

## 使用指引

- [自定义函数](functions.md)：为脚本提供独立的业务函数。
- [函数库](libraries.md)：组织同一命名空间下的多个函数。
- [应用对象导入](imports.md)：通过 import 复用应用对象和依赖。
- [片段执行器](fragments.md)：解释和执行自定义外部片段。
- [查找器](finder.md)：统一对象查找、资源和类加载。
- [自定义作用域](scope.md)：提供默认参数及应用环境。
- [引擎与查询配置](customizers.md)：配置引擎、查询，并向扩展代码提供应用对象。
- [SQL 拦截器](sql-interceptors.md)：观察和控制 JDBC 执行。
- [SQL 片段](sql-macros.md)：注册可复用的 SQL 宏。
- [SQL 规则](sql-rules.md)：扩展动态 SQL 的生成规则。
- [SQL 类型处理器](sql-types.md)：扩展 JDBC 参数写入与结果读取。
- [SQL 方言](sql-dialects.md)：扩展分页与总数查询的 SQL 生成方式。

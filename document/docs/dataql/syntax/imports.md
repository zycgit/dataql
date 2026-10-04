---
id: imports
title: 3.3 导入与复用
---

`import` 将资源绑定到一个脚本名称，后续通过该名称访问字段或调用函数。导入语句放在顶层 Hint 之后、执行语句之前。

## 函数库

```js
import 'net.hasor.dataql.host.function.basic.StringUdfSource' as string;
return string.toUpperCase('DataQL');
```

结果为 `"DATAQL"`。`as string` 指定脚本中的名称，可导入的函数和资源名见 [内置函数库](../funx/index.md)。

## 应用资源

应用可以注册对象或函数，并为其指定资源名。假设应用已注册 `catalog`，其中提供 `findById` 方法：

```js
import 'catalog' as catalog;
return catalog.findById(1);
```

导入名称和可调用方法由应用提供，接入方式见[应用对象导入](../../dataway/engine/imports.md)。

## DataQL 脚本

`import @'路径' as 名称` 将 DataQL 资源导入为可调用函数。导入路径由宿主的资源加载器解析。

```js title="/queries/greeting.dql"
return 'Hello, DataQL';
```

```js title="调用脚本"
import @'/queries/greeting.dql' as greeting;
return greeting();
```

结果为 `"Hello, DataQL"`。应用需提供对应脚本资源，配置见[查找器](../../dataway/engine/finder.md)。

---
title: "9.4 片段执行器"
---

`FragmentProcess` 负责执行 `@@名称(...)<% ... %>` 中的外部片段。DataQL 传递片段原文及命名参数，扩展实现决定如何解释和执行。下面实现一个简单的文本模板片段。

## 实现执行器


```java title="TemplateFragment.java"
package com.example.dataway;

import java.util.Map;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.FragmentProcess;

public class TemplateFragment implements FragmentProcess {
    @Override
    public Object runFragment(Hints hints, Map<String, Object> parameters, String fragmentString) {
        String result = fragmentString.trim();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
```


## 注册配置

`config` 表示应用注册的 `DatawayConfig`，以下配置在创建 Dataway 前完成。
```java
import com.example.dataway.TemplateFragment;

config.fragment("template", TemplateFragment::new);
```


## 脚本调用


```javascript
var greeting = @@template(name)<%Hello, {{name}}!%>;
return greeting('Dataway');
```
返回 `"Hello, Dataway!"`。`name` 是片段形参，调用时的值通过 `parameters.get("name")` 获取；`fragmentString` 为 `<%` 与 `%>` 之间的内容。

## 使用说明

`runFragment` 接收 Hints、参数 Map 和片段文本；`batchRunFragment` 默认逐项调用 `runFragment`，有批处理能力时可自行覆盖。

供应器在查找片段时提供执行器，返回共享实例时需保证并发安全。自定义片段可在 DataQL 脚本中使用；控制台的脚本类型仍由现有 `ApiScriptType` 定义。

SQL 执行器是上述接口的实现，通过 `FragmentProcessFactory` SPI 注册 `selectSql`、`updateSql` 等片段。接入方式见 [SQL 执行器](../dataql-engine/sql.md)。

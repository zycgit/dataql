---
title: "9.4 Fragment processors"
---

`FragmentProcess` executes external blocks in `@@name(...)<% ... %>`. DataQL supplies raw block text and named parameters; the extension interprets and executes them. This example implements a text template.

## Implement the processor


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


## Register the processor

`config` is the application’s `DatawayConfig`; apply these settings before creating Dataway.
```java
import com.example.dataway.TemplateFragment;

config.fragment("template", TemplateFragment::new);
```


## Call from a script


```javascript
var greeting = @@template(name)<%Hello, {{name}}!%>;
return greeting('Dataway');
```
Returns `"Hello, Dataway!"`. `name` is a formal fragment parameter, available through `parameters.get("name")`. `fragmentString` contains the text between `<%` and `%>`.

## Usage notes

`runFragment` receives Hints, a parameter Map and raw text. The default `batchRunFragment` calls it for each parameter set; override it for native batching.

The supplier provides a processor during fragment lookup. Shared instances must support concurrent calls. Custom fragments are available within DataQL scripts; console script types remain defined by `ApiScriptType`.

The SQL executor implements the same interface and registers fragments such as `selectSql` and `updateSql` through the `FragmentProcessFactory` SPI. See [SQL executor](../dataql-engine/sql.md) for setup.

---
id: compilerspilistener
sidebar_position: 2
title: b.CompilerSpiListener
description: DataQL Dataway SPI,CompilerSpiListener
---
# CompilerSpiListener

CompilerSpiListener 也叫做编译拦截器，DataQL 在真正执行查询之前调用。

```js
如果当 PreExecuteChainSpi 中已经通过 future.completed 或者 future.failed 处理了请求，那么就不会引发 CompilerSpiListener。
```

编译拦截器的应用场景主要有两个
- 实现对 QIL 缓存（QIL 是 DataQL 查询编译之后的指令序列，它类似 Java 的 class 文件）
- 改写或替换 DataQL 查询脚本

**QIL 缓存**

```js
public class QilCacheSpi implements CompilerSpiListener {
    private Map<String, QIL> menCache = new ConcurrentHashMap<>();

    public QIL compiler(ApiInfo apiInfo, String query, Set<String> varNames, Finder finder) throws IOException {
        String apiPath = apiInfo.getApiPath();
        if (apiPath.startsWith("/dataql/api/maps/")) {
            if (this.menCache.containsKey(apiPath)) {
                return this.menCache.get(apiPath);
            }
            QIL compiler = CompilerSpiListener.DEFAULT.compiler(apiInfo, query, varNames, finder);
            this.menCache.put(apiPath, compiler);
            return compiler;
        }
        return CompilerSpiListener.DEFAULT.compiler(apiInfo, query, varNames, finder);
    }
}
```

**改写 DataQL 查询**

在所有DataQL 查询的前面都统一追加一个 hint。

```js
public class QilCacheSpi implements CompilerSpiListener {
    public QIL compiler(ApiInfo apiInfo, String query, Set<String> varNames, Finder finder) throws IOException {
        query = "hint XXXXX = true; " + query; // 增加一个 XXXXX hint
        return CompilerSpiListener.DEFAULT.compiler(apiInfo, query, varNames, finder);
    }
}
```

---
id: fragment
sidebar_position: 4
title: d.外部代码片段
description: DataQL 开发手册，外部代码片段是 DataQL 特有能力，它允许在 DataQL 查询中混合其它语言的脚本。并将引入的外部语言脚本转换为 UDF 形式进行调用
---
# 资源加载器(Finder)

外部代码片段是 DataQL 特有能力，它允许在 DataQL 查询中混合其它语言的脚本。并将引入的外部语言脚本转换为 UDF 形式进行调用。

```js
var dataSet = @@sql(item_code) <%
    select * from category where co_code = #{item_code}
%>

// ${itemCode} 是参数
return dataSet(${itemCode}) => [
    { "id","name","code","body" }
]
```

在上面查询中通过 `@@` 指令，开启了一段外部代码片段的定义。这个外部代码片段的执行器名字是 sql

要想实现上述功能，首先编写一个外部代码执行器。执行器接口中接收 `<%、%>` 包裹的所有代码然后调用 JdbcTemplate 类的 query 系列方法执行SQL 查询。

```js
@DimFragment("sql")
public class SqlQueryFragment implements FragmentProcess {
    @Inject
    private JdbcTemplate jdbcTemplate;

    public Object runFragment(Hints hint, Map<String, Object> paramMap, String fragmentString) throws Throwable {
        return this.jdbcTemplate.queryForList(fragmentString, paramMap);
    }
}
```

然后在初始化阶段注册这个外部代码执行器，之后就可以在查询中使用这个外部代码片段了。

```js
public class MyFragment implements QueryModule {
    public void loadModule(QueryApiBinder apiBinder) {
        //扫描所有标记了@DimFragment注解的类并加载它
        apiBinder.loadFragment(queryBinder.findClass(DimFragment.class));
    }
}
```

## 资源加载器(Finder)

资源加载器完整名称是 `net.hasor.dataql.Finder` 其主要负责 `import` 语句导入资源/对象的加载。

通常情况下不会接触到它。

例如：下面这个查询语句中通过 `import` 引用一个容器中的一个 Bean。这就需要用到 `Finder`。

```js
import 'userBean' as ub;//userBean 是 Bean 的名字
return ub().name;
```

下面是 `Finder` 接口的定义。

```js
public interface Finder {
    // 负责处理 import @"/net/hasor/demo.ql" as demo; 的资源加载
    public InputStream findResource(String resourceName);

    // 负责处理 <code>import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;</code>方式的资源的加载。
    public Object findBean(String beanName);

    // 负责处理 <code>import 'net.hasor.dataql.fx.basic.CollectionUdfSource' as collect;</code>方式的资源的加载。
    public Object findBean(Class<?> beanType);
}
```

:::caution
在默认的接口实现中，`findBean(String)` 会加载 Class 然后调用 `findBean(Class<?>)` 完成最终的加载。
`findBean(String)`、`findBean(Class<?>)` 这一组方法建议同时重写。
:::
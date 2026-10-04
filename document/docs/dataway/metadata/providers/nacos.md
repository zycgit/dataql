---
title: "6.3.2 Nacos 提供者"
---

## 引入和准备

```groovy
implementation 'net.hasor:dataway-meta-nacos:@project.docsVersion@'
```

应用创建 ConfigService，配置地址、命名空间和认证，并管理客户端生命周期。

完整应用见 [Spring Boot + Nacos 示例](https://gitee.com/zycgit/dataql/tree/dev/example/dataway-spring-nacos-example)。

首次部署时，在目标 namespace 中创建 group 为 `HASOR_DATAWAY`、dataId 为 `dataway-store.json` 的配置。正文由空快照对象生成：

```java
import net.hasor.dataway.dal.nacos.NacosSnapshot;

String initialContent = NacosSnapshot.empty().serialize();
```

空快照仅用于首次初始化，访问层要求目标配置已存在。

## 创建访问层

```java
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.dal.nacos.NacosDataAccessLayer;

NacosDataAccessLayer metadata = new NacosDataAccessLayer(
        configService, "dataway-store.json", "HASOR_DATAWAY", 3000);
```

参数依次为客户端、dataId、group 和读取超时毫秒数。超时大于 0，访问层注册为 Bean 或传给 DatawayConfig。

## 快照与并发

INFO、RELEASE、路由唯一性和版本信息位于同一 JSON 快照。一次写入读取完整配置、校验变更，再用原内容摘要进行 CAS 发布，失败则报告冲突。

并发修改可能冲突，需重读后提交。写入超时应先确认结果；全部历史受 Nacos 单条配置大小限制，较大数据量可使用 JDBC。

读取使用 ConfigService.getConfig，遵循客户端故障转移策略。

## 名称映射

默认实体为 `INFO`、`RELEASE`。如果改为 `api_definitions`、`api_releases`，初始化内容也应匹配：

```java
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;

String initialContent = JsonUtils.writeValueAsString(Map.of(
        "format", 1,
        "generation", "initial",
        "records", Map.of("api_definitions", Map.of(), "api_releases", Map.of())));
```

DatawayConfig 使用 tableMapping 和 fieldMapping 设置对应名称，规则见[表字段映射](../mapping.md)。

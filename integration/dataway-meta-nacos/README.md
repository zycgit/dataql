# Dataway Metadata Nacos

`net.hasor.dataway.dal.nacos.NacosDataAccessLayer` 与 `dal.jdbc` 平行，实现同一个 `ApiDataAccessLayer`。不需要 JDBC 数据源，Spring、Hasor、Solon 均可配置 `dataway.metadata.type=nacos`，通过 `dataway.metadata.nacos.config-service` 指定宿主客户端；也可通过 `DatawayConfigurer` 或 Builder 显式注入访问层。

应用使用 Nacos 时按需添加扩展依赖（传递引入 Nacos 客户端 3.1.2）：

```groovy
implementation 'net.hasor:dataway-meta-nacos:4.3.0-SNAPSHOT'
```

`dataway-meta-nacos` 独立依赖核心和 Nacos 客户端；`dataway-embedded` 不包含 Nacos 实现，也没有 Nacos 编译或运行时依赖。客户端发行包已经包含 API，不要再混入独立的 `nacos-api` JAR。连接地址、namespace、认证、TLS 等参数由应用创建 `ConfigService` 时配置，Dataway 不读取或托管这些参数，也不会关闭传入的客户端。

首次部署，在选定 namespace 下创建配置，示例为 group `HASOR_DATAWAY`、dataId `dataway-store.json`，内容通过 `NacosSnapshot.empty().serialize()` 从空快照对象生成：

```java
import net.hasor.dataway.dal.nacos.NacosSnapshot;

String initialContent = NacosSnapshot.empty().serialize();
// Publish initialContent to the new configuration during first deployment.
```

这是部署初始化操作，只对新存储执行一次。运行中的快照不能重置或删除，也不能由旧版本或其他程序无条件覆盖。访问层不会自动创建缺失配置，避免多个进程初始化时覆盖彼此的数据。

```java
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.dal.nacos.NacosDataAccessLayer;

// configService 由应用创建或从容器注入。
NacosDataAccessLayer access = new NacosDataAccessLayer(
        configService, "dataway-store.json", "HASOR_DATAWAY", 3000);
Dataway dataway = Dataway.builder().dataAccessLayer(access).build();
```

构造参数依次为客户端、dataId、group、读取超时毫秒数（必须大于零）。SQL 脚本需要的业务数据源仍由应用单独配置，Nacos 仅存储 Dataway 元数据。

接口草稿和发布历史位于同一 JSON 快照，每个批次通过一次 [Nacos CAS 发布](https://nacos.io/docs/v3.1/manual/user/java-sdk/usage/) 完成。记录保留独立版本，草稿路由按 `(method, path)` 唯一；历史发布可以重复使用路由。更新保留未提供的字段，显式 null 表示清空。CAS 失败会报告冲突，包括其他接口同时修改快照的情况；应用应重新读取后再提交。网络超时可能意味着服务端已提交，不能盲目重放写入。

所有记录受 Nacos 单条配置大小限制，发布历史也计入大小。超过服务端限制不会拆成多次非原子写入，应调整服务器配置或选用 JDBC。读取使用 `ConfigService.getConfig`，不另建本地缓存或后台线程；客户端自身的故障转移/快照策略仍然生效，不能将断网读取视为强一致的最新值。

#### 从旧版 Nacos 迁移

停止旧版写入并备份配置，在另一个 dataId 创建上述空快照，然后显式执行一次：

```java
access.importLegacy("HASOR_DATAWAY");
```

迁移读取旧 `INDEX_DIRECTORY_0` 等索引片段，直到 `END`，再读取 `i_`、`r_` 记录；保留原 ID、脚本及原始脚本，将旧 schema/sample 分字段合并回文档，保留 OPTION 和 PREPARE_HINT，版本从 1 开始。只有整个读取、校验成功后才 CAS 发布一次。源配置不会修改或删除；非空目标、缺失记录、截断目录或路由冲突均拒绝迁移。索引监视值变化会中止导入，但这不能代替停写要求。旧版与新版存储布局不同，不能并行写同一份数据。

#### 验证

`embeddedCheck` 包含不依赖服务的 Nacos 契约测试。真实服务验证使用独立任务，客户端只会创建和清理带随机后缀的测试配置：

```bash
DATAWAY_NACOS_SERVER=127.0.0.1:8848 ./gradlew :dataway-meta-nacos:nacosServerTest
```

此测试使用无需认证的隔离 Nacos 服务，需同时开放 HTTP 端口及客户端 gRPC 端口（默认 HTTP + 1000）。

JSON 读写复用 Nacos 客户端自带的 `JacksonUtils`，本扩展不单独创建 JSON mapper。空值字段按 Nacos 默认规则省略；由于发布的是完整快照，更新时显式 null 仍会清除旧值，未指定字段仍保留。

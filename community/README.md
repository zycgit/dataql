# 开发指南

本文面向 DataQL / Dataway 的源码开发者，介绍工程结构、环境准备、编译测试、示例运行和文档维护。产品能力与使用入口见[仓库首页](../README.md)。

- [目录结构](#目录结构)
- [环境要求](#环境要求)
- [编译与测试](#编译与测试)
- [运行示例](#运行示例)
- [控制台开发](#控制台开发)
- [文档开发](#文档开发)
- [发布](#发布)
- [代码与文档约定](#代码与文档约定)

除明确给出 `cd` 的代码块外，命令均在仓库根目录执行。

## 目录结构

```text
dataql/
├── dataql/
│   ├── dataql-engine/         # 语言解析、编译、执行与内置函数
│   └── dataql-sqlproc/        # SQL 执行、动态规则、类型处理与事务
├── dataway/
│   ├── embedded/             # Dataway 核心，模块名 dataway-embedded
│   ├── embedded-web/         # Vue 控制台，模块名 dataway-embedded-web
│   └── platform/             # 平台工程，未纳入当前 Gradle 构建
├── integration/
│   ├── dataway-spring/        # Spring MVC 整合
│   ├── dataway-solon/         # Solon 整合
│   ├── dataway-hasor/         # Hasor Web 整合
│   ├── dataway-meta-jdbc/     # JDBC 元数据存储与建表脚本
│   └── dataway-meta-nacos/    # Nacos 元数据存储
├── example/                  # 独立 Maven 示例工程
├── document/                 # Docusaurus 文档、博客与静态资源
├── community/                # 开发指南与 IDE 格式化配置
├── build.gradle              # 公共编译、测试、覆盖率与发布配置
├── settings.gradle           # Gradle 模块与依赖仓库
├── gradle.properties         # Java 制品版本与公共依赖版本
├── gradlew / gradlew.bat      # Gradle Wrapper
└── build.sh                  # 打包、安装、部署与发布入口
```

Java 源码和测试分别位于各模块的 `src/main`、`src/test`。框架整合测试中的 `example` 包用于搭建应用，`testcase` 包组织测试场景；根目录 `example` 则供独立运行和学习。

## 环境要求

- JDK：安装 JDK 17，Java 模块使用 17 工具链，并以 `--release 17` 编译。
- Gradle：使用仓库提供的 Wrapper，无需单独安装。版本见 `gradle/wrapper/gradle-wrapper.properties`。
- Node.js / npm：控制台构建需要 Node.js 22.13+（22.x）或 24+；文档的 `.nvmrc` 使用 Node.js 22。
- Maven：3.9.9+，用于 `example` 下的独立示例。
- Bash / Git：用于执行 `build.sh`。Windows 可使用 Bash 环境或直接调用 `gradlew.bat`。
- Python 3：用于 OSS/CDN 文档发布，需支持 `venv` 与 pip。

完整源码构建会同时编译控制台，因此需要 Node.js 和 npm。单独构建 Java 引擎时无需前端环境。首次构建需要下载 Gradle 及 Maven/npm 依赖。

Java 依赖版本以 [gradle.properties](../gradle.properties) 为准，示例依赖以 [example/pom.xml](../example/pom.xml) 为准。当前配置含 Hasor、dbVisitor 的 SNAPSHOT 依赖；本地仓库尚无对应版本时，先在它们的仓库执行 `./build.sh install`。Hasor 示例还需要同版本的 `hasor-boot-maven-plugin` 和 `hasor-boot-loader`。

## 编译与测试

### 构建项目

```bash
# 编译并打包
./build.sh package

# 编译、执行 Java 测试并打包
./build.sh package test

# 安装到 Maven 本地仓库，供独立示例和应用引用
./build.sh install
```

脚本默认跳过 Java 测试，传入 `test` 后执行。构建产物位于各模块的 `build/libs`，制品版本取自 `gradle.properties`。

控制台模块会自动执行 npm 安装和前端构建，将资源打包到 JAR 的 `META-INF/dataway-ui` 中。只修改前端时可限定构建范围：

```bash
./build.sh package web
./build.sh install web
```

`web` 对 `package`、`install`、`deploy` 均生效；追加 `test` 可执行控制台检查。单独编译 Java 引擎可直接使用 Gradle：

```bash
./gradlew :dataql-engine:build :dataql-sqlproc:build
```

### 按模块测试

```bash
# Dataway 核心：测试及覆盖率报告
./gradlew :dataway-embedded:check

# 三大框架整合
./gradlew :dataway-spring:check :dataway-solon:check :dataway-hasor:check

# 元数据存储
./gradlew :dataway-meta-jdbc:check :dataway-meta-nacos:check
```

公共 JaCoCo 配置位于根 `build.gradle`。`test` 运行单测，`check` 同时执行测试和覆盖率任务。报告位于对应模块：

- 测试结果：`build/reports/tests/test/index.html`。
- 行覆盖率：`build/reports/jacoco/test/`，其中 `html/index.html` 为页面，`jacocoTestReport.xml` 为 XML 报告。

三大整合模块的测试包含真实 Web 服务与 H2 数据库场景。Nacos 存储模块的真实服务测试通过环境变量启用：

```bash
DATAWAY_NACOS_SERVER=127.0.0.1:8848 ./gradlew :dataway-meta-nacos:test
```

不提供该变量时跳过这组真实 Nacos 测试。自动启动本地 Nacos 的方式见下文示例。

## 运行示例

示例使用 Maven 坐标依赖已安装的 Dataway JAR，与源码的 Gradle 构建独立。修改框架源码后，重新执行 `./build.sh install`，再刷新示例的 Maven 依赖。

- [Spring 示例](../example/dataway-spring-example)：使用 Spring Boot、H2 和 JDBC 元数据存储，提供登录与 Swagger UI。
- [Solon 示例](../example/dataway-solon-example)：使用 Solon、H2 和 JDBC 元数据存储，提供登录与 Swagger UI。
- [Hasor 示例](../example/dataway-hasor-example)：使用 Hasor Boot、dbVisitor、H2 和 JDBC 元数据存储，提供登录与 Swagger UI。
- [Nacos 示例](../example/dataway-spring-nacos-example)：使用 Spring Boot，以 Nacos 保存元数据，以 H2 保存业务数据。
- [DataQL 语言样例](../example/dataql-blog-example)：包含八个数据处理样例及预期结果。

### 启动框架示例

以 Spring 为例：

```bash
./build.sh install
mvn -f example/dataway-spring-example/pom.xml clean package
java -jar example/dataway-spring-example/target/dataway-spring-example.jar
```

其他框架使用相同方式构建对应 POM，运行其 `target/<模块名>.jar`。也可执行 `mvn -f example/pom.xml clean package` 构建全部示例，其中会运行 Nacos 示例测试。

一次启动一个应用，浏览器打开 `http://127.0.0.1:8080/`。账号为 `api`、`reader`、`admin`，分别对应 API 访问、控制台只读和开发管理身份；密码均为 `example-password`。账号与启用状态从各示例的 `database/users.sql` 初始化，`disabled` 账号不能登录。

示例会初始化两组业务数据源 `ds1`、`ds2`，并发布 SQL 查询、JSON、表单、文件上传及下载接口。登录后可从首页进入：

- `/admin/`：管理控制台，编辑、调试和发布接口。
- `/swagger/index.html`：Swagger UI，展开接口后使用 **Try it out → Execute** 发起请求。
- `/docs/openapi.json`、`/docs/swagger.json`：生成的接口文档。

首页还提供 Structure、Raw Value、CSV、Text、VerifyCode 五种结果处理器的调用示例。上传下载示例返回上传的文件；验证码示例将文本转换为 PNG，实际验证码的生成和校验由应用实现。

配置分别位于 Spring 的 `application.yml`、Solon 的 `app.properties`、Hasor 的 `hconfig.xml`。完整接入方式见[框架整合](../document/docs/dataway/integration/buildtools.md)。

IDEA 可独立导入 `example/pom.xml`。先执行 Maven `compile` 准备 Swagger UI 资源，再运行对应的 `ExampleApplication`，产物统一位于 `target`。若同一工作区还导入 Gradle 源码，且 IDEA 将 Maven JAR 替换成了源码模块，可关闭 Registry 中的 `external.system.substitute.library.dependencies` 后重新导入 Maven。

### 本地 Nacos 示例

测试辅助程序会启动真实的 Nacos Java 进程，无需 Docker。首次运行下载并校验 Nacos 3.1.2 发行包，缓存于示例的 `target/nacos`；每次使用独立数据目录和空闲端口，结束时停止服务。

```bash
# 运行包含登录、发布、SQL、上传及文档访问的 HTTP 测试
mvn -f example/dataway-spring-nacos-example/pom.xml test

# 同时启动 Nacos 和示例应用，供浏览器访问
mvn -f example/dataway-spring-nacos-example/pom.xml spring-boot:test-run
```

离线环境可通过 `-Dnacos.test.archive=/absolute/path/nacos-server-3.1.2.zip` 指定发行包。`mvn clean` 会删除默认缓存，日志保留在 `target/nacos/run-*/logs`。

IDEA 中先执行 `test-compile`，再运行测试目录中的 `TestExampleApplication`，工作目录设为该示例模块。普通 `ExampleApplication` 和打包后的 JAR 使用 `application.yml` 中 `example.nacos` 配置的外部 Nacos 服务。

### 语言样例与 HTTP 测试

语言样例的 `src/main/resources/cases` 包含脚本 `query.dql`、输入 `parameters.json` 和预期结果 `expected.json`：

```bash
mvn -f example/dataql-blog-example/pom.xml test
mvn -f example/dataql-blog-example/pom.xml compile exec:java -Dexec.args=build-tree
```

三大框架示例中的 `src/main/resources/blog` 保存博客配套脚本、参数和选项，启动时注册为 `/api/blog/*` 接口。可一起运行三套示例的真实 HTTP 测试：

```bash
mvn -f example/pom.xml -pl dataway-spring-example,dataway-solon-example,dataway-hasor-example test
```

## 控制台开发

进入 `dataway/embedded-web`：

```bash
cd dataway/embedded-web
npm ci
npm run dev:mock
```

打开 `http://127.0.0.1:8888/admin/`，使用本地模拟数据调试界面。连接实际后端时改为执行 `npm run dev:proxy`；默认后端为 `http://localhost:8080`，代理 `/admin/api` 和 `/api`。

可在同目录 `.env.proxy.local` 覆盖配置，修改后重启开发服务：

```properties
DATAWAY_DEV_TARGET=http://localhost:8080
DATAWAY_DEV_ADMIN_PREFIX=/admin/api
DATAWAY_DEV_API_PREFIX=/api
```

应用有 context path 时，将其写入两个前缀配置，`DATAWAY_DEV_TARGET` 只填写协议、主机和端口。后端身份认证仍由宿主应用提供。

```bash
# 在 dataway/embedded-web 中执行
npm run check
npx playwright install chromium
npm run test:browser
npm run build
```

`check` 执行 ESLint 与 Node 测试，`test:browser` 使用 Playwright 验证页面交互，`build` 输出 `dist`。用于 Java 应用时，通过根目录 `./build.sh install web` 重新生成并安装资源 JAR。

## 文档开发

文档使用 Docusaurus，与 Java 构建独立。在 `document` 目录执行：

```bash
cd document
npm ci
npm run start
```

中文预览使用 `npm run start`，英文使用 `npm run start -- --locale en`，一次启动一个语言。构建与检查：

```bash
# 在 document 中执行
npm run build
npm run serve
```

`build` 同时构建两种语言，输出 `build/` 和 `build/en/`；`serve` 用于检查构建结果，包括搜索、旧地址跳转和 `llms.txt`。需要清理缓存时执行 `npm run clear`。

### 内容与资源

`document` 目录按内容和站点资源组织：

- `docs/dataway`：Dataway 使用、独立引擎接入和 Java 扩展。
- `docs/dataql`：DataQL 语言、SQL 用法和函数库。
- `docs/releases`：正式版本与开发版本记录。
- `blog`：实践文章与配套样例。
- `i18n/en`：英文文档、博客和界面翻译。
- `src`：首页、样式与主题组件。
- `static/img`、`static/files`：文档图片与下载附件，中英文共用。
- `plugins`：文档变量、博客专栏、跳转与 AI 索引。

导航在 `sidebars.js` 中维护，页面标题、菜单名称与编号保持一致。中文与英文文档使用相同路径和 ID，英文正文位于 `i18n/en/docusaurus-plugin-content-docs/current`；自定义界面文案位于 `i18n/en/code.json`。

文档图片使用 `/img/...`，附件使用 `/files/...`。调整章节时保留已发布的页面地址；确需迁移时，在 `docusaurus.config.js` 的 `redirects` 中配置旧地址到新地址的跳转。`plugins/redirects.js` 自动生成跳转页，`static` 中不再维护手写 HTML。变更插件或站点配置后重启预览服务。

`plugins/projectVars.js` 维护 `docsVersion`、`developmentVersion`、`lastReleaseVer` 和 `lastReleaseTime`。依赖示例使用 `@project.docsVersion@`，历史文章和版本记录保留当时的版本。文档版本与 Java 制品版本分别维护。

### 博客与版本记录

博客文件名使用实际发布日期，`slug` 保持稳定。在导语后加入 `<!-- truncate -->`；正文发生实质修订时再填写 `updated`，排版和翻译不改变发布日期。

```yaml
---
slug: api-example
title: "使用 Dataway 发布查询接口"
description: "从 SQL 查询到 HTTP 接口的完整示例。"
authors: [zyc]
tags: [DataQL, Dataway]
topics: [dataway]
language: zh-cn
---
```

`blog/authors.yml`、`tags.yml`、`topics.yml` 分别管理作者、标签与专栏。英文博客位于 `i18n/en/docusaurus-plugin-content-blog`，使用相同的文件名日期、slug 和配置 ID。随文源码与配图放在 `blog/assets/<文章文件名（不含扩展名）>/`，两种语言共用。

版本记录按依赖示例、更新内容和必要的升级说明组织。系列的 `position`、版本页的 `sidebar_position` 均从最早版本的 `999` 向下递减，使较新版本排在前面；版本概览固定为 `0`，编号同步英文。

### AI 索引

构建时由 `plugins/llms.js` 生成 `build/llms.txt` 和 `build/en/llms.txt`，列出语言、框架、扩展和博客的阅读入口。项目说明取自两种语言 `dataql/overview.md` 中的 `llms:start` / `llms:end` 标记。维护概览时保留这一对标记。

## 发布

### Java 制品

日常开发使用 `./build.sh install` 安装到本地仓库。发布到 Maven Central 使用 `./build.sh deploy`，凭据配置在 `~/.gradle/gradle.properties`：

```properties
maven.central.username=
maven.central.password=
maven.central.signing_key=
maven.central.signing_password=
maven.central.publishing_type=USER_MANAGED
```

`deploy` 要求正式版本，执行打包、本地安装、签名并上传制品；`./build.sh deploy web` 只处理控制台资源模块。

`./build.sh release test` 构建后创建正式版本提交与 `v主版本.次版本.修订号` 附注 Tag，再提交下一开发版本；Tag 说明为 `Release v<版本号>`。需要同时上传制品时使用 `./build.sh release deploy test`。发布脚本要求工作区干净，并提示确认版本，Git 提交和 Tag 需要自行推送。

### 文档站点

将 [oss-config.sample.json](../document/oss-config.sample.json) 中的 `dataql` 项填入本机 `~/.hasor-docs-deploy.json`，配置凭据、Bucket、前缀和 CDN 地址，然后执行：

```bash
cd document
./deploy.sh
```

脚本创建 `.deploy-venv`、安装 Python 依赖，依次构建、上传到 OSS 并提交 CDN 刷新。可通过 `HASOR_DOCS_DEPLOY_CONFIG` 指定配置文件路径。上传覆盖同名文件，远端旧文件不会自动删除；凭据不纳入仓库。

`npm run deploy` 是 Docusaurus 自带的部署入口，本站 OSS/CDN 发布使用 `deploy.sh`。

## 代码与文档约定

- Java 数据类型使用普通类或 POJO；未经明确要求不使用 `record`。
- `if`、`for`、`while` 等控制结构使用完整花括号；类名通过 import 引入，同名冲突除外。
- 源码保留统一的 Apache 2.0 文件头，代码注释使用英文。
- 导入本目录的 [IDEA 格式配置](idea-code-style.xml)或 [Eclipse 格式配置](eclipse-code-style.xml)，保持现有排版。
- 按功能场景组织测试，验证实际行为；主代码不为测试增加专用入口。
- 文档使用简洁的陈述句，示例与源码保持一致，界面截图使用统一分辨率。公开配置或接口变更时，同步更新文档、示例和测试。
- 仓库只在根目录与 `community` 保留 README；使用说明进入 `document`，开发维护说明集中在本文。

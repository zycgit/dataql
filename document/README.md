
# DataQL 文档工程

本目录与 Hasor 的 `document`、dbVisitor 的 `dbvisitor-doc` 使用相同的 Docusaurus 3 工程方式。Java 构建与文档构建相互独立，文档只需要 Node.js 20+ 和 npm；`.nvmrc` 使用 Node.js 22。

## 安装、预览与构建

```bash
npm ci
npm run start
npm run start -- --locale en
npm run build
npm run serve
```

两个 `start` 命令分别预览中文和英文，一次启动一个语言。`build` 构建全部语言，中文输出到 `build/`，英文输出到 `build/en/`。生产构建包含本地搜索索引和 `llms.txt`，用 `serve` 检查它们。`npm run clear` 清理 Docusaurus 缓存。

提交 `package.json` 和 `package-lock.json`，使用 `npm ci` 复现依赖。无需另行安装 nodejieba。Docusaurus 升级规则见[官方迁移文档](https://docusaurus.io/docs/migration/v3)。

## 目录与导航

| 目录 / 文件 | 用途 |
| --- | --- |
| `docs/dataql` | 语言入门、语法、数据转换、执行选项、SQL 与函数库 |
| `docs/dataway/intro`、`integration`、`principles` | Dataway 介绍、快速开始、三大框架整合与工作原理 |
| `docs/dataway/capabilities` | 控制台编辑、调试、发布，以及 HTTP 与程序化调用 |
| `docs/dataway/metadata`、`authorization`、`configuration` | 元数据、身份鉴权、框架与核心配置 |
| `docs/dataway/dataql-engine`、`engine` | 独立引擎接入与 Java 扩展 |
| `docs/releases` | 正式版本记录与独立标注的开发版本 |
| `blog` | 实践文章、脚本样例与 Dataway 迁移指南 |
| `i18n/en` | 英文文档、导航与界面翻译 |
| `src`、`static` | 首页、样式、图片和下载附件 |
| `plugins` | 版本变量、AI 索引与统计插件 |

`sidebars.js` 显式定义 DataQL 和 Dataway 的导航，版本记录按目录生成导航。新增页面加入对应侧栏，菜单与页面标题保持一致，编号同步英文。

图片统一放在 `static/img`，下载附件放在 `static/files`，中英文页面共用资源，通过 `/img/...`、`/files/...` 引用。

旧地址的跳转规则统一配置在 `docusaurus.config.js` 的 `redirects` 中。`plugins/redirects.js` 为开发预览和静态构建生成同名 HTML 跳转页，保留外部链接、查询参数和锚点；`static` 无需保留手写跳转页。

版本记录参照 dbVisitor：概览页说明版本规则，并列出当前与历史版本；版本页按依赖示例、更新内容和必要的升级说明组织。系列的 `position` 从最早系列的 `999` 逐次递减，各系列内的 `sidebar_position` 从最早版本的 `999` 逐次递减，使新版本排在前面；“版本说明”固定为 `0`。中英文使用相同编号，历史记录保留当时的版本、日期、依赖坐标和升级脚本。

正式版本使用 `v主版本.次版本.修订号` 附注标签，例如 `v5.0.0`，标签指向正式版本提交，说明为 `Release v5.0.0`。在仓库根目录执行 `./build.sh release test`，脚本检查工作区、版本格式及标签重名，构建成功后创建发布提交和标签，再提交下一个 `-SNAPSHOT` 版本。`./build.sh release deploy test` 同时执行制品发布；`package`、`install`、`deploy` 单独执行时不创建标签。脚本不推送 Git 提交或标签，已有正式标签不得覆盖。

DataQL 按“语言入门 → 语言基础 → 流程与函数 → 数据访问与转换 → 执行选项 → SQL 执行器 → 内置函数库”组织。各章概览提供用途和本章指引，详细规则只在对应参考页维护。SQL 与函数库分别作为独立模块；Java 引擎接入、宿主配置和扩展实现放在 Dataway 的引擎章节，通过链接关联。菜单分组由侧栏决定，已有参考页保持原 URL。

Dataway 的章节顺序为：介绍、快速开始、框架整合、工作原理、核心能力、元数据存储、身份鉴权、DataQL 引擎、引擎扩展、框架配置。

Dataway 当前指南以 `dataway/embedded`、各整合模块主代码和根目录 `example` 中的可运行应用 为依据。按“用途、最小配置、使用结果、必要边界”编写，避免重复开发过程。框架整合页按依赖、配置、数据源、事务、示例与配置项组织，Maven/Gradle 和等价配置格式使用 Tabs。公共配置与字段映射以专门章节为准，整合页同步列出入口配置。修改公开类型或配置时，同时检查文档中的调用示例。

old-hasor 的接口差异、数据接管和注意事项统一放在[迁移博客](blog/2026-10-04-dataway-migration.md)，不再保留“老文档”章节。

源码中的泛型、花括号和片段语法应放入行内代码或代码块，避免被 MDX 3 解析为 JSX。失效的文档链接会使构建失败。

## 文档版本变量

`plugins/projectVars.js` 统一维护 `docsVersion`、`developmentVersion`、`lastReleaseVer`、`lastReleaseTime`。正文、行内代码、代码块、链接及 front matter 都支持 `@project.docsVersion@` 等占位符；拼错变量名会导致构建失败。

通用依赖示例使用 `docsVersion`；旧版示例、历史发布日期和第三方依赖版本保持固定。此配置不修改 `gradle.properties`，也不表示 SNAPSHOT 已发布到 Maven Central。本地开发中修改变量后需重启预览。

## 英文文档

中文源文档位于 `docs/`，英文正文位于 `i18n/en/docusaurus-plugin-content-docs/current/`，两者保持相同路径和文档 ID。页面标题、导航文字和代码示例分别维护；缺少英文文件时 Docusaurus 使用中文源文档，不将回退内容当作已完成翻译。

`i18n/en/code.json` 只维护本站自定义界面文案，通用主题文案使用 Docusaurus 内置英文。导航和目录标签分别位于 `docusaurus-theme-classic/navbar.json`、`docusaurus-plugin-content-docs/current.json`。

## 博客页面与维护

博客沿用 dbVisitor 的浏览方式，顶部“博客”进入 `/blog/archive`。页面类型如下：

| 地址 | 页面 |
| --- | --- |
| `/blog/archive` | 全部文章，按年份分组、发布日期倒序排列 |
| `/blog` | 最新文章，每页 10 篇，侧栏显示最近 25 篇 |
| `/blog/tags` | 专栏卡片和标签索引 |
| `/blog/topics/<id>` | 专栏的全部文章 |
| `/blog/tags/<tag>` | 指定标签的文章列表 |
| `/blog/authors`、`/blog/authors/<id>` | 作者索引及作者文章列表 |
| `/blog/<slug>` | 文章详情，含返回入口、作者、日期、阅读时长和目录 |

中文文章放在 `blog/`，英文文章放在 `i18n/en/docusaurus-plugin-content-blog/`，文件名日期及 `slug` 保持一致。发布时间取文件名，不重复填写顶层 `date`。

迁移文章从原文发布平台核实日期，文件名保留该日期；内容按新版本改写时，用 `updated` 记录改写日期。原文没有日期时不将文档导入时间当作发布时间。调整日期保持 `slug` 不变，文章地址保持稳定。

元数据按下面的顺序维护：

```yaml
---
slug: whydataway
title: "Dataway 接口配置实践"
description: "将接口配置、调试和发布集成到现有应用。"
authors: [zyc]
tags: [DataQL, Dataway, SpringBoot]
topics: [dataway]
language: zh-cn
---
```

在导语后加入 `<!-- truncate -->`，控制“最新”页摘要和“阅读全文”入口。文章正式发布后发生实质修订时，才在元数据最后设置 `updated: YYYY-MM-DD`；纯排版、翻译或构建不改变日期。详情页在更新日期与发布日期不同时显示两者，归档和专栏仍按原始发布时间排序。

`blog/topics.yml` 定义专栏名称、描述及展示顺序，文章通过 `topics` 声明归属。可加入多个专栏；空专栏不显示，未知专栏 ID 会使构建失败。`blog/tags.yml` 定义标签，专栏和标签相互独立。作者在 `blog/authors.yml` 中配置，`page: true` 启用作者文章页。这三份配置的英文版本放在英文博客目录中，使用相同 ID。

文章配图及附件放在 `blog/assets/<文章文件名（不含扩展名）>/`，该目录不参与文章扫描。中英文共用资源，中文使用相对路径，英文通过 `../../../blog/assets/` 引用。页面布局位于 `src/theme/` 和 `src/components/Blog*`，专栏由 `plugins/blog-topics.js` 生成。修改后执行 `npm run build` 检查中英文站点。

## AI 文档索引

`npm run build` 使用 `@signalwire/docusaurus-plugin-llms-txt` 收集实际页面，再由 `plugins/llms.js` 生成 `build/llms.txt` 与 `build/en/llms.txt`。网页通过 `rel="describedby"` 链接到对应语言索引。

索引介绍 DataQL 的语言能力，提供语法、SQL、函数、Hint、独立引擎和 Dataway 的阅读入口，再按语言与框架列出文档；旧版、平台设计、版本记录及博客列入可选参考。项目说明来自两种语言 `dataql/overview.md` 中唯一一对 `llms:start` / `llms:end` 标记。

这里只生成文档发现入口，不生成 Dokka、Javadoc、Java 符号目录、`llms-full.txt` 或运行时工具。读者和 AI 需要先核对项目依赖版本，再查阅对应接口和示例。

## OSS / CDN 发布

部署脚本沿用 Hasor、dbVisitor 的配置和流程，`deploy_site.py` 固定选择 `dataql`，不会根据当前目录猜测站点。

1. 将 `oss-config.sample.json` 的 `dataql` 项合并到 `~/.hasor-docs-deploy.json`，填写自己的凭据、Bucket、前缀与 CDN 地址。样例没有真实密钥。
2. 在本目录执行 `npm ci`。
3. 需要正式发布时执行 `./deploy.sh`。脚本在 `.deploy-venv` 安装 Python 依赖，依次构建、上传、刷新 CDN；失败后不继续后续步骤。

可用 `HASOR_DOCS_DEPLOY_CONFIG=/absolute/path/config.json ./deploy.sh` 指定配置。所有站点的 OSS 前缀与 CDN 目录均检查重叠；同一站点使用本机锁。构建和发布使用相同配置快照。

上传覆盖同名文件但不删除远端旧文件，不是原子部署；CDN 接受刷新请求不代表所有节点已经刷新。站点 `url`、`baseUrl` 与云端回源路径需由部署者保持一致。凭据只放在用户目录或显式指定的外部文件，不提交到仓库。

`npm run deploy` 是 Docusaurus 自带的部署命令，不是 OSS/CDN 发布入口。普通文档检查只需执行 `npm run build`。

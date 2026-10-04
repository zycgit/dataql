/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import React from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import Translate, {translate} from '@docusaurus/Translate';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import useBaseUrl from '@docusaurus/useBaseUrl';
import CodeBlock from '@theme/CodeBlock';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import IconExternalLink from '@theme/Icon/ExternalLink';
import styles from './index.module.css';

const personQuery = `hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
var query = @@selectSql(id)<%
    SELECT id AS "id", name AS "name", balance AS "balance"
    FROM example_people
    WHERE id = #{id}
%>;
return query(\${id});`;

const examples = {
    transform: {
        script: `var people = [
    {"name": "Alice", "age": 25},
    {"name": "Bob", "age": 30}
];

return people => [{
    "name",
    "nextAge": age + 1
}];`,
        result: `[
    {"name": "Alice", "nextAge": 26},
    {"name": "Bob", "nextAge": 31}
]`,
        link: '/docs/dataql/syntax/transform',
    },
    group: {
        script: `import 'net.hasor.dataql.host.function.basic.CollectionUdfSource'
    as collect;

var rows = [
    {"name": "Alice", "team": "dev"},
    {"name": "Bob", "team": "sales"},
    {"name": "Carol", "team": "dev"}
];
return collect.groupBy(rows, 'team');`,
        result: `{
    "dev": [
        {"name": "Alice", "team": "dev"},
        {"name": "Carol", "team": "dev"}
    ],
    "sales": [
        {"name": "Bob", "team": "sales"}
    ]
}`,
        link: '/docs/dataql/funx/collect',
    },
    sql: {
        script: `hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
hint FRAGMENT_SQL_OPEN_PACKAGE = "off"

var people = @@selectSql()<%
    SELECT id AS "id", name AS "name", balance AS "balance"
    FROM example_people
    ORDER BY id
%>;
return people();`,
        result: `[
    {"id": 1, "name": "Alice", "balance": 100},
    {"id": 2, "name": "Bob", "balance": 200}
]`,
        link: '/docs/dataql/sql/execute',
    },
};

function HomepageHero() {
    const {siteConfig} = useDocusaurusContext();
    return (
        <header className={styles.hero}>
            <div className={styles.container}>
                <div className={styles.heroGrid}>
                    <div className={styles.heroCopy}>
                        <div className={styles.heroEyebrow}>
                            <span>DATAWAY + DATAQL</span>
                            <Link to="/docs/releases/latest" className={styles.version}>v{siteConfig.customFields.project.docsVersion}</Link>
                        </div>
                        <h1>
                            <Translate id="homepage.hero.title">把数据，</Translate>
                            <span><Translate id="homepage.hero.accent">变成可用的 API。</Translate></span>
                        </h1>
                        <p className={styles.heroDescription}>
                            <Translate id="homepage.hero.description">Dataway 将 API 编辑、调试与发布内嵌到 Java 应用。DataQL 用脚本完成数据查询、转换与聚合。</Translate>
                        </p>
                        <div className={styles.actions}>
                            <Link className={styles.primaryButton} to="/docs/dataway/intro/quickstart">
                                <Translate id="homepage.startDataway">开始使用 Dataway</Translate>
                            </Link>
                            <Link className={styles.secondaryButton} to="/docs/dataql/overview">
                                <Translate id="homepage.exploreDataql">了解 DataQL 语言</Translate>
                            </Link>
                        </div>
                        <p className={styles.heroNote}>
                            <Translate id="homepage.hero.note">内嵌应用 · 可视化开发 · 开源免费</Translate>
                        </p>
                        <div className={styles.communityLinks}>
                            <span><Translate id="homepage.community.title">社区交流</Translate></span>
                            <a href="https://qm.qq.com/cgi-bin/qm/qr?k=o4Ue0lHqdr7oLq8ga0vvauXuw41nudbo&jump_from=webapi"
                                target="_blank" rel="noopener noreferrer">
                                <img src="https://img.shields.io/badge/QQ%E7%BE%A41-193943114-orange" height="20"
                                    alt={`${translate({id: 'homepage.community.qq1', message: 'QQ 群 1'})}: 193943114`}/>
                            </a>
                            <a href="https://qm.qq.com/cgi-bin/qm/qr?k=wMahYnxpVZPjrJp0ghQQLJmwM2Lmpmjl&jump_from=webapi"
                                target="_blank" rel="noopener noreferrer">
                                <img src="https://img.shields.io/badge/QQ%E7%BE%A42-641341864-orange" height="20"
                                    alt={`${translate({id: 'homepage.community.qq2', message: 'QQ 群 2'})}: 641341864`}/>
                            </a>
                        </div>
                    </div>
                    <div className={styles.heroExample}>
                        <div className={styles.exampleHeading}>
                            <span><Translate id="homepage.hero.example">一段脚本，一个接口</Translate></span>
                            <span className={styles.codeLabel}>DataQL + SQL</span>
                        </div>
                        <div className={styles.endpoint}>
                            <span className={styles.method}>POST</span>
                            <code>/api/person-query</code>
                            <span className={styles.requestParameter}>{'{"id": 1}'}</span>
                        </div>
                        <CodeBlock language="javascript">{personQuery}</CodeBlock>
                        <div className={styles.heroResult}>
                            <div className={styles.resultLabel}>
                                <span><Translate id="homepage.hero.result">响应数据 · value</Translate></span>
                                <span className={styles.contentType}>application/json</span>
                            </div>
                            <CodeBlock language="json">{'{"id": 1, "name": "Alice", "balance": 100}'}</CodeBlock>
                        </div>
                        <Link className={styles.exampleGuide} to="/docs/dataway/intro/quickstart">
                            <Translate id="homepage.hero.guide">查看这个 SQL 接口的完整配置与发布步骤</Translate>
                        </Link>
                    </div>
                </div>
                <div className={styles.integrationStrip}>
                    <p><Translate id="homepage.integration.strip">接入熟悉的 Java 框架</Translate></p>
                    <div className={styles.frameworkLinks}>
                        <Link to="/docs/dataway/integration/spring">Spring</Link>
                        <Link to="/docs/dataway/integration/solon">Solon</Link>
                        <Link to="/docs/dataway/integration/hasor">Hasor</Link>
                    </div>
                    <Link className={styles.textLink} to="/docs/dataway/integration">
                        <Translate id="homepage.integration.guide">查看整合指南</Translate>
                    </Link>
                </div>
            </div>
        </header>
    );
}

function ConsolePreview({image, description, link}) {
    const src = useBaseUrl(`/img/dataway/${image}`);
    return (
        <figure className={styles.consolePreview}>
            <a href={src} target="_blank" rel="noreferrer" className={styles.screenshotLink}>
                <img src={src} alt={description} width="1440" height="920" loading="lazy" decoding="async"/>
            </a>
            <figcaption>
                <span>{description}</span>
                <Link to={link}><Translate id="homepage.console.details">查看操作说明</Translate></Link>
                <a href={src} target="_blank" rel="noreferrer" className={styles.fullImage}>
                    <Translate id="homepage.console.expand">查看大图</Translate><IconExternalLink/>
                </a>
            </figcaption>
        </figure>
    );
}

function ConsoleSection() {
    return (
        <section className={clsx(styles.section, styles.container)} aria-labelledby="dataway-title">
            <div className={styles.sectionHeading}>
                <div>
                    <p className={styles.eyebrow}>01 / DATAWAY</p>
                    <h2 id="dataway-title"><Translate id="homepage.console.title">API 开发，有一个完整的工作台。</Translate></h2>
                    <p><Translate id="homepage.console.description">在浏览器中编写脚本、传入参数、查看结果，再将接口发布给调用方。草稿修改在再次发布后生效。</Translate></p>
                </div>
                <Link className={styles.textLink} to="/docs/dataway/intro/overview">
                    <Translate id="homepage.console.overview">认识 Dataway 框架</Translate>
                </Link>
            </div>
            <Tabs defaultValue="editor" className={styles.consoleTabList} lazy>
                <TabItem value="editor" label={translate({id: 'homepage.console.tab.edit', message: '01  编写与调试'})}>
                    <ConsolePreview image="quickstart-published.png"
                        description={translate({id: 'homepage.console.edit', message: '编辑 DataQL 或 SQL，传入请求参数，直接查看执行结果。'})}
                        link="/docs/dataway/intro/quickstart"/>
                </TabItem>
                <TabItem value="results" label={translate({id: 'homepage.console.tab.result', message: '02  选择响应格式'})}>
                    <ConsolePreview image="console-result-handlers.png"
                        description={translate({id: 'homepage.console.result', message: '选择结果处理器，预览 JSON、文本、表格和图像，或下载文件。'})}
                        link="/docs/dataway/capabilities/management#result-panel"/>
                </TabItem>
                <TabItem value="list" label={translate({id: 'homepage.console.tab.publish', message: '03  发布与管理'})}>
                    <ConsolePreview image="console-list.png"
                        description={translate({id: 'homepage.console.publish', message: '统一查看已发布的 API，在列表中发起调用，进入编辑页管理草稿与发布历史。'})}
                        link="/docs/dataway/capabilities/management"/>
                </TabItem>
            </Tabs>
        </section>
    );
}

function QueryExample({example, description}) {
    return (
        <div>
            <div className={styles.queryGrid}>
                <div className={styles.queryCode}>
                    <div className={styles.codeHeading}><span>DataQL</span><span><Translate id="homepage.query.script">查询脚本</Translate></span></div>
                    <CodeBlock language="javascript">{example.script}</CodeBlock>
                </div>
                <div className={styles.queryResult}>
                    <div className={styles.codeHeading}><span>JSON</span><span><Translate id="homepage.query.result">示例结果</Translate></span></div>
                    <CodeBlock language="json">{example.result}</CodeBlock>
                </div>
            </div>
            <div className={styles.queryCaption}>
                <p>{description}</p>
                <Link to={example.link}><Translate id="homepage.query.syntax">查看完整用法</Translate></Link>
            </div>
        </div>
    );
}

function LanguageSection() {
    return (
        <section className={styles.languageSection} aria-labelledby="dataql-title">
            <div className={styles.container}>
                <div className={styles.sectionHeading}>
                    <div>
                        <p className={styles.eyebrow}>02 / DATAQL</p>
                        <h2 id="dataql-title"><Translate id="homepage.language.title">让数据，成为你需要的样子。</Translate></h2>
                        <p><Translate id="homepage.language.description">用表达式、函数和结构转换组织数据。既能在 Dataway 中开发 API，也能在 Java 应用中独立运行。</Translate></p>
                    </div>
                    <Link className={styles.textLink} to="/docs/dataql/tutorial/first-query">
                        <Translate id="homepage.language.guide">从第一个脚本开始</Translate>
                    </Link>
                </div>
                <Tabs defaultValue="transform" lazy>
                    <TabItem value="transform" label={translate({id: 'homepage.query.tab.transform', message: '结构转换'})}>
                        <QueryExample example={examples.transform}
                            description={translate({id: 'homepage.query.transform', message: '选择字段、计算新值，一次转换得到所需的数据结构。'})}/>
                    </TabItem>
                    <TabItem value="group" label={translate({id: 'homepage.query.tab.group', message: '分组聚合'})}>
                        <QueryExample example={examples.group}
                            description={translate({id: 'homepage.query.group', message: '导入内置函数，按字段分组，组合为新的结果。'})}/>
                    </TabItem>
                    <TabItem value="sql" label={translate({id: 'homepage.query.tab.sql', message: 'SQL 查询'})}>
                        <QueryExample example={examples.sql}
                            description={translate({id: 'homepage.query.sql', message: '通过 SQL 执行器访问数据库。此例使用示例工程中已配置的 ds1 数据源。'})}/>
                    </TabItem>
                </Tabs>
                <div className={styles.languageLinks}>
                    <Link to="/docs/dataway/dataql-engine"><Translate id="homepage.language.engine">在 Java 中独立使用引擎</Translate></Link>
                    <Link to="/docs/dataway/engine/functions"><Translate id="homepage.language.udf">把应用方法扩展为函数</Translate></Link>
                    <Link to="/docs/dataway/dataql-engine/sql"><Translate id="homepage.language.sql">接入 SQL 执行器</Translate></Link>
                </div>
            </div>
        </section>
    );
}

function ApplicationSection() {
    const features = [
        {
            title: translate({id: 'homepage.feature.sources.title', message: '连接多个数据源'}),
            description: translate({id: 'homepage.feature.sources.description', message: '按名称访问不同数据库，在脚本中组织查询，并接入应用的事务管理。'}),
            link: '/docs/dataway/capabilities/datasources',
            label: 'SQL · Transaction',
        },
        {
            title: translate({id: 'homepage.feature.auth.title', message: '沿用应用的身份认证'}),
            description: translate({id: 'homepage.feature.auth.description', message: '从已有登录体系获取用户身份，为 API 调用和控制台操作设置权限。'}),
            link: '/docs/dataway/authorization',
            label: 'Identity · Permission',
        },
        {
            title: translate({id: 'homepage.feature.docs.title', message: '发布标准接口文档'}),
            description: translate({id: 'homepage.feature.docs.description', message: '为已发布的 API 生成 OpenAPI 和 Swagger 文档，配合 Swagger UI 展示与调用。'}),
            link: '/docs/dataway/capabilities/document',
            label: 'OpenAPI · Swagger',
        },
        {
            title: translate({id: 'homepage.feature.results.title', message: '按需输出结果'}),
            description: translate({id: 'homepage.feature.results.description', message: '输出 JSON、文本、CSV、图像和二进制文件，也可扩展自己的结果处理器。'}),
            link: '/docs/dataway/capabilities/result-handlers',
            label: 'JSON · CSV · Binary',
        },
    ];
    return (
        <section className={clsx(styles.section, styles.container)} aria-labelledby="application-title">
            <div className={styles.applicationPanel}>
                <div className={styles.applicationIntro}>
                    <p className={styles.eyebrow}>03 / IN YOUR APPLICATION</p>
                    <h2 id="application-title"><Translate id="homepage.application.title">融入你的应用。</Translate></h2>
                    <p><Translate id="homepage.application.description">引入框架整合模块，将 API 开发能力内嵌到 Spring、Solon 或 Hasor 应用，复用已有的数据源与业务服务。</Translate></p>
                    <Link className={styles.lightButton} to="/docs/dataway/integration">
                        <Translate id="homepage.application.guide">选择你的框架</Translate>
                    </Link>
                    <div className={styles.storageNote}>
                        <span><Translate id="homepage.application.storageLabel">API 定义与发布记录</Translate></span>
                        <Link to="/docs/dataway/metadata"><Translate id="homepage.application.storage">JDBC / Nacos / 自定义存储</Translate></Link>
                    </div>
                </div>
                <div className={styles.featureGrid}>
                    {features.map(feature => (
                        <Link key={feature.link} className={styles.feature} to={feature.link}>
                            <span className={styles.featureLabel}>{feature.label}</span>
                            <h3>{feature.title}</h3>
                            <p>{feature.description}</p>
                        </Link>
                    ))}
                </div>
            </div>
        </section>
    );
}

function GetStartedSection() {
    return (
        <section className={clsx(styles.container, styles.getStarted)} aria-labelledby="get-started-title">
            <div>
                <p className={styles.eyebrow}><Translate id="homepage.start.eyebrow">开始构建</Translate></p>
                <h2 id="get-started-title"><Translate id="homepage.start.title">从第一个接口，到你的业务。</Translate></h2>
                <p><Translate id="homepage.start.description">跟随 Spring 示例发布一个 SQL 查询接口，或从一段 DataQL 脚本开始。</Translate></p>
            </div>
            <div className={styles.actions}>
                <Link className={styles.primaryButton} to="/docs/dataway/intro/quickstart"><Translate id="homepage.start.quickstart">发布第一个 API</Translate></Link>
                <Link className={styles.secondaryButton} to="/docs/dataql/tutorial/first-query"><Translate id="homepage.start.language">学习 DataQL</Translate></Link>
            </div>
        </section>
    );
}

export default function Home() {
    return (
        <Layout description={translate({id: 'homepage.description', message: '通过 Dataway 在 Java 应用中编辑、调试和发布 API，使用 DataQL 查询、转换与聚合数据。支持 Spring、Solon、Hasor、多数据源和 OpenAPI 文档。'})}>
            <main className={styles.home}>
                <HomepageHero/>
                <ConsoleSection/>
                <LanguageSection/>
                <ApplicationSection/>
                <GetStartedSection/>
                <div className={clsx(styles.container, styles.community)}>
                    <a href="https://www.apache.org/licenses/LICENSE-2.0.html" target="_blank" rel="noreferrer">Apache License 2.0</a>
                    <a href="https://gitee.com/zycgit/dataql/tree/dev/example" target="_blank" rel="noreferrer"><Translate id="homepage.community.examples">示例源码</Translate></a>
                    <a href="mailto:zyc@byshell.org"><Translate id="homepage.community.contact">联系作者</Translate></a>
                    <Link to="/blog"><Translate id="homepage.community.blog">阅读博客</Translate></Link>
                </div>
            </main>
        </Layout>
    );
}

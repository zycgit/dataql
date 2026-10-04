/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
// @ts-check
const {themes} = require('prism-react-renderer');
const analyticsPlugin = require('./plugins/analytics.js');
const projectVars = require('./plugins/projectVars.js');
const remarkProjectVars = require('./plugins/remark-project-vars.js');
const {GlobExcludeDefault} = require('@docusaurus/utils');

/** @type {import('@docusaurus/types').Config} */
const config = {
    title: 'DataQL - 数据查询语言',
    tagline: 'DataQL 数据查询语言',
    url: 'https://www.dataql.net',
    baseUrl: '/',
    onBrokenLinks: 'throw',
    markdown: {
        preprocessor: ({fileContent}) => remarkProjectVars.replaceVariables(fileContent, projectVars),
        parseFrontMatter: ({filePath, fileContent, defaultParseFrontMatter}) => defaultParseFrontMatter({
            filePath,
            fileContent: remarkProjectVars.replaceVariables(fileContent, projectVars),
        }),
        hooks: {
            onBrokenMarkdownLinks: 'throw',
        },
    },
    favicon: 'img/dataway.ico',
    organizationName: 'zycgit', // Usually your GitHub org/user name.
    projectName: 'dataql',   // Usually your repo name.
    customFields: {project: projectVars},

    i18n: {
        defaultLocale: 'zh-cn',
        locales: ['zh-cn', 'en'],
    },

    presets: [
        [
            'classic',
            /** @type {import('@docusaurus/preset-classic').Options} */
            ({
                docs: {
                    remarkPlugins: [[remarkProjectVars, projectVars]],
                    sidebarPath: require.resolve('./sidebars.js'),
                    editUrl: 'https://gitee.com/zycgit/dataql/blob/dev/document/',
                },
                blog: {
                    exclude: [...GlobExcludeDefault, '**/assets/**'],
                    remarkPlugins: [[remarkProjectVars, projectVars]],
                    showReadingTime: true,
                    blogSidebarCount: 25,
                    postsPerPage: 10,
                    feedOptions: {
                        type: ['rss', 'atom'],
                        xslt: true,
                    },
                    onInlineTags: 'warn',
                    onInlineAuthors: 'warn',
                    onUntruncatedBlogPosts: 'warn',
                    editUrl: 'https://gitee.com/zycgit/dataql/blob/dev/document/',
                },
                theme: {
                    customCss: require.resolve('./src/css/custom.css'),
                },
            }),
        ],
    ],

    themeConfig: /** @type {import('@docusaurus/preset-classic').ThemeConfig} */ {
        metadata: [
            {name: 'keywords', content: 'sql,dataway,hasor,dataql,开源,开源软件,java开源,开源项目,开源代码'},
            {name: 'description', content: 'DataQL 提供数据查询、转换和脚本扩展；Dataway 将接口编辑、调试、发布及 OpenAPI 文档内嵌到 Hasor、Solon 和 Spring 应用。'}
        ],
        colorMode: {
            disableSwitch: true,
        },
        navbar: {
            logo: {
                alt: 'Dataway / DataQL',
                src: 'img/dataway.svg',
                width: 32,
                height: 32,
            },
            items: [
                {
                    type: 'doc',
                    docId: 'dataway/intro/overview',
                    position: 'left',
                    label: 'Dataway 框架',
                },
                {
                    type: 'doc',
                    docId: 'dataql/overview',
                    position: 'left',
                    label: 'DataQL 语言',
                },
                {
                    type: 'doc',
                    docId: 'releases/latest',
                    position: 'left',
                    label: '版本记录',
                },
                {
                    to: '/blog/archive',
                    activeBasePath: '/blog',
                    position: 'left',
                    label: '博客',
                },
                {
                    position: 'right',
                    label: '码云',
                    href: 'https://gitee.com/zycgit/dataql'
                },
                {
                    position: 'right',
                    label: 'Github',
                    href: 'https://github.com/zycgit/dataql'
                },
                {
                    type: 'localeDropdown',
                    position: 'right',
                }
            ]
        },
        prism: {
            theme: themes.github,
            darkTheme: themes.dracula,
            additionalLanguages: ['java', 'sql', 'properties', 'bash']
        },
        footer: {
            style: 'dark',
            copyright: `Copyright © ${new Date().getFullYear()} DataQL. Built with Docusaurus.<br/>
<a target="_blank" href="http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=33011002016704">
<img src="/img/beian.png" style="display: inline-block;">浙公网安备 33011002016704号
</a>&nbsp;&nbsp;<a target="_blank" href="https://beian.miit.gov.cn/#/Integrated/index">浙ICP备18034797号-6</a>
<div id="analyticsDiv" style="display: inline-block;"></div>`,
        },
    },
    plugins: [
        analyticsPlugin,
        require.resolve('./plugins/blog-topics.js'),
        [
            require.resolve('./plugins/redirects.js'),
            {
                redirects: {
                    '/web/dataql/what_is_dataql.html': '/docs/dataql/overview',
                    '/web/dataway/about.html': '/docs/dataway/intro/overview',
                },
            },
        ],
        [
            require.resolve('./plugins/llms.js'),
            {
                siteTitle: 'DataQL and Dataway',
                overview: 'dataql/overview.md',
                descriptions: {
                    'zh-cn': `DataQL ${projectVars.docsVersion} 使用文档：查询语言、执行引擎、内嵌 Dataway 和框架集成。`,
                    en: `DataQL ${projectVars.docsVersion} documentation: query language, execution engine, embedded Dataway, and framework integration.`,
                },
                depth: 2,
                onRouteError: 'throw',
                content: {
                    enableMarkdownFiles: false,
                    enableLlmsFullTxt: false,
                    includeDocs: true,
                    includeBlog: true,
                    includePages: false,
                    includeGeneratedIndex: false,
                    excludeRoutes: ['**/tags{,/**}', '**/search', '**/404.html', '**/blog', '**/blog/{archive,authors,page,topics}{,/**}'],
                },
            },
        ],
    ],
    themes: [
        [
            require.resolve('@easyops-cn/docusaurus-search-local'),
            {
                hashed: true,
                language: ['en', 'zh'],
            },
        ],
    ],
};

module.exports = function createConfig() {
    if (process.env.DOCUSAURUS_CURRENT_LOCALE !== 'en') {
        return config;
    }
    const messages = require('./i18n/en/code.json');
    return {
        ...config,
        title: messages['site.title'].message,
        tagline: messages['site.tagline'].message,
        themeConfig: {
            ...config.themeConfig,
            metadata: config.themeConfig.metadata.map((entry) => ({
                ...entry,
                content: messages['site.' + entry.name]?.message ?? entry.content,
            })),
        },
    };
};

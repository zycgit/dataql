// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');
const analyticsPlugin = require('./plugins/analytics.js');

/** @type {import('@docusaurus/types').Config} */
const config = {
    title: 'DataQL - 数据查询语言',
    tagline: 'DataQL 数据查询语言',
    url: 'https://www.dataql.net',
    baseUrl: '/',
    onBrokenLinks: 'throw',
    onBrokenMarkdownLinks: 'warn',
    favicon: 'img/favicon.ico',
    organizationName: 'zycgit', // Usually your GitHub org/user name.
    projectName: 'dataql',   // Usually your repo name.

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
                    sidebarPath: require.resolve('./sidebars.js'),
                    editUrl: 'https://gitee.com/zycgit/dataql-doc/tree/master/',
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
            {name: 'description', content: 'DataQL（Data Query Language）是一种查询语言。旨在通过提供直观、灵活的语法来描述客户端应用程序的数据需求和交互。'}
        ],
        colorMode: {
            disableSwitch: true,
        },
        navbar: {
            logo: {
                alt: 'DataQL Logo',
                src: 'img/logo.svg',
            },
            items: [
                {
                    type: 'doc',
                    docId: 'dataql/overview',
                    position: 'left',
                    label: 'DataQL 语言',
                },
                {
                    type: 'doc',
                    docId: 'dataway/overview',
                    position: 'left',
                    label: 'Dataway 框架',
                },
                {
                    type: 'doc',
                    docId: 'integration/overview',
                    position: 'left',
                    label: '框架整合',
                },
                {
                    type: 'doc',
                    docId: 'releases/latest',
                    position: 'left',
                    label: '发布版本',
                },
                {
                    type: 'dropdown',
                    label: '源代码',
                    position: 'left',
                    items: [
                        {label: '码云',href: 'https://gitee.com/zycgit/dataql'},
                        {label: 'Github',href: 'https://github.com/zycgit/dataql'}
                    ]
                },
                {
                    position: 'right',
                    label: 'Hasor 框架',
                    href: 'https://www.hasor.net/'
                },
                {
                    position: 'right',
                    label: 'dbVisitor ORM',
                    href: 'https://www.dbvisitor.net/'
                },
                {
                    type: 'localeDropdown',
                    position: 'right',
                }
            ]
        },
        prism: {
            theme: lightCodeTheme,
            darkTheme: darkCodeTheme,
            additionalLanguages: ['java']
        },
        footer: {
            style: 'dark',
            copyright: `Copyright © ${new Date().getFullYear()} dbVisitor. Built with Docusaurus.<br/>
<a target="_blank" href="http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=33011002016704">
<img src="/img/beian.png" style="display: inline-block;">浙公网安备 33011002016704号
</a>&nbsp;&nbsp;<a target="_blank" href="https://beian.miit.gov.cn/#/Integrated/index">浙ICP备18034797号-6</a>
<div id="analyticsDiv" style="display: inline-block;"></div>`,
        },
    },
    plugins: [
        analyticsPlugin,
        [
            require.resolve("@cmfcmf/docusaurus-search-local"),
            {
                indexPages: true,
                // When applying `zh` in language, please install `nodejieba` in your project.
                language: ["en", "zh"],
                maxSearchResults: 8
            }
        ]
    ]
};

module.exports = config;

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
    engine: [
        {
            type: 'category', label: '1. 语言入门',
            link: {type: 'doc', id: 'dataql/overview'},
            items: ['dataql/tutorial/first-query', 'dataql/tutorial/structure'],
        },
        {
            type: 'category', label: '2. 语言基础',
            link: {type: 'doc', id: 'dataql/syntax/index'},
            items: ['dataql/syntax/lexical', 'dataql/syntax/types', 'dataql/syntax/numbers', 'dataql/syntax/setter', 'dataql/syntax/expression'],
        },
        {
            type: 'category', label: '3. 流程与函数',
            link: {type: 'doc', id: 'dataql/syntax/flow'},
            items: ['dataql/syntax/statements', 'dataql/syntax/function', 'dataql/syntax/imports', 'dataql/syntax/fragment'],
        },
        {
            type: 'category', label: '4. 数据访问与转换',
            link: {type: 'doc', id: 'dataql/syntax/data'},
            items: ['dataql/syntax/getter', 'dataql/syntax/valuescope', 'dataql/syntax/transform'],
        },
        {
            type: 'category', label: '5. 执行选项',
            link: {type: 'doc', id: 'dataql/hints/index'},
            items: ['dataql/hints/hint_core'],
        },
        {
            type: 'category', label: '6. SQL 执行器',
            link: {type: 'doc', id: 'dataql/sql/index'},
            items: ['dataql/sql/execute', 'dataql/sql/parameters', 'dataql/sql/rules', 'dataql/sql/MyBatis', 'dataql/sql/results', 'dataql/sql/dialect', 'dataql/sql/types', 'dataql/sql/transactions', 'dataql/sql/procedures', 'dataql/hints/hint_sql'],
        },
        {
            type: 'category', label: '7. 内置函数库',
            link: {type: 'doc', id: 'dataql/funx/index'},
            items: ['dataql/funx/string', 'dataql/funx/collect', 'dataql/funx/number', 'dataql/funx/datetime', 'dataql/funx/json', 'dataql/funx/convert', 'dataql/funx/codec', 'dataql/funx/state', 'dataql/funx/web', 'dataql/funx/transactions'],
        },
    ],
    dataway: [
        'dataway/intro/overview',
        'dataway/intro/quickstart',
        {
            type: 'category',
            label: '3. 框架整合',
            link: {type: 'doc', id: 'dataway/integration/buildtools'},
            items: ['dataway/integration/spring', 'dataway/integration/solon', 'dataway/integration/hasor'],
        },
        'dataway/principles/index',
        {
            type: 'category',
            label: '5. 核心能力',
            link: {type: 'doc', id: 'dataway/capabilities/index'},
            items: [
                {
                    type: 'category', label: '5.1 API 管理',
                    link: {type: 'doc', id: 'dataway/capabilities/api-management'},
                    items: ['dataway/capabilities/management', 'dataway/capabilities/programmatic'],
                },
                {
                    type: 'category', label: '5.2 API 发布',
                    link: {type: 'doc', id: 'dataway/capabilities/development/index'},
                    items: ['dataway/capabilities/development/script', 'dataway/capabilities/development/request',
                        'dataway/capabilities/development/response', 'dataway/capabilities/development/options',
                        'dataway/capabilities/development/java'],
                },
                {
                    type: 'category', label: '5.3 结果处理器',
                    link: {type: 'doc', id: 'dataway/capabilities/result-handlers'},
                    items: ['dataway/capabilities/result-handlers/structure', 'dataway/capabilities/result-handlers/raw',
                        'dataway/capabilities/result-handlers/csv', 'dataway/capabilities/result-handlers/text',
                        'dataway/capabilities/result-handlers/verify-code', 'dataway/capabilities/result-handlers/custom'],
                },
                'dataway/capabilities/document',
                'dataway/capabilities/datasources',
            ],
        },
        {
            type: 'category',
            label: '6. 元数据存储',
            link: {type: 'doc', id: 'dataway/metadata/index'},
            items: [
                'dataway/metadata/mapping', 'dataway/metadata/transactions',
                {
                    type: 'category', label: '6.3 提供者',
                    link: {type: 'doc', id: 'dataway/metadata/providers/index'},
                    items: ['dataway/metadata/providers/jdbc', 'dataway/metadata/providers/nacos', 'dataway/metadata/providers/custom'],
                },
            ],
        },
        {
            type: 'category',
            label: '7. 身份鉴权',
            link: {type: 'doc', id: 'dataway/authorization/index'},
            items: ['dataway/authorization/identity', 'dataway/authorization/permissions'],
        },
        {
            type: 'category',
            label: '8. DataQL 引擎',
            link: {type: 'doc', id: 'dataway/dataql-engine/index'},
            items: ['dataway/dataql-engine/execute', 'dataway/dataql-engine/model', 'dataway/dataql-engine/core',
                'dataway/dataql-engine/sql',
                'dataway/dataql-engine/jsr223', 'dataway/dataql-engine/instruction'],
        },
        {
            type: 'category',
            label: '9. 引擎扩展',
            link: {type: 'doc', id: 'dataway/engine/index'},
            items: ['dataway/engine/functions', 'dataway/engine/libraries', 'dataway/engine/imports',
                'dataway/engine/fragments', 'dataway/engine/finder', 'dataway/engine/scope',
                'dataway/engine/customizers', 'dataway/engine/sql-interceptors',
                'dataway/engine/sql-macros', 'dataway/engine/sql-rules', 'dataway/engine/sql-types',
                'dataway/engine/sql-dialects'],
        },
        {
            type: 'category',
            label: '10. 框架配置',
            link: {type: 'doc', id: 'dataway/configuration/index'},
            items: ['dataway/configuration/core',
                'dataway/configuration/admin-interceptors',
                'dataway/configuration/api-interceptors',
                'dataway/configuration/console'],
        },
    ],
    releases: [
        {
            type: 'autogenerated',
            dirName: 'releases',
        },
    ]
};

module.exports = sidebars;

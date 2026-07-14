import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/en/blog',
    component: ComponentCreator('/en/blog', '036'),
    exact: true
  },
  {
    path: '/en/blog/archive',
    component: ComponentCreator('/en/blog/archive', '366'),
    exact: true
  },
  {
    path: '/en/blog/tags',
    component: ComponentCreator('/en/blog/tags', '229'),
    exact: true
  },
  {
    path: '/en/blog/tags/data-ql',
    component: ComponentCreator('/en/blog/tags/data-ql', '7ff'),
    exact: true
  },
  {
    path: '/en/blog/tags/dataway',
    component: ComponentCreator('/en/blog/tags/dataway', '92c'),
    exact: true
  },
  {
    path: '/en/blog/tags/spring-boot',
    component: ComponentCreator('/en/blog/tags/spring-boot', '2c2'),
    exact: true
  },
  {
    path: '/en/blog/whydataway',
    component: ComponentCreator('/en/blog/whydataway', '80b'),
    exact: true
  },
  {
    path: '/en/markdown-page',
    component: ComponentCreator('/en/markdown-page', '0bd'),
    exact: true
  },
  {
    path: '/en/docs',
    component: ComponentCreator('/en/docs', '0af'),
    routes: [
      {
        path: '/en/docs/category/v32x',
        component: ComponentCreator('/en/docs/category/v32x', 'b5f'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/category/v40x',
        component: ComponentCreator('/en/docs/category/v40x', '561'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/category/v41x',
        component: ComponentCreator('/en/docs/category/v41x', '307'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/category/v42x',
        component: ComponentCreator('/en/docs/category/v42x', '437'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/dataql/develop/execute',
        component: ComponentCreator('/en/docs/dataql/develop/execute', '2e4'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/develop/fragment',
        component: ComponentCreator('/en/docs/dataql/develop/fragment', 'd3a'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/develop/function',
        component: ComponentCreator('/en/docs/dataql/develop/function', 'e8b'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/develop/globalvar',
        component: ComponentCreator('/en/docs/dataql/develop/globalvar', 'd62'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/develop/instruction',
        component: ComponentCreator('/en/docs/dataql/develop/instruction', '7d4'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/example/build_tree',
        component: ComponentCreator('/en/docs/dataql/example/build_tree', '848'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/example/dataset_ranking',
        component: ComponentCreator('/en/docs/dataql/example/dataset_ranking', 'a00'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/example/dim_reduction',
        component: ComponentCreator('/en/docs/dataql/example/dim_reduction', '011'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/example/group_by',
        component: ComponentCreator('/en/docs/dataql/example/group_by', 'c0b'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/example/left_join',
        component: ComponentCreator('/en/docs/dataql/example/left_join', '175'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/example/like_for',
        component: ComponentCreator('/en/docs/dataql/example/like_for', 'b40'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/example/row_to_col',
        component: ComponentCreator('/en/docs/dataql/example/row_to_col', '18b'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/example/tree_to_tree',
        component: ComponentCreator('/en/docs/dataql/example/tree_to_tree', 'e23'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/funx/collect',
        component: ComponentCreator('/en/docs/dataql/funx/collect', 'c89'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/funx/convert',
        component: ComponentCreator('/en/docs/dataql/funx/convert', '698'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/funx/datetime',
        component: ComponentCreator('/en/docs/dataql/funx/datetime', 'c22'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/funx/json',
        component: ComponentCreator('/en/docs/dataql/funx/json', '430'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/funx/state',
        component: ComponentCreator('/en/docs/dataql/funx/state', '0e7'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/funx/string',
        component: ComponentCreator('/en/docs/dataql/funx/string', 'b5f'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/funx/web',
        component: ComponentCreator('/en/docs/dataql/funx/web', '2ea'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/hints/hint_core',
        component: ComponentCreator('/en/docs/dataql/hints/hint_core', 'dd6'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/hints/hint_sql',
        component: ComponentCreator('/en/docs/dataql/hints/hint_sql', '38a'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/model/about',
        component: ComponentCreator('/en/docs/dataql/model/about', '9f0'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/model/list',
        component: ComponentCreator('/en/docs/dataql/model/list', '68d'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/model/object',
        component: ComponentCreator('/en/docs/dataql/model/object', '73e'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/model/udf',
        component: ComponentCreator('/en/docs/dataql/model/udf', '75a'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/model/value',
        component: ComponentCreator('/en/docs/dataql/model/value', '93d'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/overview',
        component: ComponentCreator('/en/docs/dataql/overview', 'cea'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/sql/about',
        component: ComponentCreator('/en/docs/dataql/sql/about', 'd35'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/sql/dialect',
        component: ComponentCreator('/en/docs/dataql/sql/dialect', 'f3a'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/sql/execute',
        component: ComponentCreator('/en/docs/dataql/sql/execute', 'f8a'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/sql/MyBatis',
        component: ComponentCreator('/en/docs/dataql/sql/MyBatis', '143'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/expression',
        component: ComponentCreator('/en/docs/dataql/syntax/expression', 'd3e'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/fragment',
        component: ComponentCreator('/en/docs/dataql/syntax/fragment', '508'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/function',
        component: ComponentCreator('/en/docs/dataql/syntax/function', '16c'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/getter',
        component: ComponentCreator('/en/docs/dataql/syntax/getter', 'b15'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/lexical',
        component: ComponentCreator('/en/docs/dataql/syntax/lexical', '0f7'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/numbers',
        component: ComponentCreator('/en/docs/dataql/syntax/numbers', '81c'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/setter',
        component: ComponentCreator('/en/docs/dataql/syntax/setter', '96b'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/statements',
        component: ComponentCreator('/en/docs/dataql/syntax/statements', 'a48'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/transform',
        component: ComponentCreator('/en/docs/dataql/syntax/transform', '0de'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/types',
        component: ComponentCreator('/en/docs/dataql/syntax/types', '865'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataql/syntax/valuescope',
        component: ComponentCreator('/en/docs/dataql/syntax/valuescope', '5fd'),
        exact: true,
        sidebar: "engine"
      },
      {
        path: '/en/docs/dataway/func/function',
        component: ComponentCreator('/en/docs/dataway/func/function', '961'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/func/use-in-program',
        component: ComponentCreator('/en/docs/dataway/func/use-in-program', 'd30'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/overview',
        component: ComponentCreator('/en/docs/dataway/overview', '093'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/quickstart',
        component: ComponentCreator('/en/docs/dataway/quickstart', 'b82'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/spi/authorizationchainspi',
        component: ComponentCreator('/en/docs/dataway/spi/authorizationchainspi', '779'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/spi/compilerspilistener',
        component: ComponentCreator('/en/docs/dataway/spi/compilerspilistener', 'ab9'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/spi/fxsqlcheckchainspi',
        component: ComponentCreator('/en/docs/dataway/spi/fxsqlcheckchainspi', '889'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/spi/loginchainspi',
        component: ComponentCreator('/en/docs/dataway/spi/loginchainspi', 'a54'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/spi/lookuplistener',
        component: ComponentCreator('/en/docs/dataway/spi/lookuplistener', 'f77'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/spi/preexecutechainspi',
        component: ComponentCreator('/en/docs/dataway/spi/preexecutechainspi', '8a4'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/spi/resultprocesschainspi',
        component: ComponentCreator('/en/docs/dataway/spi/resultprocesschainspi', '655'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/spi/serializationchainspi',
        component: ComponentCreator('/en/docs/dataway/spi/serializationchainspi', 'e3f'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/ui/apimanager',
        component: ComponentCreator('/en/docs/dataway/ui/apimanager', '871'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/ui/ui-editor',
        component: ComponentCreator('/en/docs/dataway/ui/ui-editor', 'c44'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/dataway/ui/ui-list',
        component: ComponentCreator('/en/docs/dataway/ui/ui-list', 'de7'),
        exact: true,
        sidebar: "dataway"
      },
      {
        path: '/en/docs/integration/overview',
        component: ComponentCreator('/en/docs/integration/overview', '395'),
        exact: true,
        sidebar: "integration"
      },
      {
        path: '/en/docs/integration/with-springboot',
        component: ComponentCreator('/en/docs/integration/with-springboot', '0ca'),
        exact: true,
        sidebar: "integration"
      },
      {
        path: '/en/docs/releases/3.2.x/v3.2.0',
        component: ComponentCreator('/en/docs/releases/3.2.x/v3.2.0', '61d'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/3.2.x/v3.2.1',
        component: ComponentCreator('/en/docs/releases/3.2.x/v3.2.1', '42e'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/3.2.x/v3.2.2',
        component: ComponentCreator('/en/docs/releases/3.2.x/v3.2.2', '74b'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.0.x/v4.0.x',
        component: ComponentCreator('/en/docs/releases/4.0.x/v4.0.x', '361'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.0',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.0', '4ed'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.1',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.1', '9d8'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.10',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.10', 'e54'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.11',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.11', '522'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.12',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.12', 'a99'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.13',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.13', 'c74'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.2',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.2', 'ca0'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.3',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.3', '4f8'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.4',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.4', 'c6b'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.5',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.5', 'f2f'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.6',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.6', '075'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.7',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.7', 'b99'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.8',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.8', '439'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.1.x/v4.1.9',
        component: ComponentCreator('/en/docs/releases/4.1.x/v4.1.9', '8a9'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.2.x/v4.2.0',
        component: ComponentCreator('/en/docs/releases/4.2.x/v4.2.0', '6aa'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.2.x/v4.2.1',
        component: ComponentCreator('/en/docs/releases/4.2.x/v4.2.1', '83d'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.2.x/v4.2.2',
        component: ComponentCreator('/en/docs/releases/4.2.x/v4.2.2', 'ff0'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.2.x/v4.2.3',
        component: ComponentCreator('/en/docs/releases/4.2.x/v4.2.3', 'a84'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.2.x/v4.2.4',
        component: ComponentCreator('/en/docs/releases/4.2.x/v4.2.4', '861'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/4.2.x/v4.2.5',
        component: ComponentCreator('/en/docs/releases/4.2.x/v4.2.5', '995'),
        exact: true,
        sidebar: "releases"
      },
      {
        path: '/en/docs/releases/latest',
        component: ComponentCreator('/en/docs/releases/latest', 'e4b'),
        exact: true,
        sidebar: "releases"
      }
    ]
  },
  {
    path: '/en/',
    component: ComponentCreator('/en/', 'ce3'),
    exact: true
  },
  {
    path: '*',
    component: ComponentCreator('*'),
  },
];

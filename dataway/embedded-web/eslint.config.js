/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import vue from 'eslint-plugin-vue';

export default [
    {ignores: ['build/**', 'dist/**', 'node_modules/**', 'test-results/**', 'playwright-report/**']},
    ...vue.configs['flat/essential'],
    {
        files: ['**/*.js', '**/*.vue'],
        rules: {
            curly: ['error', 'all'],
            'no-unused-vars': ['error', {argsIgnorePattern: '^_'}],
            'vue/multi-word-component-names': 'off',
        },
    },
];

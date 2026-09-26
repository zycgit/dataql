/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import path from 'node:path';
import {defineConfig, loadEnv} from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(async ({command, mode, isPreview}) => {
    const plugins = [vue()];
    let proxy;
    if (command === 'serve' && !isPreview) {
        if (mode === 'mock') {
            const {mockApi} = await import('./src/dev/mock.js');
            plugins.push(mockApi());
        } else {
            const {backendProxy} = await import('./src/dev/proxy.js');
            proxy = backendProxy(loadEnv(mode, import.meta.dirname, 'DATAWAY_'));
        }
    }
    return {
        base: command === 'serve' ? '/dataway/' : './',
        plugins,
        resolve: {alias: {'@': path.resolve(import.meta.dirname, 'src')}},
        build: {
            cssCodeSplit: false,
            rolldownOptions: {
                output: {
                    entryFileNames: 'assets/app.js',
                    chunkFileNames: 'assets/[name]-[hash].js',
                    assetFileNames: asset => asset.names.some(name => name.endsWith('.css'))
                        ? 'assets/app.css' : 'assets/[name]-[hash][extname]',
                },
            },
        },
        worker: {format: 'es'},
        server: {port: 8888, proxy},
    };
});

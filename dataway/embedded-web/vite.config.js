/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import {readdir, writeFile} from 'node:fs/promises';
import path from 'node:path';
import {defineConfig, loadEnv} from 'vite';
import vue from '@vitejs/plugin-vue';

// The host registers only the resources produced by this build, with its own prefix.
function resourceIndex() {
    let outputDirectory;
    return {
        name: 'dataway-resource-index',
        apply: 'build',
        configResolved(config) {
            outputDirectory = path.resolve(config.root, config.build.outDir);
        },
        async closeBundle() {
            const files = await readdir(outputDirectory, {recursive: true, withFileTypes: true});
            const names = files.filter(file => file.isFile() && file.name !== 'assets.list')
                .map(file => path.relative(outputDirectory, path.join(file.parentPath, file.name)).split(path.sep).join('/'))
                .sort();
            await writeFile(path.join(outputDirectory, 'assets.list'), names.join('\n') + '\n');
        },
    };
}

export default defineConfig(async ({command, mode, isPreview}) => {
    const plugins = [vue(), resourceIndex()];
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

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
const fs = require('node:fs/promises');
const path = require('node:path');
const {normalizeUrl} = require('@docusaurus/utils');

module.exports = function redirects(context, options) {
    const directory = path.join(context.generatedFilesDir, 'redirects');
    const mappings = Object.entries(options.redirects).map(([from, to]) => ({
        from,
        to: normalizeUrl([context.baseUrl, to]),
    }));
    return {
        name: 'legacy-redirects',
        async loadContent() {
            await fs.rm(directory, {recursive: true, force: true});
            for (const {from, to} of mappings) {
                const output = path.join(directory, from);
                await fs.mkdir(path.dirname(output), {recursive: true});
                await fs.writeFile(output, `<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Redirecting</title>
    <link rel="canonical" href="${to}">
    <script>window.location.replace(${JSON.stringify(to)} + window.location.search + window.location.hash);</script>
    <noscript><meta http-equiv="refresh" content="0; url=${to}"></noscript>
</head>
<body><a href="${to}">Continue to the documentation</a></body>
</html>
`);
            }
        },
        configureWebpack() {
            return {devServer: {static: [{directory, publicPath: context.baseUrl, watch: false}]}};
        },
        async postBuild({outDir, routesPaths}) {
            for (const {from, to} of mappings) {
                if (!routesPaths.includes(to)) {
                    throw new Error(`Redirect target does not exist: ${from} -> ${to}`);
                }
            }
            await fs.cp(directory, outDir, {recursive: true});
        },
    };
};

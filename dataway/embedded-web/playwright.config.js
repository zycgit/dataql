/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import {defineConfig} from '@playwright/test';
export default defineConfig({
    testDir: 'test/browser',
    timeout: 45_000,
    workers: 1,
    use: {baseURL: 'http://127.0.0.1:49181', viewport: {width: 1440, height: 900}, trace: 'retain-on-failure'},
    webServer: [
        {
            command: 'npm run dev:mock -- --port 49182 --strictPort',
            wait: {stdout: /http:\/\/127\.0\.0\.1:49182\/admin\//},
            timeout: 60_000,
        },
        {
            command: 'npm run dev:proxy -- --port 49183 --strictPort',
            wait: {stdout: /http:\/\/127\.0\.0\.1:49183\/admin\//},
            env: {DATAWAY_DEV_TARGET: 'http://127.0.0.1:49181',
                DATAWAY_DEV_ADMIN_PREFIX: '/gateway/operations', DATAWAY_DEV_API_PREFIX: '/gateway/invoke'},
            timeout: 60_000,
        },
    ],
});

/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import {expect, test} from '@playwright/test';

async function command(page, button, endpoint) {
    const response = page.waitForResponse(result => result.url().includes('/admin/api/' + endpoint));
    await page.getByRole('button', {name: button, exact: true}).click();
    return response;
}

async function createAndPublish(page, path) {
    await page.goto('/admin/#/new');
    await expect(page.locator('.code-editor[aria-label="API script"]')).toBeVisible();
    await page.getByRole('textbox', {name: 'API path', exact: true}).fill(path);
    expect((await command(page, 'Save', 'save-api')).status()).toBe(200);
    await expect(page).toHaveURL(/\/edit\/[^/]+$/);
    await expect(page.locator('.dirty-indicator')).toHaveCount(0);
    expect((await command(page, 'Smoke Test', 'smoke')).status()).toBe(200);
    await expect(page.getByRole('button', {name: 'Publish', exact: true})).toBeEnabled();
    expect((await command(page, 'Publish', 'publish')).status()).toBe(200);
    await expect(page.locator('.editor-toolbar .status-tag')).toHaveText('Published');
}

test.describe('mock development', () => {
    test.use({baseURL: 'http://127.0.0.1:49182'});

    test('the unchanged editor can save, smoke test, publish and call a mock API', async ({page}) => {
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await createAndPublish(page, '/browser-mock');
        expect((await command(page, 'Release History List', 'api-history')).status()).toBe(200);
        await expect(page.getByRole('button', {name: 'Restore release'})).toHaveCount(1);
        await page.locator('.editor-header').click({position: {x: 4, y: 4}});
        await page.getByRole('link', {name: 'Interface', exact: true}).click();
        await page.getByText('/browser-mock', {exact: true}).click();
        const invoked = page.waitForResponse(result => result.url().endsWith('/api/browser-mock'));
        await page.getByRole('button', {name: 'Execute Query', exact: true}).click();
        expect((await invoked).headers()['x-dataway-mock']).toBe('true');
        await expect(page.locator('.responsePanel')).toContainText('script was not executed');
        await expect(page.locator('.responsePanel')).toContainText('Hello DataQL.');
        await page.screenshot({path: 'test-results/mock-console.png'});
        expect(errors).toEqual([]);
    });

    test('sample SQL rows and binary downloads work in the result panel', async ({page}) => {
        await page.goto('/admin/');
        await page.getByText('/mock/users', {exact: true}).click();
        const rows = page.waitForResponse(result => result.url().endsWith('/api/mock/users'));
        await page.getByRole('button', {name: 'Execute Query', exact: true}).click();
        expect((await rows).status()).toBe(200);
        await expect(page.locator('.responsePanel')).toContainText('Alice');
        await page.getByText('/mock/download', {exact: true}).click();
        const binary = page.waitForResponse(result => result.url().endsWith('/api/mock/download'));
        await page.getByRole('button', {name: 'Execute Query', exact: true}).click();
        expect((await binary).headers()['content-type']).toBe('application/octet-stream');
        const downloaded = page.waitForEvent('download');
        await page.getByRole('button', {name: 'Save As Download', exact: true}).click();
        const file = await downloaded;
        expect(file.suggestedFilename()).toBe('mock-result.bin');
        const chunks = [];
        for await (const chunk of await file.createReadStream()) {
            chunks.push(chunk);
        }
        expect([...Buffer.concat(chunks)]).toEqual([0, 1, 255, 10]);
    });
});

test.describe('proxy development', () => {
    test.use({baseURL: 'http://127.0.0.1:49183'});

    test('custom prefixes reach real Java handlers and retain host authentication', async ({page, context}) => {
        expect((await context.request.get('/admin/api/api-list')).status()).toBe(401);
        await context.addCookies([{name: 'host-session', value: 'browser-test', url: 'http://127.0.0.1:49183'}]);
        await createAndPublish(page, '/browser-proxy');
        await page.getByRole('link', {name: 'Interface', exact: true}).click();
        await page.getByText('/browser-proxy', {exact: true}).click();
        const invoked = page.waitForResponse(result => result.url().endsWith('/api/browser-proxy'));
        await page.getByRole('button', {name: 'Execute Query', exact: true}).click();
        const response = await invoked;
        expect(response.status()).toBe(200);
        expect(response.headers()['x-dataway-mock']).toBeUndefined();
        await expect(page.locator('.responsePanel')).toContainText('Hello DataQL.');
        const checked = await context.request.post('/api/browser-proxy', {data: {message: 'Proxy reached Java'}});
        expect(checked.status()).toBe(200);
        expect((await checked.json()).value).toBe('Proxy reached Java');
    });
});

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

async function selectResultView(page, label) {
    await page.locator('.result-view-select').click();
    await page.getByRole('option', {name: label, exact: true}).click();
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

    test('initializer options select both public endpoints', async ({page}) => {
        const requests = [];
        page.on('request', request => {
            if (request.resourceType() === 'fetch') {
                requests.push(new URL(request.url()).pathname);
            }
        });
        await page.route('**/initializer.js', async route => {
            await route.fulfill({contentType: 'text/javascript', body: `
                window.addEventListener('load', () => {
                    window.DatawayUI({adminApi: '../operations/', api: '../invoke/'});
                });
            `});
        });
        for (const [publicPath, backendPath] of [['/operations/', '/admin/api/'], ['/invoke/', '/api/']]) {
            await page.route('**' + publicPath + '**', async route => {
                const url = new URL(route.request().url());
                url.pathname = url.pathname.replace(publicPath, backendPath);
                await route.fulfill({response: await route.fetch({url: url.href})});
            });
        }
        await page.goto('/admin/');
        await page.getByText('/mock/hello', {exact: true}).click();
        const invoked = page.waitForResponse(response => response.url().endsWith('/invoke/mock/hello'));
        await page.getByRole('button', {name: 'Execute Query', exact: true}).click();
        expect((await invoked).status()).toBe(200);
        await expect(page.locator('.responsePanel')).toContainText('Hello Dataway Mock.');
        expect(requests).toContain('/operations/api-list');
        expect(requests).toContain('/invoke/mock/hello');
        expect(requests.every(path => path.startsWith('/operations/') || path.startsWith('/invoke/'))).toBe(true);
    });

    test('async initialization retains the host session and retries before loading management data', async ({page, context}) => {
        await context.addCookies([{name: 'host-session', value: 'example', url: 'http://127.0.0.1:49182'}]);
        const requests = [];
        let configurations = 0;
        page.on('request', request => {
            if (request.resourceType() === 'fetch') {
                requests.push(new URL(request.url()).pathname);
            }
        });
        await page.route('**/initializer.js', async route => {
            await route.fulfill({contentType: 'text/javascript', body: `
                window.addEventListener('load', () => {
                    window.DatawayUI(async () => {
                        const response = await fetch('../host/ui-options', {credentials: 'same-origin'});
                        if (!response.ok) {
                            throw new Error('Host settings unavailable');
                        }
                        return response.json();
                    });
                });
            `});
        });
        await page.route('**/host/ui-options', async route => {
            expect(await route.request().headerValue('cookie')).toContain('host-session=example');
            configurations++;
            if (configurations === 1) {
                await route.fulfill({status: 503});
            } else {
                await route.fulfill({json: {adminApi: 'api/', api: '../api/'}});
            }
        });
        await page.goto('/admin/');
        await expect(page.getByRole('alert')).toContainText('Host settings unavailable');
        expect(requests).toEqual(['/host/ui-options']);
        await page.getByRole('button', {name: 'Retry', exact: true}).click();
        await expect(page.getByText('/mock/hello', {exact: true})).toBeVisible();
        expect(requests.slice(0, 3)).toEqual(['/host/ui-options', '/host/ui-options', '/admin/api/api-list']);
        expect(configurations).toBe(2);
    });

    for (const [name, options, message] of [
        ['missing', {}, 'adminApi'],
        ['cross-origin', {adminApi: 'https://other.test/api/'}, 'same-origin'],
    ]) {
        test('initializer reports ' + name + ' management address before sending requests', async ({page}) => {
            const requests = [];
            page.on('request', request => {
                if (request.resourceType() === 'fetch') {
                    requests.push(request.url());
                }
            });
            await page.route('**/initializer.js', async route => {
                await route.fulfill({contentType: 'text/javascript', body: `
                    window.addEventListener('load', () => window.DatawayUI(${JSON.stringify(options)}));
                `});
            });
            await page.goto('/admin/');
            await expect(page.getByRole('alert')).toContainText(message);
            await expect(page.getByRole('button', {name: 'Retry', exact: true})).toBeVisible();
            expect(requests).toEqual([]);
        });
    }

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

    test('the editor selects a result handler and downloads its CSV preview', async ({page}) => {
        await page.goto('/admin/#/edit/mock-users');
        await expect(page.getByRole('textbox', {name: 'API path', exact: true})).toHaveValue('/mock/users');
        const selector = page.getByRole('button', {name: 'Result Handler', exact: true});
        await expect(selector).toHaveText('Structure');
        await selector.click();
        await page.getByRole('menuitem', {name: 'CSV', exact: true}).click();
        await expect(selector).toHaveText('CSV');
        await expect(page.getByRole('menuitem', {name: 'CSV', exact: true})).not.toBeVisible();
        await expect(page.getByRole('tab', {name: 'Structure', exact: true})).toHaveClass(/is-disabled/);
        const response = await command(page, 'Execute Query', 'perform');
        expect(response.status()).toBe(200);
        expect(response.request().postDataJSON().optionInfo.resultHandler).toBe('csv');
        expect(response.request().postDataJSON().optionInfo).not.toHaveProperty('resultStructure');
        await expect(page.locator('.responsePanel')).toContainText('Alice');
        const downloaded = page.waitForEvent('download');
        await page.getByRole('button', {name: 'Save As Download', exact: true}).click();
        const file = await downloaded;
        expect(file.suggestedFilename()).toBe('results.csv');
        const chunks = [];
        for await (const chunk of await file.createReadStream()) {
            chunks.push(chunk);
        }
        expect(Buffer.concat(chunks).toString('utf8')).toBe('id,name\r\n1,Alice\r\n2,Bob\r\n');
    });

    test('Text displays SQL rows as text without a conversion error', async ({page}) => {
        await page.goto('/admin/#/edit/mock-users');
        await expect(page.getByRole('textbox', {name: 'API path', exact: true})).toHaveValue('/mock/users');
        await page.getByRole('button', {name: 'Result Handler', exact: true}).click();
        await page.getByRole('menuitem', {name: 'Text', exact: true}).click();
        const response = await command(page, 'Execute Query', 'perform');
        expect(response.headers()['content-type']).toBe('text/plain; charset=UTF-8');
        expect(response.request().postDataJSON().optionInfo.resultHandler).toBe('text');
        const result = page.locator('.responsePanel');
        await expect(result).toContainText('[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]');
        await expect(result).not.toContainText('Text result must be a string');
        await expect(result.getByRole('button', {name: 'Format Result', exact: true})).toHaveCount(0);
        await expect(page.getByRole('tab', {name: 'Structure', exact: true})).toHaveClass(/is-disabled/);
        await page.screenshot({path: 'test-results/text-result-rows.png', animations: 'disabled'});
    });

    test('output selection is exclusive and survives saving without losing the template', async ({page}) => {
        await page.goto('/admin/#/new');
        await page.getByRole('textbox', {name: 'API path', exact: true}).fill('/browser-output');
        const selector = page.getByRole('button', {name: 'Result Handler', exact: true});
        const structureTab = page.getByRole('tab', {name: 'Structure', exact: true});
        await expect(selector).toHaveText('Structure');
        await expect(page.getByRole('checkbox', {name: 'Structure', exact: true})).toHaveCount(0);
        await selector.click();
        await expect(page.getByRole('menu', {name: 'Result Handler'}).getByRole('menuitem')).toHaveText(['Structure', 'Raw Value', 'CSV', 'Text']);
        await expect(page.getByRole('menu', {name: 'Result Handler'}).locator('[aria-current="true"]')).toHaveText('Structure');
        await page.screenshot({path: 'test-results/result-handler-menu.png', animations: 'disabled'});
        await page.getByRole('menuitem', {name: 'Raw Value', exact: true}).click();
        await expect(structureTab).toHaveClass(/is-disabled/);
        await structureTab.click();
        await expect(page.getByRole('tab', {name: 'Result', exact: true})).toHaveAttribute('aria-selected', 'true');
        const raw = await command(page, 'Execute Query', 'perform');
        expect(raw.request().postDataJSON().optionInfo).toMatchObject({resultHandler: 'raw'});
        await expect(page.locator('.responsePanel')).toContainText(/"mock":\s*true/);
        await expect(page.locator('.responsePanel')).not.toContainText('"success"');
        const template = raw.request().postDataJSON().optionInfo.responseFormat;
        await command(page, 'Save', 'save-api');
        await expect(page).toHaveURL(/\/edit\/[^/]+$/);
        await command(page, 'Reload API', 'api-detail');
        await expect(selector).toHaveText('Raw Value');
        await selector.click();
        await page.getByRole('menuitem', {name: 'Structure', exact: true}).click();
        await expect(structureTab).not.toHaveClass(/is-disabled/);
        const structured = await command(page, 'Execute Query', 'perform');
        expect(structured.request().postDataJSON().optionInfo).toMatchObject({
            resultHandler: 'structure', responseFormat: template,
        });
        await expect(page.locator('.responsePanel')).toContainText(/"success":\s*true/);
        await expect(page.locator('.responsePanel')).toContainText('"value"');
        await structureTab.click();
        await expect(structureTab).toHaveAttribute('aria-selected', 'true');
        await selector.click();
        await page.getByRole('menuitem', {name: 'Text', exact: true}).click();
        await expect(structureTab).toHaveClass(/is-disabled/);
        await expect(page.getByRole('tab', {name: 'Result', exact: true})).toHaveAttribute('aria-selected', 'true');
        const saved = await command(page, 'Save', 'save-api');
        expect(saved.request().postDataJSON().optionInfo).toMatchObject({
            resultHandler: 'text', responseFormat: template,
        });
        await command(page, 'Reload API', 'api-detail');
        await expect(selector).toHaveText('Text');
        await page.getByRole('button', {name: 'More Settings', exact: true}).click();
        await expect(page.getByRole('dialog', {name: 'More Settings'})).not.toContainText('Result Handler');
    });

    test('custom handlers fit a narrow result panel and preserve their registered names', async ({page}) => {
        const name = 'application-specific-result-handler';
        await page.route('**/admin/api/result-handlers', route => route.fulfill({json: {success: true, result: ['structure', 'raw', 'csv', 'text', name]}}));
        await page.setViewportSize({width: 900, height: 700});
        await page.goto('/admin/#/new');
        await page.getByRole('textbox', {name: 'API path', exact: true}).fill('/browser-custom-output');
        const selector = page.getByRole('button', {name: 'Result Handler', exact: true});
        await selector.click();
        await page.getByRole('menuitem', {name, exact: true}).click();
        await expect(selector).toHaveText(name);
        const actions = await page.locator('.responsePanel > .panel-actions').boundingBox();
        const tabs = await page.locator('.responsePanel .el-tabs__header').boundingBox();
        expect(tabs.x + tabs.width).toBeLessThanOrEqual(actions.x + 1);
        await selector.click();
        await expect(page.getByRole('menu', {name: 'Result Handler'}).locator('[aria-current="true"]')).toHaveText(name);
        await page.screenshot({path: 'test-results/result-handler-narrow.png', animations: 'disabled'});
        await page.keyboard.press('Escape');
        const request = page.waitForRequest(request => request.url().includes('/admin/api/perform'));
        await page.getByRole('button', {name: 'Execute Query', exact: true}).click();
        expect((await request).postDataJSON().optionInfo).toMatchObject({resultHandler: name});
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

    for (const editor of [false, true]) {
        const mode = editor ? 'editor' : 'Interface';
        test(mode + ' switches JSON, table and text views without changing or re-executing the API', async ({page}) => {
            await page.goto(editor ? '/admin/#/edit/mock-users' : '/admin/');
            if (editor) {
                await expect(page.getByRole('textbox', {name: 'API path', exact: true})).toHaveValue('/mock/users');
            } else {
                await page.getByText('/mock/users', {exact: true}).click();
            }
            let calls = 0;
            page.on('request', request => {
                if (request.url().includes(editor ? '/admin/api/perform' : '/api/mock/users')) {
                    calls++;
                }
            });
            await page.getByRole('button', {name: 'Execute Query', exact: true}).click();
            const result = page.locator('.responsePanel');
            await expect(result.locator('.result-view-select')).toContainText('JSON');
            await expect(result).toContainText('Alice');
            await selectResultView(page, 'Table');
            await expect(result.locator('.result-table-summary')).toHaveText('$.value · 2 rows');
            await expect(result.getByRole('columnheader', {name: 'name', exact: true})).toBeVisible();
            await expect(result.getByRole('cell', {name: 'Alice', exact: true})).toBeVisible();
            await page.screenshot({path: 'test-results/' + mode + '-result-table.png', animations: 'disabled'});
            await selectResultView(page, 'Text');
            await expect(result.locator('.result-view-select')).toContainText('Text');
            await expect(result).toContainText('Alice');
            await expect(result.getByRole('button', {name: 'Format Result', exact: true})).toHaveCount(0);
            await selectResultView(page, 'JSON');
            await expect(result.getByRole('button', {name: 'Format Result', exact: true})).toBeVisible();
            expect(calls).toBe(1);
            if (editor) {
                await expect(page.getByRole('button', {name: 'Result Handler', exact: true})).toHaveText('Structure');
                await expect(page.locator('.dirty-indicator')).toHaveCount(0);
            }
        });

        test(mode + ' uses Content-Type for images, file downloads, CSV and plain text', async ({page}) => {
            const errors = [];
            page.on('pageerror', error => errors.push(error.message));
            for (const name of ['image', 'download', 'csv', 'text']) {
                await page.goto(editor ? '/admin/#/edit/mock-' + name : '/admin/');
                if (editor) {
                    await expect(page.getByRole('textbox', {name: 'API path', exact: true})).toHaveValue('/mock/' + name);
                } else {
                    await page.getByText('/mock/' + name, {exact: true}).click();
                }
                await page.getByRole('button', {name: 'Execute Query', exact: true}).click();
                const result = page.locator('.responsePanel');
                if (name === 'image') {
                    const image = result.getByRole('img', {name: 'Response preview', exact: true});
                    await expect(image).toBeVisible();
                    await expect.poll(() => image.evaluate(node => node.complete && node.naturalWidth)).toBe(640);
                    await expect(result.locator('.result-content-type')).toHaveText('image/svg+xml');
                    await page.screenshot({path: 'test-results/' + mode + '-result-image.png', animations: 'disabled'});
                    await selectResultView(page, 'File');
                    await expect(result).toContainText('dataway-preview.svg');
                    await selectResultView(page, 'Image');
                    await expect(image).toBeVisible();
                } else if (name === 'download') {
                    await expect(result.locator('.result-view-select')).toContainText('File');
                    await expect(result).toContainText('mock-result.bin');
                    const downloading = page.waitForEvent('download');
                    await result.getByRole('button', {name: 'Download file', exact: true}).click();
                    const file = await downloading;
                    expect(file.suggestedFilename()).toBe('mock-result.bin');
                    const chunks = [];
                    for await (const chunk of await file.createReadStream()) {
                        chunks.push(chunk);
                    }
                    expect([...Buffer.concat(chunks)]).toEqual([0, 1, 255, 10]);
                } else if (name === 'csv') {
                    await expect(result.locator('.result-view-select')).toContainText('Table');
                    await expect(result.getByRole('cell', {name: 'Alice, "A"', exact: true})).toBeVisible();
                    await selectResultView(page, 'Text');
                    await expect(result).toContainText('"Alice, ""A"""');
                } else {
                    await expect(result.locator('.result-view-select')).toContainText('Text');
                    await expect(result).toContainText('Plain text · 你好');
                }
            }
            expect(errors).toEqual([]);
        });
    }
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

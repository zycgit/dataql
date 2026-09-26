/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import {expect, test} from '@playwright/test';

async function openEditor(page, context, path) {
    await context.addCookies([{name: 'host-session', value: 'browser-test', url: 'http://127.0.0.1:49181'}]);
    await context.grantPermissions(['clipboard-read', 'clipboard-write']);
    await page.goto('/gateway/console/#/new');
    await expect(page.locator('.code-editor[aria-label="API script"]')).toBeVisible();
    await page.getByRole('textbox', {name: 'API path', exact: true}).fill(path);
}

async function editor(page, name, value) {
    await page.evaluate(text => navigator.clipboard.writeText(text), value);
    await page.locator('.code-editor[aria-label="' + name + '"]').click();
    await page.keyboard.press('Escape');
    await page.keyboard.press('ControlOrMeta+A');
    await page.keyboard.press('ControlOrMeta+V');
}

async function command(page, button, endpoint) {
    const response = page.waitForResponse(result => result.url().includes('/operations/' + endpoint));
    await page.getByRole('button', {name: button, exact: true}).click();
    return response;
}

async function save(page) {
    const response = await command(page, 'Save', 'save-api');
    expect(response.status()).toBe(200);
    await expect(page).toHaveURL(/\/edit\/[^/]+$/);
    await expect(page.locator('.dirty-indicator')).toHaveCount(0);
    return page.url().split('/edit/')[1];
}

async function publish(page) {
    expect((await command(page, 'Smoke Test', 'smoke')).status()).toBe(200);
    await expect(page.getByRole('button', {name: 'Publish', exact: true})).toBeEnabled();
    expect((await command(page, 'Publish', 'publish')).status()).toBe(200);
    await expect(page.locator('.editor-toolbar .status-tag')).toHaveText('Published');
}

test('legacy layout and the complete API lifecycle work against real embedded handlers', async ({page, context}) => {
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => {
        if (message.type() === 'error' && /Content Security Policy|worker/i.test(message.text())) {
            errors.push(message.text());
        }
    });
    await openEditor(page, context, '/browser-lifecycle');
    const header = await page.locator('.el-header').boundingBox();
    const script = await page.locator('.code-editor[aria-label="API script"]').boundingBox();
    expect(header.height).toBe(60);
    expect(script.x).toBe(0);
    expect(script.width).toBeGreaterThan(700);
    expect(script.width).toBeLessThan(735);
    expect(script.y).toBeLessThan(110);
    const requestPane = page.locator('.interface-edit .requestPanel');
    const responsePane = page.locator('.interface-edit .responsePanel');
    await expect.poll(async () => {
        const top = await requestPane.boundingBox();
        const bottom = await responsePane.boundingBox();
        return top.height / (top.height + bottom.height);
    }).toBeCloseTo(0.5, 2);
    await page.screenshot({path: 'test-results/editor.png'});
    const divider = await page.locator('.editor-workspace > .el-splitter-bar .el-splitter-bar__dragger').boundingBox();
    await page.mouse.move(divider.x + divider.width / 2, divider.y + divider.height / 3);
    await page.mouse.down();
    await page.mouse.move(divider.x + 100, divider.y + divider.height / 3, {steps: 10});
    await page.mouse.up();
    await expect.poll(async () => (await page.locator('.code-editor[aria-label="API script"]').boundingBox()).width).toBeGreaterThan(790);
    await page.getByRole('button', {name: 'Format Parameters', exact: true}).click();
    await editor(page, 'API script', "return {'version':'one'};");
    const id = await save(page);
    await editor(page, 'API script', "return {'version':'unsaved'};");
    let response = await command(page, 'Execute Query', 'perform');
    await expect(page.locator('.responsePanel')).toContainText('unsaved');
    const detail = await context.request.get('/gateway/operations/api-detail?id=' + id);
    expect((await detail.json()).result.codeInfo.codeValue).toBe("return {'version':'one'};");
    await editor(page, 'API script', "return {'version':'one'};");
    await publish(page);
    await page.getByRole('link', {name: 'Interface', exact: true}).click();
    await page.getByText('/browser-lifecycle', {exact: true}).click();
    response = page.waitForResponse(result => result.url().endsWith('/invoke/browser-lifecycle'));
    await page.getByRole('button', {name: 'Execute Query', exact: true}).click();
    expect((await response).status()).toBe(200);
    await expect(page.locator('.responsePanel')).toContainText('one');
    await page.screenshot({path: 'test-results/list.png'});
    await page.locator('tr').filter({hasText: '/browser-lifecycle'}).getByRole('link', {name: 'Edit API'}).click();
    await expect(page).toHaveURL(new RegExp('/edit/' + id));
    await editor(page, 'API script', "return {'version':'two'};");
    await save(page);
    await publish(page);
    await command(page, 'Release History List', 'api-history');
    await expect(page.getByRole('button', {name: 'Restore release'})).toHaveCount(2);
    await page.getByRole('button', {name: 'Restore release'}).last().click();
    await expect(page.locator('.dirty-indicator')).toBeVisible();
    await page.locator('.editor-header').click({position: {x: 4, y: 4}});
    await save(page);
    const restored = await context.request.get('/gateway/operations/api-detail?id=' + id);
    expect((await restored.json()).result.codeInfo.codeValue).toBe("return {'version':'one'};");
    await page.getByRole('button', {name: 'Disable API', exact: true}).click();
    const disabled = page.waitForResponse(result => result.url().includes('/operations/disable'));
    await page.getByRole('button', {name: 'OK', exact: true}).click();
    expect((await disabled).status()).toBe(200);
    await expect(page.locator('.editor-toolbar .status-tag')).toHaveText('Disable');
    expect((await context.request.post('/gateway/invoke/browser-lifecycle', {data: {}})).status()).toBe(404);
    await page.getByRole('button', {name: 'Delete API', exact: true}).click();
    const deleted = page.waitForResponse(result => result.url().includes('/operations/delete'));
    await page.getByRole('button', {name: 'OK', exact: true}).click();
    expect((await deleted).status()).toBe(200);
    await expect(page).toHaveURL(/#\/$/);
    expect(errors).toEqual([]);
});

test('version conflicts keep edits and unsaved navigation can be cancelled', async ({page, context}) => {
    await openEditor(page, context, '/browser-conflict');
    const id = await save(page);
    const detail = (await (await context.request.get('/gateway/operations/api-detail?id=' + id)).json()).result;
    expect((await context.request.post('/gateway/operations/save-api?id=' + id, {
        data: {
            version: detail.version, select: detail.select, apiPath: detail.path, codeType: 'DataQL',
            codeValue: "return 'another user';", requestBody: '{}', optionInfo: {resultStructure: false},
        }
    })).status()).toBe(200);
    await editor(page, 'API script', "return 'my unsaved change';");
    expect((await command(page, 'Save', 'save-api')).status()).toBe(409);
    await expect(page.getByText(/Your edits were kept/)).toBeVisible();
    await expect(page.locator('.dirty-indicator')).toBeVisible();
    page.once('dialog', dialog => dialog.dismiss());
    await page.getByRole('link', {name: 'Interface', exact: true}).click();
    await expect(page).toHaveURL(new RegExp('/edit/' + id));
    page.once('dialog', dialog => dialog.accept());
    await command(page, 'Reload API', 'api-detail');
    await expect(page.locator('.dirty-indicator')).toHaveCount(0);
});

test('SQL, parameter wrapping, custom headers and response structure remain editable', async ({page, context}) => {
    await openEditor(page, context, '/browser-sql');
    await page.locator('.el-radio').filter({hasText: /^SQL$/}).click();
    await editor(page, 'API script', 'select #{message} as "message"');
    let response = await command(page, 'Execute Query', 'perform');
    expect(response.status()).toBe(200);
    await expect(page.locator('.responsePanel')).toContainText('Hello DataQL.');
    await save(page);
    await publish(page);
    const invoked = await context.request.post('/gateway/invoke/browser-sql', {data: {message: 'published SQL'}});
    expect((await invoked.json()).value).toBe('published SQL');
    await page.locator('.el-radio').filter({hasText: /^DataQL$/}).click();
    await editor(page, 'API script', "import 'net.hasor.dataway.function.WebUdfSource' as w; return {'name':${root}.message,'header':w.header('x-custom'),'cookie':w.cookie('host-session')};");
    await page.getByRole('button', {name: 'More Settings', exact: true}).click();
    await page.locator('.parameter-settings .el-switch').click();
    await page.keyboard.press('Escape');
    await page.getByRole('tab', {name: 'Headers', exact: true}).click();
    await page.getByRole('button', {name: 'Add Header', exact: true}).click();
    await page.getByRole('textbox', {name: 'Header name', exact: true}).fill('X-CuStOm');
    await page.getByRole('textbox', {name: 'Header value', exact: true}).fill('a b=1');
    response = await command(page, 'Execute Query', 'perform');
    expect(response.status()).toBe(200);
    await expect(page.locator('.responsePanel')).toContainText('a b=1');
    await expect(page.locator('.responsePanel')).toContainText('browser-test');
    await page.getByRole('tab', {name: 'Structure', exact: true}).click();
    await editor(page, 'Response structure', '{"data":"@resultData","ok":"@resultStatus"}');
    response = await command(page, 'Execute Query', 'perform');
    expect(response.status()).toBe(200);
    await expect(page.locator('.responsePanel')).toContainText('\"ok\"');
    await page.locator('.structure-toggle').click();
    response = await command(page, 'Execute Query', 'perform');
    expect(response.status()).toBe(200);
    await expect(page.locator('.responsePanel')).toContainText('a b=1');
    await expect(page.locator('.responsePanel')).not.toContainText('\"success\"');
});

test('binary responses keep all bytes when downloaded from the result panel', async ({page, context}) => {
    await openEditor(page, context, '/binary');
    const response = await command(page, 'Execute Query', 'perform');
    expect(response.headers()['content-type']).toBe('application/octet-stream');
    await expect(page.getByRole('button', {name: 'Save As Download', exact: true})).toBeVisible();
    const downloaded = page.waitForEvent('download');
    await page.getByRole('button', {name: 'Save As Download', exact: true}).click();
    const file = await downloaded;
    expect(file.suggestedFilename()).toBe('result.bin');
    const stream = await file.createReadStream();
    const chunks = [];
    for await (const chunk of stream) {
        chunks.push(chunk);
    }
    expect([...Buffer.concat(chunks)]).toEqual([0, 1, 255, 10]);
});

test('management requires host identity and the console rejects an invalid deployment address', async ({page, context}) => {
    expect((await context.request.get('/gateway/console/assets/app.js')).status()).toBe(200);
    expect((await context.request.get('/gateway/operations/api-list')).status()).toBe(401);
    await context.addCookies([{name: 'host-session', value: 'browser-test', url: 'http://127.0.0.1:49181'}]);
    await page.route('**/config.json', route => route.fulfill({json: {adminApi: 'https://other.test/api/'}}));
    await page.goto('/gateway/console/');
    await expect(page.getByRole('alert')).toContainText('same-origin');
    await expect(page.getByRole('button', {name: 'Retry', exact: true})).toBeVisible();
});

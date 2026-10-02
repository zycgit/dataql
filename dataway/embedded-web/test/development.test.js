/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import assert from 'node:assert/strict';
import {after, before, test} from 'node:test';
import {createServer as httpServer} from 'node:http';
import path from 'node:path';
import {createServer} from 'vite';
import {newInterface} from '../src/utils/model.js';

let mock;
before(async () => {
    // Mock mode must not validate or contact the configured backend.
    mock = await developmentServer('mock', {DATAWAY_DEV_TARGET: 'not-a-backend'});
});
after(async () => {
    await mock?.server.close();
});

async function developmentServer(mode, env = {}) {
    const previous = Object.fromEntries(Object.keys(env).map(key => [key, process.env[key]]));
    Object.assign(process.env, env);
    try {
        const server = await createServer({root: path.resolve(import.meta.dirname, '..'), mode, logLevel: 'silent',
            server: {host: '127.0.0.1', port: 0, strictPort: true, hmr: false, watch: null},
            optimizeDeps: {noDiscovery: true, include: []}});
        await server.listen();
        return {server, origin: 'http://127.0.0.1:' + server.httpServer.address().port};
    } finally {
        for (const [key, value] of Object.entries(previous)) {
            if (value === undefined) {
                delete process.env[key];
            } else {
                process.env[key] = value;
            }
        }
    }
}

async function management(endpoint, {id, body, query = '', status = 200} = {}) {
    const url = new URL('/admin/api/' + endpoint + query, mock.origin);
    if (id !== undefined) {
        url.searchParams.set('id', id);
    }
    const response = await fetch(url, body === undefined ? {} : {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body),
    });
    assert.equal(response.status, status);
    return response.json();
}

test('mock serves the independent initializer and seeded management documents', async () => {
    const page = await fetch(mock.origin + '/admin/');
    assert.equal(page.status, 200);
    const html = await page.text();
    assert.match(html, /src\/main.js/);
    assert.match(html, /src="\/admin\/initializer.js" defer/);
    assert.doesNotMatch(html, /dataway-admin-api|dataway-api|config\.json/);
    const initializer = await fetch(mock.origin + '/admin/initializer.js');
    assert.equal(initializer.status, 200);
    assert.match(await initializer.text(), /window\.DatawayUI\(/);
    const list = await management('api-list');
    assert.equal(list.success, true);
    assert.deepEqual(list.result.map(item => item.status), [1, 1, 1, 0, 3, 1, 1, 1, 1]);
    const detail = (await management('api-detail', {id: 'mock-hello'})).result;
    assert.equal(detail.version, 2);
    assert.equal(JSON.parse(detail.requestBody).message, 'Hello Dataway Mock.');
    assert.equal(detail.codeInfo.headerData[0].name, 'X-Demo');
});

test('mock supports edits, immutable publications, conflicts, history, disabling and deletion', async () => {
    const form = {...newInterface(), apiPath: '/test/lifecycle', optionInfo: {resultHandler: 'raw'},
        sample: {custom: 'preserved'}, schema: {type: 'object'}};
    const created = await management('save-api', {id: '-1', body: form});
    const id = created.result;
    assert.equal(created.version, 1);
    const command = {id, version: 1};
    const beforePublish = await fetch(mock.origin + '/api/test/lifecycle', {method: 'POST'});
    assert.equal(beforePublish.status, 404);
    const preview = await management('perform', {id, body: {...form, id, codeValue: 'not valid DataQL'}});
    assert.equal(preview.mock, true);
    const first = (await management('api-detail', {id})).result;
    assert.equal(first.status, 0);
    assert.equal(first.codeInfo.codeValue, form.codeValue);
    await management('smoke', {id, body: {...command, requestBody: {value: 'smoke'}}});
    assert.equal((await management('publish', {id, body: command})).version, 2);
    await management('save-api', {id, body: {...form, ...command}, status: 409});
    await management('save-api', {id, body: {...form, id, version: 2, apiPath: '/changed'}, status: 409});
    const updated = {...form, id, version: 2, codeValue: 'changed script',
        optionInfo: {resultHandler: 'structure', responseFormat: '{"data":"@resultData"}'}};
    assert.equal((await management('save-api', {id, body: updated})).version, 3);
    assert.equal((await management('api-detail', {id})).result.status, 2);
    const published = await (await fetch(mock.origin + '/api/test/lifecycle', {method: 'POST'})).json();
    assert.equal(published.mock, true);
    assert.equal(published.data, undefined);
    const smoke = await management('smoke', {id, body: {version: 3, requestBody: {value: 'draft'}}});
    assert.equal(smoke.data.parameters.value, 'draft');
    assert.equal((await management('publish', {id, body: {version: 3}})).version, 4);
    const history = (await management('api-history', {id})).result;
    assert.deepEqual(history.map(item => item.status), [1, 3]);
    const restored = (await management('get-history', {id, query: '?historyId=' + history[1].historyId})).result;
    assert.equal(restored.version, 4);
    assert.equal(restored.codeInfo.codeValue, form.codeValue);
    assert.equal(restored.sample.custom, 'preserved');
    assert.deepEqual(restored.schema, {type: 'object'});
    await management('get-history', {id: 'mock-hello', query: '?historyId=' + history[1].historyId, status: 404});
    assert.equal((await management('disable', {id, body: {version: 4}})).version, 5);
    assert.equal((await management('api-detail', {id})).result.status, 3);
    assert.equal((await fetch(mock.origin + '/api/test/lifecycle', {method: 'POST'})).status, 404);
    await management('delete', {id, body: {version: 4}, status: 409});
    await management('delete', {id, body: {version: 5}});
    await management('api-detail', {id, status: 404});
    await management('api-history', {id, status: 404});
});

test('mock enforces method/path uniqueness and validates management requests', async () => {
    const form = {...newInterface(), apiPath: '/mock/hello'};
    await management('save-api', {id: '-1', body: form, status: 409});
    const created = await management('save-api', {id: '-1', body: {...form, select: 'GET'}});
    await management('publish', {id: created.result, body: {version: 1}});
    const response = await fetch(mock.origin + '/api/mock/hello?tag=a&tag=b');
    assert.deepEqual((await response.json()).value.parameters.tag, ['a', 'b']);
    await management('publish', {id: 'mock-draft', body: {}, status: 400});
    await management('publish', {id: 'mock-draft', body: {version: -1}, status: 400});
    await management('publish', {id: 'mock-draft', body: {id: 'different', version: 1}, status: 400});
    await management('api-detail', {query: '?id=a&id=b', status: 400});
    await management('api-detail', {status: 400});
    await management('missing', {status: 404});
    await management('save-api', {id: '-1', status: 405});
    await management('perform', {id: '-1', body: {...form, requestBody: '[]'}, status: 400});
    const malformed = await fetch(mock.origin + '/admin/api/save-api?id=-1', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: '{',
    });
    assert.equal(malformed.status, 400);
    const text = await fetch(mock.origin + '/admin/api/perform?id=-1', {method: 'POST', body: '{}'});
    assert.equal(text.status, 415);
});

test('mock applies headers, parameter wrapping, response templates and binary downloads', async () => {
    const greeting = await fetch(mock.origin + '/api/mock/hello?message=query', {method: 'POST',
        headers: {'Content-Type': 'application/json', 'x-DeMo': 'custom'}, body: '{"message":"body"}'});
    assert.equal(greeting.headers.get('x-dataway-mock'), 'true');
    const value = (await greeting.json()).value;
    assert.equal(value.message, 'body');
    assert.equal(value.header, 'custom');
    const form = {...newInterface(), apiPath: '/test/wrapping', requestBody: {name: 'example'},
        optionInfo: {wrapAllParameters: true, wrapParameterName: 'root',
            responseFormat: '{"ok":"@resultStatus","data":"@resultData","literal":"kept"}'}};
    const wrapped = await management('perform', {id: '-1', body: form});
    assert.equal(wrapped.ok, true);
    assert.equal(wrapped.literal, 'kept');
    assert.deepEqual(wrapped.data.parameters, {root: {name: 'example'}});
    const binary = await fetch(mock.origin + '/api/mock/download');
    assert.equal(binary.headers.get('content-type'), 'application/octet-stream');
    assert.match(binary.headers.get('content-disposition'), /mock-result.bin/);
    assert.deepEqual([...new Uint8Array(await binary.arrayBuffer())], [0, 1, 255, 10]);
});

test('restarting mock mode resets changes to the sample definitions', async () => {
    await management('disable', {id: 'mock-hello', body: {version: 2}});
    await mock.server.close();
    mock = await developmentServer('mock');
    const restored = (await management('api-detail', {id: 'mock-hello'})).result;
    assert.equal(restored.status, 1);
    assert.equal(restored.version, 2);
    const list = (await management('api-list')).result;
    assert.equal(list.length, 9);
});

test('proxy rewrites prefixes and preserves request bytes, credentials, cookies and backend errors', async t => {
    const received = [];
    const backend = httpServer(async (request, response) => {
        const chunks = [];
        for await (const chunk of request) {
            chunks.push(chunk);
        }
        received.push({url: request.url, method: request.method, headers: request.headers, body: Buffer.concat(chunks)});
        if (request.url.startsWith('/app/operations/')) {
            response.writeHead(401, {'Content-Type': 'application/json',
                'Set-Cookie': 'host-session=next; Domain=backend.test; Path=/app; HttpOnly; SameSite=Lax'});
            response.end('{"success":false,"message":"Sign in to the host"}');
        } else if (request.url.startsWith('/app/invoke/failure')) {
            response.writeHead(409, {'Content-Type': 'application/json'});
            response.end('{"message":"Conflict from backend"}');
        } else {
            response.writeHead(200, {'Content-Type': 'application/octet-stream',
                'Content-Disposition': 'attachment; filename="backend.bin"'});
            response.end(Buffer.from([0, 1, 255, 10]));
        }
    });
    await new Promise(resolve => backend.listen(0, '127.0.0.1', resolve));
    t.after(() => {
        if (backend.listening) {
            return new Promise(resolve => backend.close(resolve));
        }
    });
    const target = 'http://127.0.0.1:' + backend.address().port;
    const proxy = await developmentServer('proxy', {DATAWAY_DEV_TARGET: target,
        DATAWAY_DEV_ADMIN_PREFIX: '/app/operations/', DATAWAY_DEV_API_PREFIX: '/app/invoke'});
    t.after(() => proxy.server.close());
    const unauthorized = await fetch(proxy.origin + '/admin/api/api-list?filter=a%2Fb', {
        headers: {Authorization: 'Bearer test', Cookie: 'host-session=current', 'X-CSRF-Token': 'csrf'},
    });
    assert.equal(unauthorized.status, 401);
    assert.equal((await unauthorized.json()).message, 'Sign in to the host');
    assert.equal(unauthorized.headers.get('x-dataway-mock'), null);
    assert.equal(unauthorized.headers.get('set-cookie'), 'host-session=next; Path=/; HttpOnly; SameSite=Lax');
    assert.equal(received[0].url, '/app/operations/api-list?filter=a%2Fb');
    assert.equal(received[0].headers.authorization, 'Bearer test');
    assert.equal(received[0].headers.cookie, 'host-session=current');
    assert.equal(received[0].headers['x-csrf-token'], 'csrf');
    assert.equal(received[0].headers.host, new URL(target).host);
    const bytes = Buffer.from([255, 0, 10, 123]);
    const binary = await fetch(proxy.origin + '/api/download?name=a%20b', {method: 'POST',
        headers: {'Content-Type': 'application/octet-stream'}, body: bytes});
    assert.deepEqual(Buffer.from(await binary.arrayBuffer()), Buffer.from([0, 1, 255, 10]));
    assert.match(binary.headers.get('content-disposition'), /backend.bin/);
    assert.equal(received[1].url, '/app/invoke/download?name=a%20b');
    assert.equal(received[1].method, 'POST');
    assert.deepEqual(received[1].body, bytes);
    const conflict = await fetch(proxy.origin + '/api/failure');
    assert.equal(conflict.status, 409);
    assert.equal((await conflict.json()).message, 'Conflict from backend');
    await fetch(proxy.origin + '/api-extra', {redirect: 'manual'});
    await fetch(proxy.origin + '/admin/api-extra', {headers: {Accept: 'text/plain'}});
    assert.equal(received.length, 3);
    const rootProxy = await developmentServer('proxy', {DATAWAY_DEV_TARGET: target,
        DATAWAY_DEV_ADMIN_PREFIX: '/admin/api', DATAWAY_DEV_API_PREFIX: '/'});
    t.after(() => rootProxy.server.close());
    const root = await fetch(rootProxy.origin + '/api?tag=root');
    assert.equal(root.status, 200);
    assert.equal(received[3].url, '/?tag=root');
    await new Promise(resolve => backend.close(resolve));
    const unavailable = await fetch(proxy.origin + '/admin/api/api-list');
    assert.equal(unavailable.status, 502);
    assert.equal(unavailable.headers.get('x-dataway-mock'), null);
});

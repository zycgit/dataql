/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import {afterEach, test} from 'node:test';
import assert from 'node:assert/strict';
import {baseAddress, DatawayClient, loadConfiguration, parameters, readResponse, requestHeaders} from '../src/utils/api.js';
import {directories, editInterface} from '../src/utils/model.js';
const originalFetch = globalThis.fetch;
const page = 'https://example.test/gateway/console/';
afterEach(() => { globalThis.fetch = originalFetch; });

test('public endpoints are resolved from static browser configuration under a rewritten prefix', () => {
    assert.equal(baseAddress('../operations/', page, 'adminApi').href, 'https://example.test/gateway/operations/');
    for (const value of ['https://other.test/api/', '/missing-slash', '/api/?secret=1', '/api/#x', 'https://a:b@example.test/api/']) {
        assert.throws(() => baseAddress(value, page, 'adminApi'));
    }
});

test('configuration and management requests retain the host session without a Dataway login', async () => {
    const calls = [];
    globalThis.fetch = async (url, options) => {
        calls.push({url: url.href, options});
        return Response.json(calls.length === 1 ? {adminApi: '../operations/', api: '../invoke/'} : {success: true, result: []});
    };
    const config = await loadConfiguration(page);
    const client = new DatawayClient(config, page);
    await client.management('api-detail', {id: 'a b'});
    assert.equal(calls[0].url, page + 'config.json');
    assert.equal(calls[1].url, 'https://example.test/gateway/operations/api-detail?id=a+b');
    assert.equal(calls[0].options.credentials, 'same-origin');
    assert.equal(calls[1].options.credentials, 'same-origin');
});

test('version conflicts are surfaced instead of silently retrying a write', async () => {
    let calls = 0;
    globalThis.fetch = async (_url, options) => {
        calls++;
        assert.equal(JSON.parse(options.body).version, 7);
        return Response.json({success: false, message: 'API changed'}, {status: 409});
    };
    await assert.rejects(new DatawayClient({adminApi: 'api/'}, page).management('save-api', {method: 'POST', body: {version: 7}}),
        error => error.status === 409 && error.message === 'API changed');
    assert.equal(calls, 1);
});

test('header names are case insensitive and values are not URI encoded', () => {
    const headers = requestHeaders([{checked: true, name: ' X-Custom ', value: 'a b=1'},
        {checked: true, name: 'x-custom', value: 'two'}, {checked: false, name: 'Ignored', value: 'x'}]);
    assert.equal(headers.get('X-CUSTOM'), 'a b=1, two');
    assert.equal(headers.has('Ignored'), false);
});

test('business invocations use the configured address and correct HTTP body semantics', async () => {
    const calls = [];
    globalThis.fetch = async (url, options) => {
        calls.push({url: url.href, options});
        return Response.json({ok: true});
    };
    const client = new DatawayClient({adminApi: '../operations/', api: '../invoke/'}, page);
    await client.invoke({path: '/echo', select: 'GET'}, '{"q":"a b"}', []);
    assert.equal(calls[0].url, 'https://example.test/gateway/invoke/echo?q=a+b');
    assert.equal(calls[0].options.body, undefined);
    await client.invoke({path: '/echo', select: 'POST'}, '{"nested":{"value":1}}', []);
    assert.deepEqual(JSON.parse(calls[1].options.body), {nested: {value: 1}});
    await assert.rejects(client.invoke({path: '/../outside', select: 'POST'}, '{}', []));
    for (const value of ['null', '[]', '"string"']) {
        assert.throws(() => parameters(value));
    }
    assert.throws(() => parameters('{"nested":{}}', 'GET'));
});

test('JSON, text, and binary results preserve the original download bytes and content type', async () => {
    let result = await readResponse(Response.json({name: 'Ada'}), performance.now());
    assert.equal(result.kind, 'json');
    assert.deepEqual(result.data, {name: 'Ada'});
    result = await readResponse(new Response('plain text', {headers: {'Content-Type': 'text/plain'}}), performance.now());
    assert.equal(result.text, 'plain text');
    const bytes = new Uint8Array([0, 128, 255, 10]);
    result = await readResponse(new Response(bytes, {headers: {'Content-Type': 'application/pdf',
        'Content-Disposition': "attachment; filename*=UTF-8''report%20one.pdf"}}), performance.now());
    assert.equal(result.kind, 'bytes');
    assert.equal(result.filename, 'report one.pdf');
    assert.equal(result.text, '00 80 FF 0A');
    assert.deepEqual(new Uint8Array(await result.blob.arrayBuffer()), bytes);
});

test('imported metadata and false editor options survive an edit round trip', () => {
    const detail = {id: 'id', version: 4, select: 'POST', path: '/hello', status: 1, codeType: 'DataQL',
        codeInfo: {codeValue: 'return 1;', requestBody: '{}', headerData: []},
        optionData: {resultStructure: false, extra: 123}, schema: {custom: 1}, sample: {custom: 2}};
    const form = editInterface(detail);
    assert.equal(form.version, 4);
    assert.equal(form.optionInfo.resultStructure, false);
    assert.equal(form.optionInfo.extra, 123);
    assert.deepEqual(form.schema, detail.schema);
    assert.deepEqual(form.sample, detail.sample);
    assert.equal(directories([{path: '/one/two'}, {path: '/one/three'}])[0].children.length, 1);
});

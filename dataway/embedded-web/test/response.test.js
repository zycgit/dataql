/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import assert from 'node:assert/strict';
import {test} from 'node:test';
import {readResponse} from '../src/utils/api.js';
import {responseTable} from '../src/utils/response.js';

async function receive(body, contentType, headers = {}) {
    return readResponse(new Response(body, {headers: {'Content-Type': contentType, ...headers}}), performance.now());
}

test('response MIME controls the default view and preserves the original text', async () => {
    const raw = '{"name":"Ada"}';
    const text = await receive(raw, 'text/plain; charset=UTF-8');
    assert.equal(text.kind, 'text');
    assert.equal(text.text, raw);
    assert.equal(text.hasJson, true);
    const json = await receive(raw, 'Application/Problem+JSON; charset="UTF-8"');
    assert.equal(json.kind, 'json');
    assert.equal(json.mime, 'application/problem+json');
    assert.equal(json.rawText, raw);
    assert.match(json.text, /\n/);
    assert.deepEqual(json.data, {name: 'Ada'});
    const scalar = await receive('null', 'application/json');
    assert.equal(scalar.hasJson, true);
    assert.equal(scalar.data, null);
});

test('invalid JSON and unsupported charset names retain readable response bodies', async () => {
    const malformed = await receive('{broken', 'application/json');
    assert.equal(malformed.kind, 'text');
    assert.equal(malformed.hasJson, false);
    assert.equal(malformed.rawText, '{broken');
    const text = await receive('你好', 'text/plain; charset=unknown');
    assert.equal(text.text, '你好');
    const latin = await receive(new Uint8Array([0x63, 0x61, 0x66, 0xe9]), 'text/plain; charset=iso-8859-1');
    assert.equal(latin.text, 'café');
});

test('image responses include SVG and preserve downloaded bytes without text conversion', async () => {
    for (const type of ['image/png', 'image/jpeg', 'image/svg+xml']) {
        const bytes = new Uint8Array([0, 1, 128, 255]);
        const result = await receive(bytes, type, {'Content-Disposition': "inline; filename*=UTF-8''%E5%9B%BE%E5%83%8F.png"});
        assert.equal(result.kind, 'image');
        assert.equal(result.filename, '图像.png');
        assert.equal(result.downloadable, true);
        assert.equal(result.hasJson, false);
        assert.deepEqual(new Uint8Array(await result.blob.arrayBuffer()), bytes);
    }
});

test('binary previews are bounded while downloads retain the complete file', async () => {
    const bytes = new Uint8Array(256 * 1024).fill(255);
    const result = await receive(bytes, 'application/pdf');
    assert.equal(result.kind, 'bytes');
    assert.equal(result.filename, 'dataway-result.pdf');
    assert.equal(result.blob.size, bytes.length);
    assert.ok(result.text.length < 13000);
    assert.deepEqual(new Uint8Array(await result.blob.arrayBuffer()), bytes);
});

test('JSON records form columns without losing nested values or fields from later rows', async () => {
    const values = [{name: 'Ada', enabled: false, details: {score: 1}}, {name: 'Bob', tags: ['java']}];
    for (const data of [values, {success: true, value: values}, {data: values}, {result: values}]) {
        const table = responseTable(await receive(JSON.stringify(data), 'application/json'));
        assert.deepEqual(table.columns, ['name', 'enabled', 'details', 'tags']);
        assert.deepEqual(table.rows, [['Ada', false, {score: 1}, null], ['Bob', null, null, ['java']]]);
        assert.equal(table.total, 2);
    }
    assert.equal(responseTable(await receive('{"message":"not a table"}', 'application/json')), null);
    assert.equal(responseTable(await receive('plain', 'text/plain')), null);
});

test('arrays and empty JSON results provide table previews with an explicit row limit', async () => {
    const scalar = responseTable(await receive('[1,false,null]', 'application/json'));
    assert.deepEqual(scalar.columns, ['Value']);
    assert.deepEqual(scalar.rows, [[1], [false], [null]]);
    const matrix = responseTable(await receive('[[1,2],[3]]', 'application/json'));
    assert.deepEqual(matrix.columns, ['Column 1', 'Column 2']);
    const empty = responseTable(await receive('[]', 'application/json'));
    assert.equal(empty.total, 0);
    const result = await receive(JSON.stringify(Array.from({length: 250}, (_, id) => ({id}))), 'application/json');
    const table = responseTable(result);
    assert.equal(table.total, 250);
    assert.equal(table.rows.length, 200);
    assert.equal(result.data.length, 250);
});

test('CSV tables handle quoted delimiters, escaped quotes, multiline cells and empty fields', async () => {
    const csv = 'name,note,empty\r\n"Alice, ""A""","line1\r\nline2",\r\nBob,,\r\n';
    for (const mime of ['text/csv', 'application/csv']) {
        const result = await receive(csv, mime + '; charset=UTF-8');
        assert.equal(result.rawText, csv);
        const table = responseTable(result);
        assert.deepEqual(table.columns, ['name', 'note', 'empty']);
        assert.deepEqual(table.rows, [['Alice, "A"', 'line1\r\nline2', ''], ['Bob', '', '']]);
    }
    for (const csv of ['name\n"not closed', 'name\n"closed"oops']) {
        assert.equal(responseTable(await receive(csv, 'text/csv')), null);
    }
});

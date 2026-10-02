/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
import {randomUUID} from 'node:crypto';
import {readFileSync} from 'node:fs';
import {isDeepStrictEqual} from 'node:util';
import {definitions, responses, defaultResponse} from './fixtures.js';

// A fixed PNG rendered by VerifyCodeResultHandler; mock mode never executes Java or DataQL.
const verifyCodeImage = readFileSync(new URL('./verify-code.png', import.meta.url));

export function mockApi() {
    return {
        name: 'dataway-development-mock',
        apply: 'serve',
        configureServer(server) {
            const records = new Map();
            for (const seed of definitions) {
                const draft = definition(seed.id, seed);
                const published = seed.published ? release(draft) : null;
                records.set(seed.id, {draft, version: published ? 2 : 1, published,
                    enabled: seed.enabled !== false, history: published ? [published] : []});
            }
            server.config.logger.info('Dataway Mock: in-memory samples; scripts are not executed. Restart to reset.');
            server.middlewares.use((request, response, next) => {
                if (!/^\/(?:admin\/api|api)(?:\/|\?|$)/.test(request.url)) {
                    next();
                    return;
                }
                handle(request, records).then(result => {
                    respond(request, response, result);
                }).catch(error => {
                    const status = error.status || 500;
                    respond(request, response, {status, body: {success: false, code: status,
                        message: error.message, error: error.message}});
                });
            });
        },
    };
}

function definition(id, input, previous) {
    for (const name of ['select', 'apiPath', 'codeType', 'codeValue']) {
        if (typeof input[name] !== 'string' || !input[name].trim()) {
            fail(400, name + ' is required');
        }
    }
    const select = input.select.toUpperCase();
    const codeType = input.codeType.toUpperCase();
    if (!['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'].includes(select)) {
        fail(400, 'Unsupported HTTP method');
    }
    if (!['DATAQL', 'SQL'].includes(codeType)) {
        fail(400, 'Unsupported script type');
    }
    const path = input.apiPath;
    if (!path.startsWith('/') || path.length > 512 || /[?#%\\\s\x00-\x1f\x7f]/.test(path)
        || path.includes('//') || path.split('/').some(part => part === '.' || part === '..')) {
        fail(400, 'Invalid API path');
    }
    const headerData = input.headerData ?? [];
    if (!Array.isArray(headerData) || headerData.some(row => !row || typeof row.checked !== 'boolean'
        || typeof row.name !== 'string' || typeof row.value !== 'string')) {
        fail(400, 'Each header requires checked, name and value');
    }
    const optionInfo = document(input.optionInfo);
    for (const name of ['wrapAllParameters']) {
        if (Object.hasOwn(optionInfo, name) && typeof optionInfo[name] !== 'boolean') {
            fail(400, name + ' must be a boolean');
        }
    }
    if (Object.hasOwn(optionInfo, 'resultHandler') && !['structure', 'raw', 'csv', 'text', 'verifyCode'].includes(optionInfo.resultHandler)) {
        fail(400, 'Unknown result handler: ' + optionInfo.resultHandler);
    }
    if (optionInfo.wrapAllParameters && !/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(wrapper(optionInfo))) {
        fail(400, 'Invalid parameter wrapper name');
    }
    if ((optionInfo.resultHandler ?? 'structure') === 'structure' && optionInfo.responseFormat != null) {
        object(optionInfo.responseFormat, 'responseFormat');
    }
    const requestBody = object(input.requestBody ?? {}, 'requestBody');
    const sample = {...document(input.sample), requestBody, requestHeader: JSON.stringify(headerData)};
    delete sample.headerData;
    return {id, select, apiPath: path, codeType: codeType === 'SQL' ? 'SQL' : 'DataQL',
        codeValue: input.codeValue, comment: String(input.comment ?? ''), requestBody, headerData,
        optionInfo, sample, schema: Object.hasOwn(input, 'schema') ? document(input.schema) : previous?.schema ?? {}};
}

function fail(status, message) {
    throw Object.assign(new Error(message), {status});
}

function document(value) {
    return object(value == null || value === '' ? {} : value, 'Metadata');
}

function object(value, name) {
    if (typeof value === 'string') {
        try {
            value = JSON.parse(value);
        } catch {
            fail(400, name + ' must be a JSON object');
        }
    }
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
        fail(400, name + ' must be a JSON object');
    }
    return structuredClone(value);
}

function wrapper(options) {
    return String(options.wrapParameterName ?? 'root').trim();
}

function release(draft) {
    return {historyId: randomUUID(), time: new Date().toISOString().slice(0, 19).replace('T', ' '),
        definition: structuredClone(draft)};
}

async function handle(request, records) {
    const url = new URL(request.url, 'http://localhost');
    const body = await readBody(request);
    if (url.pathname === '/admin/api' || url.pathname.startsWith('/admin/api/')) {
        return management(url, request, body, records);
    }
    const path = url.pathname.slice('/api'.length) || '/';
    const record = [...records.values()].find(item => item.enabled && item.published
        && item.published.definition.select === request.method && item.published.definition.apiPath === path);
    if (!record) {
        fail(404, 'Published API not found');
    }
    const entries = [...new Set(url.searchParams.keys())].map(key => {
        const values = url.searchParams.getAll(key);
        return [key, values.length === 1 ? values[0] : values];
    });
    return execute(record.published.definition, {...Object.fromEntries(entries), ...body}, request);
}

async function readBody(request) {
    const chunks = [];
    for await (const chunk of request) {
        chunks.push(chunk);
    }
    if (!chunks.length) {
        return {};
    }
    const type = (request.headers['content-type'] || '').split(';')[0].trim().toLowerCase();
    if (type !== 'application/json') {
        fail(415, 'Expected application/json');
    }
    return object(Buffer.concat(chunks).toString('utf8'), 'JSON body');
}

function management(url, request, body, records) {
    const path = url.pathname.slice('/admin/api'.length);
    const reads = ['/api-list', '/api-info', '/api-detail', '/api-history', '/get-history', '/get-handlers'];
    const writes = ['/save-api', '/perform', '/smoke', '/publish', '/disable', '/delete'];
    if (!reads.includes(path) && !writes.includes(path)) {
        fail(404, 'Not found');
    }
    if (request.method !== (reads.includes(path) ? 'GET' : 'POST')) {
        fail(405, 'Method not allowed');
    }
    for (const key of url.searchParams.keys()) {
        if (url.searchParams.getAll(key).length > 1) {
            fail(400, 'Duplicate query parameter: ' + key);
        }
    }
    const id = url.searchParams.get('id') ?? body.id;
    if (Object.hasOwn(body, 'id') && id !== body.id) {
        fail(400, 'Conflicting API ids');
    }
    if (!['/api-list', '/get-handlers'].includes(path) && (typeof id !== 'string' || !id.trim())) {
        fail(400, 'id is required');
    }
    switch (path) {
        case '/get-handlers': {
            return result(['structure', 'raw', 'csv', 'text', 'verifyCode']);
        }
        case '/api-list': {
            return result([...records.values()].map(record => ({id: record.draft.id, version: record.version,
                checked: false, select: record.draft.select, path: record.draft.apiPath,
                status: status(record), comment: record.draft.comment})));
        }
        case '/api-info':
        case '/api-detail': {
            const record = get(records, id);
            return result(detail(record.draft, record));
        }
        case '/api-history': {
            const record = get(records, id);
            return result(record.history.toReversed().map(item => ({historyId: item.historyId, time: item.time,
                status: record.enabled && item === record.published ? 1 : 3})));
        }
        case '/get-history': {
            const record = get(records, id);
            const item = record.history.find(item => item.historyId === url.searchParams.get('historyId'));
            if (!item) {
                fail(404, 'Release not found');
            }
            return result(detail(item.definition, record));
        }
        case '/save-api': {
            return save(records, id === '-1' ? randomUUID() : id, body);
        }
        case '/perform': {
            const draft = definition(id, body);
            return execute(draft, draft.requestBody, request);
        }
        case '/smoke': {
            const record = get(records, id);
            checkVersion(record, body);
            return execute(record.draft, object(body.requestBody ?? {}, 'requestBody'), request);
        }
        case '/publish': {
            const record = get(records, id);
            checkVersion(record, body);
            record.published = release(record.draft);
            record.history.push(record.published);
            record.enabled = true;
            return result(true, ++record.version);
        }
        case '/disable': {
            const record = get(records, id);
            checkVersion(record, body);
            record.enabled = false;
            return result(true, ++record.version);
        }
        case '/delete': {
            const record = get(records, id);
            checkVersion(record, body);
            records.delete(id);
            return result(true);
        }
    }
}

function result(value, version) {
    return {body: {success: true, code: 200, message: 'OK', result: value,
        ...(version === undefined ? {} : {version})}};
}

function status(record) {
    if (!record.published) {
        return 0;
    }
    if (!record.enabled) {
        return 3;
    }
    return isDeepStrictEqual(record.draft, record.published.definition) ? 1 : 2;
}

function get(records, id) {
    const record = records.get(id);
    if (!record) {
        fail(404, 'API not found');
    }
    return record;
}

function detail(draft, record) {
    const requestBody = JSON.stringify(draft.requestBody);
    return {id: draft.id, version: record.version, select: draft.select, path: draft.apiPath,
        status: status(record), apiComment: draft.comment, codeType: draft.codeType,
        codeInfo: {codeValue: draft.codeValue, requestBody, headerData: draft.headerData},
        requestBody, headerData: draft.headerData, optionData: draft.optionInfo, sample: draft.sample, schema: draft.schema};
}

function save(records, id, body) {
    const previous = records.get(id);
    checkVersion(previous, body);
    const draft = definition(id, body, previous?.draft);
    if (previous && (previous.draft.select !== draft.select || previous.draft.apiPath !== draft.apiPath)) {
        fail(409, 'API route is immutable; create a new API for a different route');
    }
    if ([...records.values()].some(record => record.draft.id !== id
        && record.draft.select === draft.select && record.draft.apiPath === draft.apiPath)) {
        fail(409, 'An API already exists for this method and path');
    }
    const record = {draft, version: body.version + 1, published: previous?.published ?? null,
        enabled: previous?.enabled ?? false, history: previous?.history ?? []};
    records.set(id, record);
    return result(id, record.version);
}

function checkVersion(record, body) {
    if (!Number.isSafeInteger(body.version) || body.version < 0) {
        fail(400, 'A non-negative integer version is required');
    }
    if ((record?.version ?? 0) !== body.version) {
        fail(409, 'API changed; reload and retry');
    }
}

async function execute(definition, values, request) {
    const options = definition.optionInfo;
    const parameters = options.wrapAllParameters ? {[wrapper(options)]: values} : values;
    const responder = responses[definition.select + ' ' + definition.apiPath] ?? defaultResponse;
    const response = await responder({definition: structuredClone(definition), parameters, request});
    const handler = options.resultHandler ?? 'structure';
    if (!['structure', 'raw', 'text', 'csv', 'verifyCode'].includes(handler)) {
        fail(400, 'Unknown result handler: ' + handler);
    }
    if ((response.status ?? 200) < 400 && handler === 'verifyCode') {
        return {...response, body: verifyCodeImage, headers: {...response.headers, 'Content-Type': 'image/png', 'Cache-Control': 'no-store'}};
    }
    if ((response.status ?? 200) < 400 && ['text', 'csv'].includes(handler)) {
        if (handler === 'text') {
            if (Buffer.isBuffer(response.body)) {
                fail(400, 'Use Raw Value to return binary content');
            }
            const text = textValue(response.body);
            return {...response, body: Buffer.from(text), headers: {...response.headers, 'Content-Type': 'text/plain; charset=UTF-8'}};
        }
        if (!Array.isArray(response.body) || response.body.some(row => !row || typeof row !== 'object' || Array.isArray(row))) {
            fail(400, 'CSV result must be a list of objects');
        }
        const columns = [...new Set(response.body.flatMap(row => Object.keys(row)))];
        const cell = value => {
            if (value != null && typeof value === 'object') {
                fail(400, 'CSV cells must be scalar values');
            }
            const text = String(value ?? '');
            return /[",\r\n]/.test(text) ? '"' + text.replaceAll('"', '""') + '"' : text;
        };
        const rows = columns.length ? [columns, ...response.body.map(row => columns.map(name => row[name]))] : [];
        const csv = rows.map(row => row.map(cell).join(',') + '\r\n').join('');
        return {...response, body: Buffer.from(csv), headers: {...response.headers,
            'Content-Type': 'text/csv; charset=UTF-8', 'Content-Disposition': 'attachment; filename=results.csv'}};
    }
    if (handler === 'raw' || Buffer.isBuffer(response.body) || response.status >= 400) {
        return response;
    }
    const fields = {'@resultStatus': true, '@resultMessage': 'OK', '@resultCode': 0,
        '@codeLocation': null, '@timeLifeCycle': 1, '@timeExecution': 1, '@resultData': response.body};
    const format = options.responseFormat == null ? {
        success: '@resultStatus', message: '@resultMessage', code: '@resultCode', location: '@codeLocation',
        lifeCycleTime: '@timeLifeCycle', executionTime: '@timeExecution', value: '@resultData',
    } : object(options.responseFormat, 'responseFormat');
    return {...response, body: Object.fromEntries(Object.entries(format).map(([key, value]) =>
        [key, typeof value === 'string' && Object.hasOwn(fields, value) ? fields[value] : value]))};
}

function textValue(value) {
    if (Array.isArray(value)) {
        return '[' + value.map(textValue).join(', ') + ']';
    }
    if (value !== null && typeof value === 'object') {
        return '{' + Object.entries(value).map(([key, item]) => key + '=' + textValue(item)).join(', ') + '}';
    }
    return String(value);
}

function respond(request, response, result) {
    const binary = Buffer.isBuffer(result.body);
    response.writeHead(result.status ?? 200, {
        'Content-Type': binary ? 'application/octet-stream' : 'application/json; charset=utf-8',
        'Cache-Control': 'no-store', 'X-Dataway-Mock': 'true', ...result.headers,
    });
    response.end(request.method === 'HEAD' ? undefined : binary ? result.body : JSON.stringify(result.body));
}

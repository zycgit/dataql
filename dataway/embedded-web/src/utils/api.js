/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
export function baseAddress(value, page, name) {
    if (typeof value !== 'string' || !value.trim()) {
        throw new Error('DatawayUI requires ' + name + '.');
    }
    const url = new URL(value, page);
    if (url.origin !== new URL(page).origin || url.username || url.password || url.search || url.hash || !url.pathname.endsWith('/')) {
        throw new Error(name + ' must be a same-origin address ending in /, without credentials, query or fragment.');
    }
    return url;
}

export function requestHeaders(rows = []) {
    const headers = new Headers();
    for (const row of rows) {
        if (row.checked && row.name.trim()) {
            headers.append(row.name.trim(), row.value);
        }
    }
    return headers;
}

export function parameters(text, method = 'POST') {
    const value = JSON.parse(text);
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
        throw new Error('Parameters must be a JSON object.');
    }
    if (['GET', 'HEAD'].includes(method)) {
        for (const item of Object.values(value)) {
            if (item !== null && typeof item === 'object') {
                throw new Error(method + ' parameters cannot contain objects or arrays.');
            }
        }
    }
    return value;
}

export async function readResponse(response, started) {
    const blob = await response.blob();
    const contentType = response.headers.get('content-type') || 'application/octet-stream';
    const mime = contentType.split(';')[0].trim().toLowerCase();
    const charset = /charset\s*=\s*["']?([^;"'\s]+)/i.exec(contentType)?.[1] || 'utf-8';
    const isJson = mime === 'application/json' || mime.endsWith('+json');
    const isImage = mime.startsWith('image/');
    const isText = !isImage && (isJson || mime.startsWith('text/') || mime === 'application/csv'
        || /(?:xml|javascript|x-www-form-urlencoded)$/.test(mime));
    let data = null;
    let hasJson = false;
    let rawText = '';
    let text = '';
    let kind = isImage ? 'image' : isText ? 'text' : 'bytes';
    if (isText) {
        let decoder;
        try {
            decoder = new TextDecoder(charset);
        } catch {
            decoder = new TextDecoder('utf-8');
        }
        rawText = decoder.decode(await blob.arrayBuffer());
        text = rawText;
        try {
            data = JSON.parse(rawText);
            hasJson = true;
        } catch {
            // Keep malformed JSON and ordinary text visible in the Text view.
        }
        if (isJson && hasJson) {
            kind = 'json';
            text = JSON.stringify(data, null, 2);
        }
    } else if (!isImage) {
        // Bound the binary summary; downloads always use the complete original blob.
        const bytes = new Uint8Array(await blob.slice(0, 4096).arrayBuffer());
        for (let offset = 0; offset < bytes.length; offset += 16) {
            text += Array.from(bytes.subarray(offset, offset + 16), byte => byte.toString(16).padStart(2, '0').toUpperCase()).join(' ') + '\n';
        }
        text = text.trim();
    }
    const disposition = response.headers.get('content-disposition') || '';
    const extension = isJson ? 'json' : ['text/csv', 'application/csv'].includes(mime) ? 'csv' : isText ? 'txt'
        : isImage ? mime.split('/')[1].split('+')[0].replace('jpeg', 'jpg') : mime === 'application/pdf' ? 'pdf' : 'bin';
    let filename = /filename="?([^";]+)"?/i.exec(disposition)?.[1] || 'dataway-result.' + extension;
    const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1];
    if (encoded) {
        try {
            filename = decodeURIComponent(encoded);
        } catch {
            // Keep the plain filename when the encoded form is malformed.
        }
    }
    return {
        status: response.status, ok: response.ok, kind, contentType, mime, hasJson,
        data, text, rawText, blob, filename, downloadable: !isText || /attachment/i.test(disposition), elapsed: Math.round(performance.now() - started)
    };
}

export class DatawayClient {
    constructor(config, page = document.baseURI) {
        this.admin = baseAddress(config.adminApi, page, 'adminApi');
        this.api = config.api ? baseAddress(config.api, page, 'api') : null;
    }

    async management(path, {method = 'GET', id, query = {}, body, headers} = {}) {
        const url = new URL(path, this.admin);
        for (const [key, value] of Object.entries({...query, ...(id === undefined ? {} : {id})})) {
            url.searchParams.set(key, value);
        }
        const result = await this.send(url, method, body, headers);
        if (!result.ok || result.data?.success !== true) {
            const fallback = result.kind === 'text' && result.text.includes('<html')
                ? 'The host returned a login page. Sign in to the host application and retry.'
                : 'Request failed (HTTP ' + result.status + ').';
            const error = new Error(result.data?.message || result.data?.error || fallback);
            error.status = result.status;
            throw error;
        }
        return result.data;
    }

    async execute(path, id, body, headers) {
        const url = new URL(path, this.admin);
        url.searchParams.set('id', id);
        return this.send(url, 'POST', body, headers);
    }

    async invoke(api, text, rows) {
        if (!this.api) {
            throw new Error('Set the DatawayUI api option to the public business API address.');
        }
        const values = parameters(text, api.select);
        const url = new URL(api.path.replace(/^\/+/, ''), this.api);
        if (url.origin !== this.api.origin || !url.pathname.startsWith(this.api.pathname)) {
            throw new Error('Invalid business API path.');
        }
        const queryOnly = ['GET', 'HEAD'].includes(api.select);
        if (queryOnly) {
            for (const [key, value] of Object.entries(values)) {
                if (value !== null) {
                    url.searchParams.append(key, String(value));
                }
            }
        }
        return this.send(url, api.select, queryOnly ? undefined : values, requestHeaders(rows));
    }

    async send(url, method, body, suppliedHeaders) {
        const headers = new Headers(suppliedHeaders);
        if (body !== undefined && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }
        const started = performance.now();
        const response = await fetch(url, {
            method, headers, credentials: 'same-origin',
            body: body === undefined ? undefined : JSON.stringify(body)
        });
        return readResponse(response, started);
    }
}

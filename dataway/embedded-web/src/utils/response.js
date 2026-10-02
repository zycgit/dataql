/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */

// Limit rendered rows while retaining the complete response for JSON, text and downloads.
const previewLimit = 200;

export function responseTable(response) {
    if (!response) {
        return null;
    }
    if (response.mime === 'text/csv' || response.mime === 'application/csv') {
        const rows = csvRows(response.rawText);
        if (!rows?.length) {
            return null;
        }
        const headers = rows.shift();
        const width = rows.reduce((count, row) => Math.max(count, row.length), headers.length);
        const columns = Array.from({length: width}, (_, index) => headers[index] || 'Column ' + (index + 1));
        return {source: 'CSV', columns, rows: rows.slice(0, previewLimit), total: rows.length};
    }
    if (!response.hasJson) {
        return null;
    }
    let rows = response.data;
    let source = '$';
    if (!Array.isArray(rows) && rows && typeof rows === 'object') {
        const field = ['value', 'data', 'result'].find(name => Array.isArray(rows[name]));
        if (field) {
            rows = rows[field];
            source += '.' + field;
        }
    }
    if (!Array.isArray(rows)) {
        return null;
    }
    if (rows.every(row => row && typeof row === 'object' && !Array.isArray(row))) {
        const columns = [...new Set(rows.flatMap(row => Object.keys(row)))];
        return {source, columns, total: rows.length,
            rows: rows.slice(0, previewLimit).map(row => columns.map(name => Object.hasOwn(row, name) ? row[name] : null))};
    }
    if (rows.every(Array.isArray)) {
        const width = rows.reduce((count, row) => Math.max(count, row.length), 0);
        return {source, columns: Array.from({length: width}, (_, index) => 'Column ' + (index + 1)),
            rows: rows.slice(0, previewLimit), total: rows.length};
    }
    return {source, columns: ['Value'], rows: rows.slice(0, previewLimit).map(value => [value]), total: rows.length};
}

function csvRows(text) {
    const rows = [];
    let row = [];
    let cell = '';
    let quoted = false;
    let closed = false;
    for (let index = 0; index < text.length; index++) {
        const char = text[index];
        if (quoted) {
            if (char === '"' && text[index + 1] === '"') {
                cell += '"';
                index++;
            } else if (char === '"') {
                quoted = false;
                closed = true;
            } else {
                cell += char;
            }
        } else if (char === ',' || char === '\r' || char === '\n') {
            row.push(cell);
            cell = '';
            closed = false;
            if (char !== ',') {
                rows.push(row);
                row = [];
                if (char === '\r' && text[index + 1] === '\n') {
                    index++;
                }
            }
        } else if (char === '"' && cell === '' && !closed) {
            quoted = true;
        } else if (closed || char === '"') {
            return null;
        } else {
            cell += char;
        }
    }
    if (quoted) {
        return null;
    }
    if (cell !== '' || row.length || closed) {
        row.push(cell);
        rows.push(row);
    }
    return rows;
}

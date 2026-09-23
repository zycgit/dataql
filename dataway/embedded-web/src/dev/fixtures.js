/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */

// Each seed uses the save-api fields, plus a local id and publication flags.
export const definitions = [
    {
        id: 'mock-hello', select: 'POST', apiPath: '/mock/hello', published: true,
        comment: 'Mock greeting and request headers', codeType: 'DataQL',
        codeValue: 'return ${message};', requestBody: {message: 'Hello Dataway Mock.'},
        headerData: [{checked: true, name: 'X-Demo', value: 'console'}],
        optionInfo: {resultStructure: true},
    },
    {
        id: 'mock-users', select: 'GET', apiPath: '/mock/users', published: true,
        comment: 'Mock SQL rows', codeType: 'SQL',
        codeValue: 'select id, name from users', requestBody: {},
        optionInfo: {resultStructure: true},
    },
    {
        id: 'mock-download', select: 'GET', apiPath: '/mock/download', published: true,
        comment: 'Mock binary download', codeType: 'DataQL',
        codeValue: '// The mock responder supplies the download bytes.\nreturn null;', requestBody: {},
        optionInfo: {resultStructure: false},
    },
    {
        id: 'mock-draft', select: 'POST', apiPath: '/mock/draft', published: false,
        comment: 'Draft ready for editing', codeType: 'DataQL',
        codeValue: 'return ${message};', requestBody: {message: 'Draft example'},
        optionInfo: {resultStructure: true},
    },
    {
        id: 'mock-disabled', select: 'POST', apiPath: '/mock/disabled', published: true, enabled: false,
        comment: 'Disabled API example', codeType: 'DataQL',
        codeValue: 'return ${message};', requestBody: {message: 'Disabled example'},
        optionInfo: {resultStructure: true},
    },
];

// Responses are selected by method and path. Editor scripts are never evaluated.
// A responder can return {body, status, headers}; Buffer bodies bypass JSON formatting.
export const responses = {
    'POST /mock/hello': ({parameters, request}) => ({
        body: {mock: true, message: parameters.message ?? 'Hello Dataway Mock.',
            parameters, header: request.headers['x-demo'] ?? null},
    }),
    'GET /mock/users': () => ({body: [{id: 1, name: 'Alice'}, {id: 2, name: 'Bob'}]}),
    'GET /mock/download': () => ({
        body: Buffer.from([0, 1, 255, 10]),
        headers: {'Content-Type': 'application/octet-stream', 'Content-Disposition': 'attachment; filename="mock-result.bin"'},
    }),
};

export function defaultResponse({definition, parameters}) {
    return {body: {mock: true, message: 'Sample response; the script was not executed.',
        method: definition.select, path: definition.apiPath, codeType: definition.codeType, parameters}};
}
